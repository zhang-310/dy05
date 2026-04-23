package cn.gaifan.douyinOperations.module.system.service.impl;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.storage.service.BosStorageService;
import cn.gaifan.douyinOperations.module.system.entity.SysApiCallLog;
import cn.gaifan.douyinOperations.module.system.entity.SysSyncLog;
import cn.gaifan.douyinOperations.module.system.repository.SysApiCallLogRepository;
import cn.gaifan.douyinOperations.module.system.repository.SysSyncLogRepository;
import cn.gaifan.douyinOperations.module.system.service.SystemService;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cluster.HealthRequest;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.collection.HasCollectionParam;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class SystemServiceImpl implements SystemService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private SysApiCallLogRepository apiCallLogRepository;

    @Resource
    private SysSyncLogRepository syncLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;
    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;
    @Autowired(required = false)
    private ElasticsearchClient elasticsearchClient;
    @Autowired(required = false)
    private MilvusServiceClient milvusClient;
    @Autowired(required = false)
    private BosStorageService bosStorageService;

    @Value("${app.ai.ollama-url:http://localhost:11434}")
    private String ollamaUrl;

    // ─── API 调用日志 ────────────────────────────────────────────

    private static final Set<String> API_LOG_SORTABLE = Set.of("id", "module", "apiName", "createTime", "durationMs", "status");

    @Override
    public PageResultVO<Map<String, Object>> searchApiLogs(String module, String apiName,
                                                            Integer status, String startTime,
                                                            String endTime, String sortName, String sortOrder,
                                                            int page, int rows) {
        Specification<SysApiCallLog> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (module != null && !module.isBlank())
                predicates.add(cb.equal(root.get("module"), module));
            if (apiName != null && !apiName.isBlank())
                predicates.add(cb.like(root.get("apiName"), "%" + apiName + "%"));
            if (status != null)
                predicates.add(cb.equal(root.get("status"), status));
            if (startTime != null && !startTime.isBlank())
                predicates.add(cb.greaterThanOrEqualTo(root.get("createTime"),
                        Timestamp.valueOf(LocalDateTime.parse(startTime, FMT))));
            if (endTime != null && !endTime.isBlank())
                predicates.add(cb.lessThanOrEqualTo(root.get("createTime"),
                        Timestamp.valueOf(LocalDateTime.parse(endTime, FMT))));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        String sort = (sortName != null && API_LOG_SORTABLE.contains(sortName)) ? sortName : "id";
        Sort.Direction dir = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Page<SysApiCallLog> p = apiCallLogRepository.findAll(spec,
                PageRequest.of(page, rows, Sort.by(dir, sort)));
        List<Map<String, Object>> list = p.getContent().stream().map(this::apiLogToMap).toList();
        return PageResultVO.of(p.getTotalElements(), list, page, rows);
    }

    @Override
    public Map<String, Object> getApiLogStats(String module, String startTime, String endTime) {
        StringBuilder jpql = new StringBuilder(
                "SELECT COUNT(l), SUM(CASE WHEN l.status=1 THEN 1 ELSE 0 END), AVG(l.durationMs) FROM SysApiCallLog l WHERE 1=1");
        Map<String, Object> params = new LinkedHashMap<>();
        if (module != null && !module.isBlank()) { jpql.append(" AND l.module = :module"); params.put("module", module); }
        if (startTime != null && !startTime.isBlank()) { jpql.append(" AND l.createTime >= :st"); params.put("st", Timestamp.valueOf(LocalDateTime.parse(startTime, FMT))); }
        if (endTime != null && !endTime.isBlank()) { jpql.append(" AND l.createTime <= :et"); params.put("et", Timestamp.valueOf(LocalDateTime.parse(endTime, FMT))); }

        var q = entityManager.createQuery(jpql.toString());
        params.forEach(q::setParameter);
        Object[] row = (Object[]) q.getSingleResult();

        long total = row[0] == null ? 0L : ((Number) row[0]).longValue();
        long success = row[1] == null ? 0L : ((Number) row[1]).longValue();
        double avgMs = row[2] == null ? 0.0 : ((Number) row[2]).doubleValue();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCalls", total);
        result.put("successCount", success);
        result.put("failCount", total - success);
        result.put("successRate", total == 0 ? 0.0 : Math.round(success * 10000.0 / total) / 100.0);
        result.put("avgDurationMs", Math.round(avgMs));

        List<Map<String, Object>> byModule = queryStatsByModule(module, startTime, endTime);
        List<Map<String, Object>> byApiName = queryStatsByApiName(module, startTime, endTime);
        result.put("byModule", byModule);
        result.put("byApiName", byApiName);
        return result;
    }

    @Override
    public Map<String, Object> getApiLogById(Long id) {
        return apiCallLogRepository.findById(id)
                .map(this::apiLogToMapFull)
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> queryStatsByModule(String module, String startTime, String endTime) {
        String jpql = "SELECT l.module, COUNT(l), SUM(CASE WHEN l.status=1 THEN 1 ELSE 0 END), AVG(l.durationMs) " +
                "FROM SysApiCallLog l WHERE 1=1";
        if (module != null && !module.isBlank()) jpql += " AND l.module = :module";
        if (startTime != null && !startTime.isBlank()) jpql += " AND l.createTime >= :st";
        if (endTime != null && !endTime.isBlank()) jpql += " AND l.createTime <= :et";
        jpql += " GROUP BY l.module ORDER BY COUNT(l) DESC";

        var q = entityManager.createQuery(jpql);
        if (module != null && !module.isBlank()) q.setParameter("module", module);
        if (startTime != null && !startTime.isBlank()) q.setParameter("st", Timestamp.valueOf(LocalDateTime.parse(startTime, FMT)));
        if (endTime != null && !endTime.isBlank()) q.setParameter("et", Timestamp.valueOf(LocalDateTime.parse(endTime, FMT)));

        List<Object[]> rows = q.getResultList();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] r : rows) {
            long cnt = ((Number) r[1]).longValue();
            long succ = ((Number) r[2]).longValue();
            double avg = r[3] != null ? ((Number) r[3]).doubleValue() : 0;
            list.add(Map.of(
                    "module", r[0] != null ? r[0] : "unknown",
                    "totalCalls", cnt,
                    "successRate", cnt == 0 ? 0.0 : Math.round(succ * 10000.0 / cnt) / 100.0,
                    "avgDurationMs", Math.round(avg)
            ));
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> queryStatsByApiName(String module, String startTime, String endTime) {
        String jpql = "SELECT l.apiName, COUNT(l), SUM(CASE WHEN l.status=1 THEN 1 ELSE 0 END), AVG(l.durationMs) " +
                "FROM SysApiCallLog l WHERE 1=1";
        if (module != null && !module.isBlank()) jpql += " AND l.module = :module";
        if (startTime != null && !startTime.isBlank()) jpql += " AND l.createTime >= :st";
        if (endTime != null && !endTime.isBlank()) jpql += " AND l.createTime <= :et";
        jpql += " GROUP BY l.apiName ORDER BY COUNT(l) DESC";

        var q = entityManager.createQuery(jpql).setMaxResults(20);
        if (module != null && !module.isBlank()) q.setParameter("module", module);
        if (startTime != null && !startTime.isBlank()) q.setParameter("st", Timestamp.valueOf(LocalDateTime.parse(startTime, FMT)));
        if (endTime != null && !endTime.isBlank()) q.setParameter("et", Timestamp.valueOf(LocalDateTime.parse(endTime, FMT)));

        List<Object[]> rows = q.getResultList();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] r : rows) {
            long cnt = ((Number) r[1]).longValue();
            long succ = ((Number) r[2]).longValue();
            double avg = r[3] != null ? ((Number) r[3]).doubleValue() : 0;
            list.add(Map.of(
                    "apiName", r[0] != null ? r[0] : "unknown",
                    "totalCalls", cnt,
                    "successRate", cnt == 0 ? 0.0 : Math.round(succ * 10000.0 / cnt) / 100.0,
                    "avgDurationMs", Math.round(avg)
            ));
        }
        return list;
    }

    @Override
    @Async
    public void saveApiLog(String module, String apiName, String requestUrl, String requestMethod,
                           String requestParams, Integer responseStatus, String responseBody,
                           Integer status, String errorMessage, Long durationMs, Long userId) {
        SysApiCallLog log = new SysApiCallLog();
        log.setModule(module);
        log.setApiName(apiName);
        log.setRequestUrl(requestUrl);
        log.setRequestMethod(requestMethod);
        log.setRequestParams(requestParams);
        log.setResponseStatus(responseStatus);
        log.setResponseBody(responseBody != null && responseBody.length() > 2000
                ? responseBody.substring(0, 2000) : responseBody);
        log.setStatus(status);
        log.setErrorMessage(errorMessage);
        log.setDurationMs(durationMs);
        log.setUserId(userId);
        apiCallLogRepository.save(log);
    }

    // ─── 同步日志 ────────────────────────────────────────────────

    @Override
    public PageResultVO<Map<String, Object>> searchSyncLogs(String syncType, String status,
                                                             Long userId, String startTime,
                                                             String endTime, int page, int rows) {
        Specification<SysSyncLog> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (syncType != null && !syncType.isBlank())
                predicates.add(cb.equal(root.get("syncType"), syncType));
            if (status != null && !status.isBlank())
                predicates.add(cb.equal(root.get("status"), status));
            if (userId != null)
                predicates.add(cb.equal(root.get("userId"), userId));
            if (startTime != null && !startTime.isBlank())
                predicates.add(cb.greaterThanOrEqualTo(root.get("createTime"),
                        Timestamp.valueOf(LocalDateTime.parse(startTime, FMT))));
            if (endTime != null && !endTime.isBlank())
                predicates.add(cb.lessThanOrEqualTo(root.get("createTime"),
                        Timestamp.valueOf(LocalDateTime.parse(endTime, FMT))));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<SysSyncLog> p = syncLogRepository.findAll(spec,
                PageRequest.of(page, rows, Sort.by(Sort.Direction.DESC, "id")));
        List<Map<String, Object>> list = p.getContent().stream().map(this::syncLogToMap).toList();
        return PageResultVO.of(p.getTotalElements(), list, page, rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long startSync(String syncType, Long userId, Long accountId) {
        SysSyncLog log = new SysSyncLog();
        log.setSyncType(syncType);
        log.setUserId(userId);
        log.setAccountId(accountId);
        log.setStatus("running");
        return syncLogRepository.save(log).getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSyncProgress(Long syncLogId, int total, int success, int fail) {
        syncLogRepository.updateProgress(syncLogId, total, success, fail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeSync(Long syncLogId, int total, int success, int fail) {
        syncLogRepository.completeSync(syncLogId, "success", total, success, fail,
                new Timestamp(System.currentTimeMillis()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void failSync(Long syncLogId, String errorMessage) {
        syncLogRepository.failSync(syncLogId, "failed", errorMessage,
                new Timestamp(System.currentTimeMillis()));
    }

    // ─── 系统监控 ────────────────────────────────────────────────

    @Override
    public Map<String, Object> checkHealth() {
        Map<String, CompletableFuture<Map<String, Object>>> futures = new LinkedHashMap<>();
        futures.put("database", CompletableFuture.supplyAsync(this::checkDatabase));
        futures.put("ollama", CompletableFuture.supplyAsync(this::checkOllama));
        futures.put("milvus", CompletableFuture.supplyAsync(this::checkMilvus));
        futures.put("elasticsearch", CompletableFuture.supplyAsync(this::checkElasticsearch));
        futures.put("redis", CompletableFuture.supplyAsync(this::checkRedis));
        futures.put("rabbitmq", CompletableFuture.supplyAsync(this::checkRabbitmq));
        futures.put("storage", CompletableFuture.supplyAsync(this::checkStorage));

        Map<String, Object> result = new LinkedHashMap<>();
        int downCount = 0;
        for (Map.Entry<String, CompletableFuture<Map<String, Object>>> e : futures.entrySet()) {
            try {
                Map<String, Object> v = e.getValue().get(5, TimeUnit.SECONDS);
                result.put(e.getKey(), v);
                if (!"UP".equals(v.get("status"))) downCount++;
            } catch (Exception ex) {
                result.put(e.getKey(), Map.of("status", "DOWN"));
                downCount++;
            }
        }
        result.put("_overall", downCount == 0 ? "UP" : (downCount == futures.size() ? "DOWN" : "DEGRADED"));
        return result;
    }

    @Override
    public Map<String, Object> getSystemInfo() {
        Runtime runtime = Runtime.getRuntime();
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();

        long maxMem = runtime.maxMemory();
        long totalMem = runtime.totalMemory();
        long freeMem = runtime.freeMemory();
        long usedMem = totalMem - freeMem;

        Map<String, Object> jvm = new LinkedHashMap<>();
        jvm.put("maxMemory", formatBytes(maxMem));
        jvm.put("totalMemory", formatBytes(totalMem));
        jvm.put("freeMemory", formatBytes(freeMem));
        jvm.put("usedMemory", formatBytes(usedMem));
        jvm.put("usagePercent", maxMem == 0 ? 0.0 : Math.round(usedMem * 1000.0 / maxMem) / 10.0);

        Map<String, Object> threads = new LinkedHashMap<>();
        threads.put("activeCount", threadBean.getThreadCount());
        threads.put("peakCount", threadBean.getPeakThreadCount());
        threads.put("daemonCount", threadBean.getDaemonThreadCount());

        long uptimeMs = runtimeBean.getUptime();
        long hours = uptimeMs / 3600000;
        long minutes = (uptimeMs % 3600000) / 60000;
        long seconds = (uptimeMs % 60000) / 1000;

        Map<String, Object> runtimeInfo = new LinkedHashMap<>();
        runtimeInfo.put("startTime", java.time.Instant.ofEpochMilli(runtimeBean.getStartTime())
                .atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        runtimeInfo.put("uptime", hours + "h " + minutes + "m " + seconds + "s");
        runtimeInfo.put("javaVersion", System.getProperty("java.version"));
        runtimeInfo.put("osName", System.getProperty("os.name"));
        runtimeInfo.put("osArch", System.getProperty("os.arch"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("jvm", jvm);
        result.put("threads", threads);
        result.put("runtime", runtimeInfo);
        return result;
    }

    // ─── 私有方法 ────────────────────────────────────────────────

    private Map<String, Object> checkDatabase() {
        long start = System.currentTimeMillis();
        try {
            entityManager.createNativeQuery("SELECT 1").getSingleResult();
            return Map.of("status", "UP", "latency", (System.currentTimeMillis() - start) + "ms");
        } catch (Exception e) {
            return Map.of("status", "DOWN");
        }
    }

    private Map<String, Object> checkOllama() {
        long start = System.currentTimeMillis();
        try {
            String url = (ollamaUrl != null ? ollamaUrl : "http://localhost:11434").replaceAll("/$", "") + "/api/tags";
            new RestTemplate().getForEntity(url, String.class);
            return Map.of("status", "UP", "latency", (System.currentTimeMillis() - start) + "ms");
        } catch (Exception e) {
            return Map.of("status", "DOWN");
        }
    }

    private Map<String, Object> checkMilvus() {
        long start = System.currentTimeMillis();
        if (milvusClient == null) {
            return Map.of("status", "DOWN", "provider", "未配置");
        }
        try {
            milvusClient.hasCollection(HasCollectionParam.newBuilder().withCollectionName("_health_check").build());
            return Map.of("status", "UP", "latency", (System.currentTimeMillis() - start) + "ms");
        } catch (Exception e) {
            return Map.of("status", "DOWN");
        }
    }

    private Map<String, Object> checkElasticsearch() {
        long start = System.currentTimeMillis();
        if (elasticsearchClient == null) {
            return Map.of("status", "DOWN", "provider", "未配置");
        }
        try {
            elasticsearchClient.cluster().health(HealthRequest.of(h -> h));
            return Map.of("status", "UP", "latency", (System.currentTimeMillis() - start) + "ms");
        } catch (Exception e) {
            return Map.of("status", "DOWN");
        }
    }

    private Map<String, Object> checkRedis() {
        long start = System.currentTimeMillis();
        if (redisConnectionFactory == null) {
            return Map.of("status", "DOWN", "provider", "未配置");
        }
        try {
            redisConnectionFactory.getConnection().ping();
            return Map.of("status", "UP", "latency", (System.currentTimeMillis() - start) + "ms");
        } catch (Exception e) {
            return Map.of("status", "DOWN");
        }
    }

    private Map<String, Object> checkRabbitmq() {
        long start = System.currentTimeMillis();
        if (rabbitTemplate == null) {
            return Map.of("status", "DOWN", "provider", "未配置");
        }
        try {
            rabbitTemplate.execute(channel -> Boolean.TRUE);
            return Map.of("status", "UP", "latency", (System.currentTimeMillis() - start) + "ms");
        } catch (Exception e) {
            return Map.of("status", "DOWN");
        }
    }

    private Map<String, Object> checkStorage() {
        long start = System.currentTimeMillis();
        if (bosStorageService != null && bosStorageService.isConfigured()) {
            try {
                bosStorageService.listObjects("");
                return Map.of("status", "UP", "latency", (System.currentTimeMillis() - start) + "ms", "provider", "bos");
            } catch (Exception e) {
                return Map.of("status", "DOWN");
            }
        }
        try {
            java.io.File tmpDir = new java.io.File(System.getProperty("java.io.tmpdir"));
            return Map.of("status", tmpDir.canWrite() ? "UP" : "DOWN", "provider", "local");
        } catch (Exception e) {
            return Map.of("status", "DOWN");
        }
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1024 * 1024 * 1024) return (bytes / (1024 * 1024 * 1024)) + "GB";
        if (bytes >= 1024 * 1024) return (bytes / (1024 * 1024)) + "MB";
        if (bytes >= 1024) return (bytes / 1024) + "KB";
        return bytes + "B";
    }

    private Map<String, Object> apiLogToMap(SysApiCallLog e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("module", e.getModule());
        m.put("apiName", e.getApiName());
        m.put("requestUrl", e.getRequestUrl());
        m.put("requestMethod", e.getRequestMethod());
        m.put("responseStatus", e.getResponseStatus());
        m.put("status", e.getStatus());
        m.put("errorMessage", e.getErrorMessage());
        m.put("durationMs", e.getDurationMs());
        m.put("userId", e.getUserId());
        m.put("createTime", e.getCreateTime());
        return m;
    }

    private Map<String, Object> apiLogToMapFull(SysApiCallLog e) {
        Map<String, Object> m = apiLogToMap(e);
        m.put("requestParams", e.getRequestParams());
        m.put("responseBody", e.getResponseBody());
        return m;
    }

    private Map<String, Object> syncLogToMap(SysSyncLog e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("syncType", e.getSyncType());
        m.put("userId", e.getUserId());
        m.put("accountId", e.getAccountId());
        m.put("status", e.getStatus());
        m.put("totalCount", e.getTotalCount());
        m.put("successCount", e.getSuccessCount());
        m.put("failCount", e.getFailCount());
        m.put("errorMessage", e.getErrorMessage());
        m.put("startTime", e.getStartTime());
        m.put("endTime", e.getEndTime());
        m.put("createTime", e.getCreateTime());
        return m;
    }
}
