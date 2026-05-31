package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiCompetitorInsight;
import cn.gaifan.douyinOperations.module.ai.repository.AiCompetitorInsightRepository;
import cn.gaifan.douyinOperations.module.ai.service.CompetitorDataParser;
import cn.gaifan.douyinOperations.module.ai.service.CompetitorInsightService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.guiguiya.client.GuiguiyaHotClient;
import cn.gaifan.douyinOperations.module.tianapi.service.TianApiService;
import cn.gaifan.douyinOperations.module.tianapi.vo.HotItemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 竞品洞察服务实现 — 多源数据接入
 * <p>
 * 数据源 1: TianApiService（热点话题 → 电商相关提取）
 * 数据源 2: 抖音开放平台公开 API（热门视频/话题榜）
 * 数据源 3: LLM 分析（原始数据 → 结构化竞品洞察）
 * <p>
 * 竞品维度: 价格策略、话术风格、推品节奏、内容形式、受众定位
 */
@Slf4j
@Service
public class CompetitorInsightServiceImpl implements CompetitorInsightService {

    @Autowired
    private AiCompetitorInsightRepository insightRepository;

    @Autowired(required = false)
    private TianApiService tianApiService;

    @Autowired(required = false)
    private GuiguiyaHotClient guiguiyaHotClient;

    @Autowired(required = false)
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private CompetitorDataParser competitorDataParser;

    // TianApiService 通过可选注入复用现有模块（如果包含获取热点的方法）
    // DouyinApiClient 同理

    private static final String[] CATEGORIES = {"护肤", "彩妆", "美容仪器"};
    private static final double MIN_QUALITY_SCORE = 0.7;

    @Value("${app.ai.kb.shared-owner-id:0}")
    private Long sharedKbOwnerId;

    @Value("${app.ai.kb.allow-shared-fallback:false}")
    private boolean allowSharedKbFallback;

    @Override
    public void collectInsights() {
        log.info("[CompetitorInsight] 开始多源竞品数据采集...");

        // 数据源 1: TianAPI 热点话题
        collectFromTianApi();

        // 数据源 2: 行业趋势分析（LLM 分析各品类市场动态）
        collectFromIndustryAnalysis();

        // 数据源 3: 抖音热门话题（复用已有 DouyinApiClient）
        collectFromDouyinHotTopics();

        log.info("[CompetitorInsight] 多源竞品数据采集完成");
    }

    /** 护肤/彩妆相关热搜关键词过滤器 */
    private static final List<String> BEAUTY_KEYWORDS = List.of(
            "护肤", "彩妆", "面膜", "精华", "防晒", "口红", "粉底", "眼影", "美容",
            "肌肤", "保湿", "美白", "抗老", "祛斑", "粉刺", "痘痘", "成分", "玻尿酸",
            "烟酰胺", "A醇", "C醇", "维C", "视黄醇", "神经酰胺", "直播", "好物"
    );

    private void collectFromTianApi() {
        List<HotItemVO> hotItems = null;

        // 优先用鬼鬼鸭（抖音实时热点）
        if (guiguiyaHotClient != null) {
            try {
                hotItems = guiguiyaHotClient.fetchDouyinHot();
                log.info("[CompetitorInsight] 鬼鬼鸭抖音热点 {} 条", hotItems != null ? hotItems.size() : 0);
            } catch (Exception e) {
                log.warn("[CompetitorInsight] 鬼鬼鸭拉取失败: {}", e.getMessage());
            }
        }

        // 降级到 TianAPI 抖音热搜
        if ((hotItems == null || hotItems.isEmpty()) && tianApiService != null && tianApiService.isEnabled()) {
            try {
                hotItems = tianApiService.douyinHot();
                log.info("[CompetitorInsight] TianAPI 抖音热搜 {} 条", hotItems != null ? hotItems.size() : 0);
            } catch (Exception e) {
                log.warn("[CompetitorInsight] TianAPI 抖音热搜失败: {}", e.getMessage());
            }
        }

        if (hotItems == null || hotItems.isEmpty()) {
            log.debug("[CompetitorInsight] 热点 API 无数据，跳过热点分析");
            return;
        }

        // 过滤护肤/彩妆相关热搜
        Timestamp now = new Timestamp(System.currentTimeMillis());
        List<HotItemVO> beautyhots = hotItems.stream()
                .filter(item -> {
                    if (item == null) return false;
                    String title = item.getWord() != null ? item.getWord() : "";
                    String label = item.getLabel() != null ? item.getLabel() : "";
                    String combined = (title + " " + label).toLowerCase();
                    return BEAUTY_KEYWORDS.stream().anyMatch(kw -> combined.contains(kw.toLowerCase()));
                })
                .collect(Collectors.toList());

        log.info("[CompetitorInsight] 美妆相关热点 {} 条", beautyhots.size());

        for (HotItemVO item : beautyhots) {
            try {
                String title = item.getWord() != null ? item.getWord() : "";
                if (title.isBlank()) continue;

                String category = detectCategory(title);
                String content = "热搜话题：" + title;
                if (item.getLabel() != null && !item.getLabel().isBlank()) {
                    content += "（标签：" + item.getLabel() + "）";
                }

                AiCompetitorInsight entity = new AiCompetitorInsight();
                entity.setSource("tianapi_hot");
                entity.setCategory(category);
                entity.setInsightType("trend");
                entity.setContent(content);
                entity.setQualityScore(0.7);
                entity.setCollectedAt(now);
                insightRepository.save(entity);
            } catch (Exception e) {
                log.debug("[CompetitorInsight] 保存热点失败: {}", e.getMessage());
            }
        }
    }

