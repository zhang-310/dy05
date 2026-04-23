package cn.gaifan.douyinOperations.module.shortvideo.service;

import cn.gaifan.douyinOperations.common.config.BusinessParamConfig;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvHotTopic;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvHotTopicRepository;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 热点时间窗口服务：判断热点所处阶段（黄金/白银/青铜/过时）并返回内容策略建议。
 */
@Service
public class HotspotWindowService {

    @Resource
    private BusinessParamConfig businessParamConfig;

    @Autowired(required = false)
    private SvHotTopicRepository svHotTopicRepository;

    /** H-2：多源节奏代理窗口（天）：统计该窗口内同步入库的热点条数 */
    @Value("${app.shortvideo.hotspot.recency-window-days:14}")
    private int hotspotRecencyWindowDays;

    /** H-2：追加到热点时效性 prompt 的可选运营文案（非统计模型） */
    @Value("${app.shortvideo.hotspot.prompt-extra:}")
    private String hotspotPromptExtra;

    /** H-2：prompt-extra 最大字符数（0=不截断，避免过长运营片段撑爆 prompt） */
    @Value("${app.shortvideo.hotspot.prompt-extra-max-chars:0}")
    private int hotspotPromptExtraMaxChars;

    public enum HotspotTier {
        GOLD("黄金期", "热点刚爆发，立即跟进，标题直接蹭热点，预计流量最大"),
        SILVER("白银期", "热点已发酵，可做深度解读/二创，带个人观点"),
        BRONZE("青铜期", "热点进入长尾，适合做总结/盘点类内容，竞争较小"),
        EXPIRED("过时期", "热点已过时，不建议跟进，除非有独特角度");

        private final String label;
        private final String strategy;

        HotspotTier(String label, String strategy) {
            this.label = label;
            this.strategy = strategy;
        }

        public String getLabel() { return label; }
        public String getStrategy() { return strategy; }
    }

    public HotspotTier classify(Timestamp hotspotCreateTime) {
        if (hotspotCreateTime == null) return HotspotTier.EXPIRED;
        BusinessParamConfig.HotspotWindow hw = businessParamConfig.getHotspot();
        long daysSince = Duration.between(hotspotCreateTime.toInstant(), Instant.now()).toDays();
        if (daysSince <= hw.getGoldDays()) return HotspotTier.GOLD;
        if (daysSince <= hw.getSilverDays()) return HotspotTier.SILVER;
        if (daysSince <= hw.getBronzeDays()) return HotspotTier.BRONZE;
        return HotspotTier.EXPIRED;
    }

    /**
     * 为话术/短视频生成提供热点时效性 prompt 增强
     */
    public String buildHotspotWindowPrompt(Timestamp hotspotCreateTime) {
        HotspotTier tier = classify(hotspotCreateTime);
        String base = String.format("【热点时效性：%s】%s", tier.getLabel(), tier.getStrategy());
        if (StringUtils.hasText(hotspotPromptExtra)) {
            String extra = hotspotPromptExtra.trim();
            int cap = hotspotPromptExtraMaxChars;
            if (cap > 0 && extra.length() > cap) {
                extra = extra.substring(0, cap) + "…";
            }
            return base + "\n" + extra;
        }
        return base;
    }

    /**
     * H-2：基于本库近期热点入库间隔的轻量「节奏」信号（非 Prophet；多源依赖同步任务）。
     */
    public Map<String, Object> recencySignals(Timestamp topicCreateTime) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (svHotTopicRepository == null || hotspotRecencyWindowDays <= 0) {
            m.put("recencySignalsEnabled", false);
            return m;
        }
        m.put("recencySignalsEnabled", true);
        m.put("recencyWindowDays", hotspotRecencyWindowDays);
        Timestamp since = Timestamp.from(Instant.now().minus(Duration.ofDays(hotspotRecencyWindowDays)));
        try {
            m.put("topicsSyncedInWindow", svHotTopicRepository.countByCreateTimeAfter(since));
        } catch (Exception ex) {
            m.put("topicsSyncedInWindow", 0);
            m.put("recencyError", ex.getMessage() != null ? ex.getMessage() : "count_failed");
        }
        try {
            Map<String, Long> bySource = new LinkedHashMap<>();
            for (Object[] row : svHotTopicRepository.countGroupedBySourceSince(since)) {
                if (row == null || row.length < 2 || row[0] == null) {
                    continue;
                }
                String src = String.valueOf(row[0]).trim().toLowerCase(Locale.ROOT);
                long cnt = row[1] instanceof Number n ? n.longValue() : 0L;
                bySource.put(src.isEmpty() ? "unknown" : src, cnt);
            }
            m.put("topicsBySourceInWindow", bySource);
        } catch (Exception ex) {
            m.put("topicsBySourceInWindow", Map.of());
            m.put("topicsBySourceError", ex.getMessage() != null ? ex.getMessage() : "group_failed");
        }
        long ageDays = topicCreateTime == null ? -1L
                : Duration.between(topicCreateTime.toInstant(), Instant.now()).toDays();
        m.put("topicAgeDays", ageDays);
        try {
            List<SvHotTopic> recent = svHotTopicRepository.findTop8ByOrderByCreateTimeDesc();
            double avgGap = averageGapDays(recent);
            m.put("avgDaysBetweenRecentTopics", avgGap);
            if (avgGap > 0 && ageDays >= 0) {
                m.put("simpleRecencyForecastNote",
                        ageDays <= avgGap * 1.5 ? "相对近期入库间隔：节奏偏早" : "相对近期入库间隔：可能已进入长尾");
            }
        } catch (Exception ex) {
            m.put("avgDaysBetweenRecentTopics", null);
            m.put("recencySeriesError", ex.getMessage() != null ? ex.getMessage() : "series_failed");
        }
        m.put("dataSource", "local_sv_hot_topic_sync_proxy");
        return m;
    }

    private static double averageGapDays(List<SvHotTopic> recent) {
        if (recent == null || recent.size() < 2) {
            return 0;
        }
        long total = 0;
        int pairs = 0;
        for (int i = 0; i < recent.size() - 1; i++) {
            Timestamp a = recent.get(i).getCreateTime();
            Timestamp b = recent.get(i + 1).getCreateTime();
            if (a == null || b == null) {
                continue;
            }
            long days = Math.abs(Duration.between(a.toInstant(), b.toInstant()).toDays());
            total += days;
            pairs++;
        }
        return pairs == 0 ? 0 : Math.round(total * 10.0 / pairs) / 10.0;
    }
}
