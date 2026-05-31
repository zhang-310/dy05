package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiCallLog;
import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTask;
import cn.gaifan.douyinOperations.module.ai.entity.AiKbDocument;
import cn.gaifan.douyinOperations.module.ai.repository.AiCallLogRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveReportRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveTaskRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import cn.gaifan.douyinOperations.module.ai.service.EvolveRoiService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class EvolveRoiServiceImpl implements EvolveRoiService {

    private static final Logger log = LoggerFactory.getLogger(EvolveRoiServiceImpl.class);

    private static final String REDIS_THROTTLE = "ai:evolve:throttle";
    private static final String REDIS_LAST_RUN = "ai:evolve:last_run";
    private static final String REDIS_LOW_ROUNDS = "ai:evolve:low_rounds";
    private static final String REDIS_RECOVERY_ROUNDS = "ai:evolve:recovery_rounds";
    private static final long THROTTLE_HOURS = 48;

    @Resource
    private AiEvolveTaskRepository taskRepository;

    @Resource
    private AiEvolveReportRepository reportRepository;

    @Resource
    private AiKbDocumentRepository documentRepository;

    @Resource
    private AiCallLogRepository callLogRepository;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 为 false 时不再因 Redis 降频键跳过调度（本地开发/排障用）；生产建议保持 true。
     */
    @Value("${app.ai.evolve.roi-throttle-enabled:true}")
    private boolean roiThrottleEnabled;

    private final AtomicBoolean throttleSkipHintLogged = new AtomicBoolean();

    @PostConstruct
    void logEvolveRoiThrottleBinding() {
        log.info("进化调度 ROI 降频 app.ai.evolve.roi-throttle-enabled={}；为 true 且 Redis {}=1 时会跳过调度（另有 48h 窗口逻辑）。Docker 默认 profile 常为 prod，不会读取 application-dev 中的 false，需环境变量 EVOLVE_ROI_THROTTLE_ENABLED=false 或删 Redis 键。",
                roiThrottleEnabled, REDIS_THROTTLE);
    }

    @Override
    public Map<String, Object> getRoiMetrics(int lastRounds) {
        Map<String, Object> result = new LinkedHashMap<>();
        Timestamp since = Timestamp.from(LocalDateTime.now().minusDays(30).atZone(ZoneId.systemDefault()).toInstant());
        List<AiEvolveTask> tasks = taskRepository.findScoredTasksSince(since);
        int total = Math.min(tasks.size(), lastRounds);
        if (total == 0) {
            result.put("storageRate", 0.0);
            result.put("generatedCount", 0);
            result.put("indexedCount", 0);
            result.put("throttled", isThrottled());
            return result;
        }
        List<AiEvolveTask> recent = tasks.subList(Math.max(0, tasks.size() - lastRounds), tasks.size());
        int indexed = (int) recent.stream().filter(t -> t.getScoreTotal() != null && t.getScoreTotal() >= 50).count();
        double rate = recent.size() > 0 ? (double) indexed / recent.size() : 0;
        result.put("storageRate", Math.round(rate * 100) / 100.0);
        result.put("generatedCount", recent.size());
        result.put("indexedCount", indexed);
        result.put("throttled", isThrottled());
        result.put("knowledgeRefRate", getKnowledgeRefRate());
        return result;
    }

    @Override
    public double getKnowledgeRefRate() {
        List<AiKbDocument> evolvedDocs = documentRepository.findBySourceTypeAndDeleted("evolved", 0);
        if (evolvedDocs.isEmpty()) return -1;

        Set<Long> evolvedChunkIds = new HashSet<>();
        for (AiKbDocument d : evolvedDocs) {
            int chunks = d.getChunkCount() != null ? d.getChunkCount() : 0;
            for (int i = 0; i < chunks; i++) {
                evolvedChunkIds.add(d.getId() * 10000L + i);
            }
        }
        if (evolvedChunkIds.isEmpty()) return -1;

        Timestamp since = Timestamp.from(LocalDateTime.now().minusDays(30).atZone(ZoneId.systemDefault()).toInstant());
        Set<Long> referencedChunkIds = new HashSet<>();
        for (AiCallLog callLog : callLogRepository.findWithReferencedChunksSince(since)) {
            String json = callLog.getReferencedChunkIds();
            if (json == null || json.isBlank()) continue;
            try {
                List<Number> ids = JSON.parseArray(json, Number.class);
                if (ids != null) ids.forEach(n -> referencedChunkIds.add(n.longValue()));
            } catch (Exception e) {
                log.debug("referencedChunkIds JSON解析失败: {}", e.getMessage());
            }
        }

        long referencedEvolved = referencedChunkIds.stream().filter(evolvedChunkIds::contains).count();
        return (double) referencedEvolved / evolvedChunkIds.size();
    }

    @Override
    public boolean shouldRun() {
        if (!roiThrottleEnabled) {
            return true;
        }
        if (stringRedisTemplate == null) return true;
        try {
            if (!isThrottled()) return true;
            String lastRun = stringRedisTemplate.opsForValue().get(REDIS_LAST_RUN);
            if (lastRun == null) return true;
            long last = Long.parseLong(lastRun);
            boolean allow = System.currentTimeMillis() - last >= THROTTLE_HOURS * 3600_000;
            if (!allow && throttleSkipHintLogged.compareAndSet(false, true)) {
                log.warn("进化引擎因 ROI 已降频，调度会持续跳过直至满 {}h 或手动解除。可执行: redis-cli DEL {} {} {}；或设 EVOLVE_ROI_THROTTLE_ENABLED=false 并重启；或 app.ai.evolve.roi-throttle-enabled=false（仅当当前 Spring profile 的 yml 生效，application-dev 仅在 activeProfiles 含 dev 时加载）。",
                        THROTTLE_HOURS, REDIS_THROTTLE, REDIS_LOW_ROUNDS, REDIS_RECOVERY_ROUNDS);
            }
            return allow;
        } catch (Exception e) {
            log.debug("Redis 不可用，进化引擎 ROI shouldRun 默认允许执行: {}", e.getMessage());
            return true;
        }
    }

    @Override
    public void recordRun(int generatedCount, int indexedCount) {
        if (stringRedisTemplate == null) return;
        try {
            long now = System.currentTimeMillis();
            stringRedisTemplate.opsForValue().set(REDIS_LAST_RUN, String.valueOf(now), 7, TimeUnit.DAYS);

            double rate = generatedCount > 0 ? (double) indexedCount / generatedCount : 0;
            if (isThrottled()) {
                if (rate >= RECOVERY_THRESHOLD) {
                    String r = stringRedisTemplate.opsForValue().get(REDIS_RECOVERY_ROUNDS);
                    int recovery = r != null ? Integer.parseInt(r) : 0;
                    recovery++;
                    stringRedisTemplate.opsForValue().set(REDIS_RECOVERY_ROUNDS, String.valueOf(recovery), 7, TimeUnit.DAYS);
                    if (recovery >= RECOVERY_ROUNDS_TO_RESTORE) {
                        stringRedisTemplate.delete(REDIS_THROTTLE);
                        stringRedisTemplate.delete(REDIS_RECOVERY_ROUNDS);
                        stringRedisTemplate.delete(REDIS_LOW_ROUNDS);
                        log.info("进化引擎 ROI 恢复，解除降频");
                    }
                } else {
                    stringRedisTemplate.delete(REDIS_RECOVERY_ROUNDS);
                }
            } else {
                if (rate < LOW_STORAGE_THRESHOLD) {
                    String r = stringRedisTemplate.opsForValue().get(REDIS_LOW_ROUNDS);
                    int low = r != null ? Integer.parseInt(r) : 0;
                    low++;
                    stringRedisTemplate.opsForValue().set(REDIS_LOW_ROUNDS, String.valueOf(low), 7, TimeUnit.DAYS);
                    if (low >= LOW_ROUNDS_TO_THROTTLE) {
                        stringRedisTemplate.opsForValue().set(REDIS_THROTTLE, "1", 7, TimeUnit.DAYS);
                        log.warn("进化引擎 ROI 过低（连续 {} 轮入库率<30%），自动降频至 48h", low);
                    }
                } else {
                    stringRedisTemplate.delete(REDIS_LOW_ROUNDS);
                }
            }
        } catch (Exception e) {
            log.debug("Redis 不可用，跳过进化 ROI 记录: {}", e.getMessage());
        }
    }

    private boolean isThrottled() {
        if (stringRedisTemplate == null) return false;
        try {
            return "1".equals(stringRedisTemplate.opsForValue().get(REDIS_THROTTLE));
        } catch (Exception e) {
            log.debug("Redis 不可用，isThrottled 视为 false: {}", e.getMessage());
            return false;
        }
    }
}