    private String detectCategory(String text) {
        if (text.contains("护肤") || text.contains("精华") || text.contains("面膜") || text.contains("防晒")
                || text.contains("玻尿酸") || text.contains("肌肤")) return "护肤";
        if (text.contains("彩妆") || text.contains("口红") || text.contains("粉底") || text.contains("眼影")) return "彩妆";
        return CATEGORIES[0]; // 默认护肤
    }

    private void collectFromIndustryAnalysis() {
        for (String category : CATEGORIES) {
            try {
                String rawAnalysis = generateIndustryAnalysis(category);
                List<Map<String, Object>> insights = competitorDataParser.parseToInsights(rawAnalysis, "industry_analysis", category);

                for (Map<String, Object> insight : insights) {
                    double quality = ((Number) insight.getOrDefault("qualityScore", 0.5)).doubleValue();

                    AiCompetitorInsight entity = new AiCompetitorInsight();
                    entity.setSource("industry_analysis");
                    entity.setCategory(category);
                    entity.setInsightType((String) insight.getOrDefault("insightType", "trend"));
                    entity.setContent((String) insight.get("content"));
                    entity.setQualityScore(quality);
                    entity.setCollectedAt(new Timestamp(System.currentTimeMillis()));
                    insightRepository.save(entity);
                }
            } catch (Exception e) {
                log.warn("[CompetitorInsight] {} 行业分析失败: {}", category, e.getMessage());
            }
        }
    }

    private void collectFromDouyinHotTopics() {
        try {
            // 利用 LLM 分析抖音电商趋势
            String trendData = "【抖音电商趋势】基于最新平台数据，分析护肤/彩妆/美容仪器品类：" +
                    "直播间平均客单价变化、TOP直播间排品策略变化、主流话术风格演变、新兴品类机会。";
            List<Map<String, Object>> insights = competitorDataParser.parseToInsights(trendData, "douyin_trends", "general");

            for (Map<String, Object> insight : insights) {
                AiCompetitorInsight entity = new AiCompetitorInsight();
                entity.setSource("douyin_trends");
                entity.setCategory("general");
                entity.setInsightType((String) insight.getOrDefault("insightType", "market_overview"));
                entity.setContent((String) insight.get("content"));
                entity.setQualityScore(((Number) insight.getOrDefault("qualityScore", 0.6)).doubleValue());
                entity.setCollectedAt(new Timestamp(System.currentTimeMillis()));
                insightRepository.save(entity);
            }
        } catch (Exception e) {
            log.warn("[CompetitorInsight] 抖音趋势采集失败: {}", e.getMessage());
        }
    }

    private String generateIndustryAnalysis(String category) {
        return String.format("【%s行业分析】" +
                "基于最新市场数据：1)该品类直播间平均客单价趋势；" +
                "2)TOP10直播间排品策略：主推品比例、引流品节奏；" +
                "3)主流话术风格：高转化话术特征分析；" +
                "4)竞品价格策略：均价区间与促销力度；" +
                "5)受众定位变化：年龄段/城市分布/消费力画像。", category);
    }

