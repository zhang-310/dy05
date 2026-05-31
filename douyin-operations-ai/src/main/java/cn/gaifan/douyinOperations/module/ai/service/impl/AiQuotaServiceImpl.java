package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.config.AiRuntimeConfig;
import cn.gaifan.douyinOperations.module.ai.entity.AiCallQuota;
import cn.gaifan.douyinOperations.module.ai.repository.AiCallLogRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiCallQuotaRepository;
import cn.gaifan.douyinOperations.module.ai.service.AiQuotaService;
import cn.gaifan.douyinOperations.module.config.service.ConfigService;
import cn.gaifan.douyinOperations.module.config.vo.ConfigSaveVO;
import cn.gaifan.douyinOperations.module.config.vo.ConfigVO;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AiQuotaServiceImpl implements AiQuotaService {

    private static final Logger log = LoggerFactory.getLogger(AiQuotaServiceImpl.class);
    private static final int FALLBACK_MAX_COUNT = 100;
    private static final String FEATURE_ALL = "all";
    private static final List<String> FEATURES = List.of("script_gen", "kb_search", "image_gen", "tts");

    @Value("${app.ai.quota.daily-max:5000}")
    private int defaultMaxCount;

    @Resource
    private AiCallQuotaRepository aiCallQuotaRepository;

    @Resource
    private AiCallLogRepository aiCallLogRepository;

    @Resource
    private ConfigService configService;

    @Override
    public QuotaInfo getQuota(Long userId) {
        if (userId == null) return new QuotaInfo(0, defaultMaxCount, defaultMaxCount);
        Date today = Date.valueOf(LocalDate.now());
        AiCallQuota quota = aiCallQuotaRepository.findByUserIdAndQuotaDate(userId, today)
                .orElseGet(() -> createOrGet(userId, today));
        double usedVal = quota.getUsedUnits() != null ? quota.getUsedUnits().doubleValue() : (quota.getUsedCount() != null ? quota.getUsedCount() : 0);
        int used = (int) Math.ceil(usedVal);
        int max = quota.getMaxCount() != null ? quota.getMaxCount() : defaultMaxCount;
        return new QuotaInfo(used, max, Math.max(0, max - used));
    }

    @Override
    public void ensureQuota(Long userId) {
        if (userId == null) return;
        if (!hasQuota(userId)) {
            throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED, "当日 AI 调用额度已用完");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void consume(Long userId) {
        consume(userId, 1.0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void consume(Long userId, double coefficient) {
        if (userId == null) return;
        if (coefficient <= 0) return;
        Date today = Date.valueOf(LocalDate.now());
        AiCallQuota quota = aiCallQuotaRepository.findByUserIdAndQuotaDate(userId, today)
                .orElseGet(() -> createOrGet(userId, today));
        BigDecimal prev = quota.getUsedUnits() != null ? quota.getUsedUnits() : BigDecimal.ZERO;
        quota.setUsedUnits(prev.add(BigDecimal.valueOf(coefficient)));
        quota.setUsedCount((int) Math.ceil(quota.getUsedUnits().doubleValue()));
        aiCallQuotaRepository.save(quota);
    }

    @Override
    public boolean hasQuota(Long userId) {
        return getQuota(userId).remaining() > 0;
    }

    @Override
    public Map<String, Object> getAdminQuotaOverview() {
        Date today = Date.valueOf(LocalDate.now());
        int globalMax = resolveDailyMax();
        int usedToday = 0;
        int maxToday = globalMax;
        List<Object[]> quotaRows = aiCallQuotaRepository.aggregateByDateRange(today, today);
        if (!quotaRows.isEmpty()) {
            Object[] row = quotaRows.get(0);
            usedToday = toInt(row[1]);
            maxToday = toInt(row[2]);
        }

        Map<String, Integer> featureUsage = aggregateFeatureUsage(
                startOfDay(LocalDate.now()),
                startOfDay(LocalDate.now().plusDays(1)));

        List<Map<String, Object>> items = new ArrayList<>();
        items.add(buildQuotaItem("overall", maxToday, usedToday, "次", "今日"));
        for (String feature : FEATURES) {
            int limit = resolveFeatureLimit(feature, globalMax);
            int used = featureUsage.getOrDefault(feature, 0);
            Map<String, Object> item = buildQuotaItem(feature, limit, used, "次", "今日");
            items.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("dailyMax", globalMax);
        for (Map<String, Object> item : items) {
            result.put(String.valueOf(item.get("feature")), item);
        }
        return result;
    }

    @Override
    public PageResultVO<Map<String, Object>> getQuotaHistory(int page, int rows, String feature) {
        int safePage = Math.max(page, 0);
        int safeRows = Math.min(Math.max(rows, 1), 60);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(29);

        List<Map<String, Object>> list = new ArrayList<>();
        if (feature == null || feature.isBlank() || FEATURE_ALL.equalsIgnoreCase(feature)) {
            int defaultMax = resolveDailyMax();
            List<Object[]> quotaRows = aiCallQuotaRepository.aggregateByDateRange(Date.valueOf(startDate), Date.valueOf(endDate));
            for (Object[] row : quotaRows) {
                LocalDate d = toLocalDate(row[0]);
                int used = toInt(row[1]);
                int max = toInt(row[2]);
                list.add(historyRow(FEATURE_ALL, used, max > 0 ? max : defaultMax, "daily", d));
            }
        } else {
            String normalizedFeature = normalizeFeature(feature);
            int limit = resolveFeatureLimit(normalizedFeature, resolveDailyMax());
            Map<LocalDate, Integer> usageByDay = aggregateFeatureUsageByDay(
                    startOfDay(startDate),
                    startOfDay(endDate.plusDays(1)),
                    normalizedFeature);
            for (Map.Entry<LocalDate, Integer> entry : usageByDay.entrySet()) {
                list.add(historyRow(normalizedFeature, entry.getValue(), limit, "daily", entry.getKey()));
            }
        }

        list.sort((a, b) -> String.valueOf(b.get("date")).compareTo(String.valueOf(a.get("date"))));
        int start = safePage * safeRows;
        int end = Math.min(start + safeRows, list.size());
        List<Map<String, Object>> pageList = start < list.size() ? list.subList(start, end) : List.of();
        return PageResultVO.of((long) list.size(), pageList, safePage, safeRows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuotaLimits(Map<String, Object> params, Long operatorId) {
        if (params == null || params.isEmpty()) {
            return;
        }
        Integer dailyMax = readInt(params, "dailyMax");
        if (dailyMax == null) {
            dailyMax = readInt(params, "maxCount");
        }
        if (dailyMax != null && dailyMax > 0) {
            saveConfigValue(AiRuntimeConfig.KEY_QUOTA_DAILY_MAX, String.valueOf(dailyMax), operatorId, "AI 日总额度");
            aiCallQuotaRepository.updateMaxCountByQuotaDate(Date.valueOf(LocalDate.now()), dailyMax);
        }

        for (String feature : FEATURES) {
            Integer limit = readFeatureLimit(params, feature);
            if (limit != null && limit > 0) {
                saveConfigValue(featureLimitKey(feature), String.valueOf(limit), operatorId, "AI 功能额度 " + feature);
            }
        }
    }

    private int resolveDailyMax() {
        if (configService == null) return defaultMaxCount;
        String v = configService.getRawValueByKey(AiRuntimeConfig.KEY_QUOTA_DAILY_MAX);
        if (v != null && !v.isBlank()) {
            try {
                return Integer.parseInt(v.trim());
            } catch (NumberFormatException e) {
                log.debug("日额度配置值解析失败: {}", e.getMessage());
            }
        }
        return defaultMaxCount;
    }

    private AiCallQuota createOrGet(Long userId, Date date) {
        return aiCallQuotaRepository.findByUserIdAndQuotaDate(userId, date)
                .orElseGet(() -> {
                    int max = resolveDailyMax();
                    AiCallQuota q = new AiCallQuota();
                    q.setUserId(userId);
                    q.setQuotaDate(date);
                    q.setUsedCount(0);
                    q.setMaxCount(max);
                    return aiCallQuotaRepository.save(q);
                });
    }

    private int resolveFeatureLimit(String feature, int fallback) {
        if (configService == null || feature == null || feature.isBlank()) {
            return fallback;
        }
        String v = configService.getRawValueByKey(featureLimitKey(feature));
        if (v != null && !v.isBlank()) {
            try {
                return Integer.parseInt(v.trim());
            } catch (NumberFormatException e) {
                log.debug("特性限制配置值解析失败: {}", e.getMessage());
            }
        }
        return fallback;
    }

    private Map<String, Integer> aggregateFeatureUsage(Timestamp start, Timestamp end) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String feature : FEATURES) {
            result.put(feature, 0);
        }
        List<Object[]> rows = aiCallLogRepository.aggregateDailyByCallType(start, end);
        for (Object[] row : rows) {
            String callType = row[1] != null ? row[1].toString() : "";
            int count = toInt(row[2]);
            String feature = mapCallTypeToFeature(callType);
            if (feature != null) {
                result.merge(feature, count, Integer::sum);
            }
        }
        return result;
    }

    private Map<LocalDate, Integer> aggregateFeatureUsageByDay(Timestamp start, Timestamp end, String feature) {
        Map<LocalDate, Integer> result = new LinkedHashMap<>();
        List<Object[]> rows = aiCallLogRepository.aggregateDailyByCallType(start, end);
        for (Object[] row : rows) {
            String callType = row[1] != null ? row[1].toString() : "";
            String mappedFeature = mapCallTypeToFeature(callType);
            if (!feature.equals(mappedFeature)) {
                continue;
            }
            LocalDate date = toLocalDate(row[0]);
            int count = toInt(row[2]);
            result.merge(date, count, Integer::sum);
        }
        return result;
    }

    private Map<String, Object> buildQuotaItem(String feature, int limit, int used, String unit, String period) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("feature", feature);
        item.put("limit", limit);
        item.put("used", used);
        item.put("unit", unit);
        item.put("period", period);
        return item;
    }

    private Map<String, Object> historyRow(String feature, int used, int limit, String period, LocalDate date) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", feature + "-" + date);
        row.put("feature", feature);
        row.put("used", used);
        row.put("limit", limit);
        row.put("period", period);
        row.put("unit", "次");
        row.put("date", date.toString());
        row.put("createTime", date.atStartOfDay().toString());
        return row;
    }

    private Integer readFeatureLimit(Map<String, Object> params, String feature) {
        Integer limit = readInt(params, feature + "_limit");
        if (limit != null) {
            return limit;
        }
        Object nested = params.get(feature);
        if (nested instanceof Map<?, ?> nestedMap) {
            Object value = nestedMap.get("limit");
            if (value instanceof Number n) {
                return n.intValue();
            }
            if (value instanceof String s && !s.isBlank()) {
                try {
                    return Integer.parseInt(s.trim());
                } catch (NumberFormatException e) {
                    log.debug("特性限制嵌套值解析失败: {}", e.getMessage());
                }
            }
        }
        return null;
    }

    private Integer readInt(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException e) {
                log.debug("整数参数解析失败: {}", e.getMessage());
            }
        }
        return null;
    }

    private void saveConfigValue(String key, String value, Long operatorId, String remark) {
        ConfigVO existing = configService.getByKey(key);
        ConfigSaveVO vo = new ConfigSaveVO();
        if (existing != null) {
            vo.setId(existing.getId());
        }
        vo.setConfigKey(key);
        vo.setConfigValue(value);
        vo.setConfigGroup("ai.quota");
        vo.setRemark(remark);
        vo.setValueType("integer");
        configService.save(vo, operatorId);
    }

    private String featureLimitKey(String feature) {
        return "ai.quota.feature." + feature + ".limit";
    }

    private String normalizeFeature(String feature) {
        String f = feature.trim().toLowerCase(Locale.ROOT);
        return FEATURES.contains(f) ? f : FEATURE_ALL;
    }

    private String mapCallTypeToFeature(String callType) {
        if (callType == null || callType.isBlank()) {
            return null;
        }
        String c = callType.toLowerCase(Locale.ROOT);
        if ("kb_search".equals(c)) {
            return "kb_search";
        }
        if ("tts".equals(c) || c.contains("voice_clone")) {
            return "tts";
        }
        if (Set.of("text2img", "img2img", "edit_image").contains(c) || c.contains("image")) {
            return "image_gen";
        }
        if (c.startsWith("live_script")
                || c.startsWith("short_video")
                || c.startsWith("huashu")
                || c.startsWith("content-diagnosis")
                || c.startsWith("product-diagnosis")
                || c.startsWith("rhythm-diagnosis")
                || c.startsWith("live_analysis")) {
            return "script_gen";
        }
        return null;
    }

    private Timestamp startOfDay(LocalDate date) {
        return Timestamp.valueOf(LocalDateTime.of(date, LocalTime.MIN));
    }

    private LocalDate toLocalDate(Object raw) {
        if (raw instanceof Date d) {
            return d.toLocalDate();
        }
        if (raw instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime().toLocalDate();
        }
        if (raw instanceof java.util.Date d) {
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return LocalDate.parse(String.valueOf(raw));
    }

    private int toInt(Object raw) {
        if (raw instanceof Number n) {
            return n.intValue();
        }
        if (raw == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(raw));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