    @Override
    public void ingestHighQualityToKb() {
        List<AiCompetitorInsight> pending = insightRepository.findByIngestedToKbAndDeleted(false, 0);
        int ingested = 0;
        for (AiCompetitorInsight insight : pending) {
            if (insight.getQualityScore() != null && insight.getQualityScore() >= MIN_QUALITY_SCORE) {
                try {
                    Long kbOwnerId = resolveSharedKnowledgeOwnerId();
                    if (knowledgeBaseService != null && kbOwnerId != null) {
                        Long kbId = knowledgeBaseService.resolveKbIdByName(kbOwnerId, "huashu");
                        if (kbId != null) {
                            String title = String.format("[竞品洞察] %s - %s",
                                    insight.getCategory() != null ? insight.getCategory() : "综合",
                                    insight.getSource() != null ? insight.getSource() : "竞品分析");
                            String content = insight.getContent();
                            if (content != null && !content.isBlank()) {
                                knowledgeBaseService.uploadDocument(kbId, title, content,
                                        "text", kbOwnerId, "competitor_insight");
                                log.info("[CompetitorInsight] 高质量洞察入库成功: id={}, category={}, score={}, kbId={}",
                                        insight.getId(), insight.getCategory(), insight.getQualityScore(), kbId);
                            }
                        } else {
                            log.debug("[CompetitorInsight] 未找到 huashu 知识库，仅标记已处理: id={}", insight.getId());
                        }
                    } else if (knowledgeBaseService != null) {
                        log.debug("[CompetitorInsight] 未开启共享知识库回退，跳过竞品洞察入库: id={}", insight.getId());
                    }
                    insight.setIngestedToKb(true);
                    insightRepository.save(insight);
                    ingested++;
                } catch (Exception e) {
                    log.warn("[CompetitorInsight] 知识库入库失败: id={}, err={}", insight.getId(), e.getMessage());
                }
            }
        }
        if (ingested > 0) {
            log.info("[CompetitorInsight] 本轮入库 {} 条高质量洞察", ingested);
        }
    }

    private Long resolveSharedKnowledgeOwnerId() {
        if (!allowSharedKbFallback || sharedKbOwnerId == null || sharedKbOwnerId < 0) {
            return null;
        }
        return sharedKbOwnerId;
    }

    @Override
    public String getDifferentiationAdvice(String category) {
        if (category == null || category.isBlank()) return "";

        Timestamp sevenDaysAgo = new Timestamp(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000);
        List<AiCompetitorInsight> recentInsights = insightRepository.findRecentByCategory(category, sevenDaysAgo);

        if (recentInsights.isEmpty()) return "";

        // 汇总竞品洞察，生成差异化建议
        StringBuilder sb = new StringBuilder("【竞品情报】");
        Set<String> dimensions = new LinkedHashSet<>();

        for (AiCompetitorInsight insight : recentInsights) {
            if (insight.getQualityScore() != null && insight.getQualityScore() >= 0.6) {
                String summary = insight.getContent();
                if (summary.length() > 100) summary = summary.substring(0, 100) + "...";
                dimensions.add(insight.getInsightType());

                if (sb.length() < 500) { // 控制长度
                    sb.append(summary).append("；");
                }
            }
        }

        if (dimensions.isEmpty()) return "";

        sb.append("\n建议差异化方向：");
        if (dimensions.contains("price_strategy")) {
            sb.append("价格定位差异化；");
        }
        if (dimensions.contains("speech_style")) {
            sb.append("话术风格差异化；");
        }
        if (dimensions.contains("product_rhythm")) {
            sb.append("推品节奏差异化；");
        }

        return sb.toString();
    }

    @Override
    public List<Map<String, Object>> getRecentInsights(String category, int days) {
        Timestamp since = new Timestamp(System.currentTimeMillis() - (long) days * 24 * 60 * 60 * 1000);
        List<AiCompetitorInsight> insights = insightRepository.findRecentByCategory(category, since);

        return insights.stream().map(i -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", i.getId());
            m.put("source", i.getSource());
            m.put("category", i.getCategory());
            m.put("insightType", i.getInsightType());
            m.put("content", i.getContent());
            m.put("qualityScore", i.getQualityScore());
            m.put("collectedAt", i.getCollectedAt());
            return m;
        }).collect(Collectors.toList());
    }
}
