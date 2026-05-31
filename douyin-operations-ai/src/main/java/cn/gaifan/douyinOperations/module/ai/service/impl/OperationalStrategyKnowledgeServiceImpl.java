package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTopic;
import cn.gaifan.douyinOperations.module.ai.entity.AiKbDocument;
import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import cn.gaifan.douyinOperations.module.ai.service.EvolveTopicService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.ai.service.OperationalStrategyKnowledgeService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 将人工运营方法沉淀为可检索、可进化的 AI 学习材料。
 */
@Service
public class OperationalStrategyKnowledgeServiceImpl implements OperationalStrategyKnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(OperationalStrategyKnowledgeServiceImpl.class);
    private static final String SOURCE_TYPE = "ops_strategy";
    private static final String CONTENT_TYPE = "ops_strategy";
    private static final String STRATEGY_TITLE = "抖音运营智能体策略种子：直播排品、商品时长、官方规则消费";
    private static final String VIRAL_PATTERN_SOURCE_TYPE = "viral_pattern_learning";
    private static final String PERFORMANCE_REFLECTION_SOURCE_TYPE = "performance_reflection";

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    private EvolveTopicService evolveTopicService;

    @Resource
    private AiKbDocumentRepository documentRepository;

    @Value("${app.ai.ops-strategy.enabled:true}")
    private boolean enabled;

    @Value("${app.ai.ops-strategy.kb-name:douyin_ops_strategy}")
    private String strategyKbName;

    @Value("${app.ai.ops-strategy.general-kb-name:douyin}")
    private String generalKbName;

    @Value("${app.ai.ops-strategy.violation-kb-name:douyin_weigui}")
    private String violationKbName;

    @Value("${app.ai.ops-strategy.viral-pattern-kb-name:douyin_viral_patterns}")
    private String viralPatternKbName;

    @Value("${app.ai.ops-strategy.performance-kb-name:douyin_performance_reflections}")
    private String performanceKbName;

    @Value("${app.ai.ops-strategy.search-top-k:3}")
    private int searchTopK;

    @Value("${app.ai.kb.shared-owner-id:0}")
    private Long sharedKbOwnerId;

    @Value("${app.ai.kb.allow-shared-fallback:false}")
    private boolean allowSharedKbFallback;

    private final Set<Long> seededUsers = ConcurrentHashMap.newKeySet();

    @Override
    public void ensureSeeded(Long userId) {
        if (!enabled || userId == null || userId <= 0 || seededUsers.contains(userId)) {
            return;
        }
        try {
            Long kbId = ensureKb(userId, strategyKbName,
                    "抖音运营智能体策略知识库：排品顺序、商品角色时长、品类节奏、AI 学习闭环与官方规则消费方式");
            knowledgeBaseService.uploadDocument(
                    kbId,
                    STRATEGY_TITLE,
                    strategySeedMarkdown(),
                    "md",
                    userId,
                    SOURCE_TYPE,
                    CONTENT_TYPE,
                    Map.of(
                            "source", "system_seed",
                            "source_type", SOURCE_TYPE,
                            "contentType", CONTENT_TYPE,
                            "kbPurpose", "douyin_ops_strategy",
                            "version", "2026-05-25"
                    )
            );
            seedEvolveTopics(kbId);
            seededUsers.add(userId);
        } catch (Exception e) {
            log.warn("运营策略知识种子初始化跳过 userId={}, err={}", userId, e.getMessage());
        }
    }

    @Override
    public PromptContext buildLiveGenerationContext(Long userId, String query, String materialType, int maxChars) {
        if (!enabled || userId == null || userId <= 0) {
            return new PromptContext("", List.of(), List.of());
        }
        ensureSeeded(userId);
        String mergedQuery = String.join(" ",
                nonBlank(query, "直播话术 直播排品 商品时长 违规规则"),
                "爆品 控单品 利润品 亏品 平价品 直播违规 官方规则",
                StringUtils.hasText(materialType) ? materialType : "");
        return buildContext(userId, mergedQuery, maxChars, "live");
    }

    @Override
    public PromptContext buildShortVideoGenerationContext(Long userId, String query, int maxChars) {
        if (!enabled || userId == null || userId <= 0) {
            return new PromptContext("", List.of(), List.of());
        }
        ensureSeeded(userId);
        String mergedQuery = String.join(" ",
                nonBlank(query, "短视频脚本 短视频策划 官方规则 违规风险"),
                "短视频违规 千川素材违规 标题 封面 脚本 口播 文案");
        return buildContext(userId, mergedQuery, maxChars, "short_video");
    }

    @Override
    public PromptContext buildViolationRuleContext(Long userId, String query, String scene, int maxChars) {
        if (!enabled || userId == null || userId <= 0) {
            return new PromptContext("", List.of(), List.of());
        }
        ensureSeeded(userId);
        String normalizedScene = StringUtils.hasText(scene) ? scene.trim() : "violation_check";
        String mergedQuery = String.join(" ",
                nonBlank(query, "违规审核 官方规则"),
                "douyin_weigui 官方规则 绝对化 虚假宣传 直播违规 短视频违规 千川素材违规 商品宣传合规",
                normalizedScene);
        return buildContext(userId, mergedQuery, maxChars, normalizedScene, true, false, false);
    }

    @Override
    public PromptContext buildViralPatternContext(Long userId, String query, int maxChars) {
        if (!enabled || userId == null || userId <= 0) {
            return new PromptContext("", List.of(), List.of());
        }
        ensureSeeded(userId);
        String mergedQuery = String.join(" ",
                nonBlank(query, "爆款短视频模式 脚本结构 分镜节奏"),
                "爆款模式 三秒钩子 口播结构 分镜节奏 数字人口播 产品展示 成交转化 复盘模板");
        return buildContext(userId, mergedQuery, maxChars, "viral_pattern", false, true, true);
    }

    @Override
    public void writeViralPatternKnowledge(Long userId, String title, String content, Map<String, String> metadata) {
        writeKnowledgeDocument(userId, viralPatternKbName,
                "爆款模式知识库：采集爆款视频后的结构化脚本、分镜、钩子、成交信号和可复用模板",
                nonBlank(title, "爆款模式结构化沉淀"),
                content,
                VIRAL_PATTERN_SOURCE_TYPE,
                "viral_pattern",
                metadata);
    }

    @Override
    public void writePerformanceReflection(Long userId, String title, String content, Map<String, String> metadata) {
        writeKnowledgeDocument(userId, performanceKbName,
                "发布和直播复盘知识库：成功模板、失败原因、指标反馈和下一轮优化策略",
                nonBlank(title, "运营复盘沉淀"),
                content,
                PERFORMANCE_REFLECTION_SOURCE_TYPE,
                "performance_reflection",
                metadata);
    }

    private PromptContext buildContext(Long userId, String query, int maxChars, String scene) {
        return buildContext(userId, query, maxChars, scene, false, true, false);
    }

    private PromptContext buildContext(Long userId, String query, int maxChars, String scene,
                                       boolean violationOnly, boolean includeStrategy, boolean includeViralPatterns) {
        int budget = Math.max(800, maxChars > 0 ? maxChars : 2600);
        List<KnowledgeBaseService.SearchResult> refs = new ArrayList<>();
        Map<String, List<KnowledgeBaseService.SearchResult>> grouped = new LinkedHashMap<>();
        grouped.put("strategy", includeStrategy ? searchKb(userId, strategyKbName, query) : List.of());
        if (!violationOnly) {
            grouped.put("official_learning", searchOfficialKb(userId, generalKbName, query));
        } else {
            grouped.put("official_learning", List.of());
        }
        grouped.put("violation_rules", searchOfficialKb(userId, violationKbName, query));
        grouped.put("viral_patterns", includeViralPatterns ? searchKb(userId, viralPatternKbName, query) : List.of());
        grouped.put("performance_reflections", includeViralPatterns ? searchKb(userId, performanceKbName, query) : List.of());
        grouped.values().forEach(refs::addAll);
        List<OperationalStrategyKnowledgeService.OfficialReference> officialReferences = buildOfficialReferences(grouped);

        StringBuilder sb = new StringBuilder();
        sb.append("\n<douyin_ops_learning_context scene=\"").append(scene).append("\">\n");
        sb.append("<usage>这是 AI 学习中心沉淀的运营策略、抖音官方资料和违规约束。生成内容必须原创；违规库只作为红线和风险检查，禁止把违规案例改写成可执行玩法。</usage>\n");
        sb.append("<core_strategy>\n").append(coreStrategyFallback()).append("\n</core_strategy>\n");

        int used = sb.length();
        used = appendRefs(sb, "strategy_refs", grouped.get("strategy"), used, budget);
        used = appendRefs(sb, "official_learning_refs", grouped.get("official_learning"), used, budget);
        used = appendRefs(sb, "violation_rule_refs", grouped.get("violation_rules"), used, budget);
        used = appendRefs(sb, "viral_pattern_refs", grouped.get("viral_patterns"), used, budget);
        appendRefs(sb, "performance_reflection_refs", grouped.get("performance_reflections"), used, budget);
        sb.append("</douyin_ops_learning_context>\n");
        return new PromptContext(sb.toString(), refs, officialReferences);
    }

    private List<OperationalStrategyKnowledgeService.OfficialReference> buildOfficialReferences(
            Map<String, List<KnowledgeBaseService.SearchResult>> grouped) {
        List<OperationalStrategyKnowledgeService.OfficialReference> refs = new ArrayList<>();
        appendOfficialReferences(refs, generalKbName, "official_learning", grouped.get("official_learning"));
        appendOfficialReferences(refs, violationKbName, "violation_rule", grouped.get("violation_rules"));
        return refs;
    }

    private void appendOfficialReferences(
            List<OperationalStrategyKnowledgeService.OfficialReference> target,
            String kbName,
            String refType,
            List<KnowledgeBaseService.SearchResult> results) {
        if (target == null || results == null || results.isEmpty()) {
            return;
        }
        Set<Long> seen = new LinkedHashSet<>();
        for (KnowledgeBaseService.SearchResult result : results) {
            if (result == null) continue;
            Long key = result.chunkId() != null ? result.chunkId() : result.docId();
            if (key != null && !seen.add(key)) continue;
            target.add(new OperationalStrategyKnowledgeService.OfficialReference(
                    kbName,
                    refType,
                    result.docId(),
                    result.chunkId(),
                    nonBlank(result.title(), "抖音官方资料"),
                    truncate(result.content(), 180),
                    result.score()
            ));
        }
    }

    private int appendRefs(StringBuilder sb, String tag, List<KnowledgeBaseService.SearchResult> results, int used, int budget) {
        if (results == null || results.isEmpty() || used >= budget) {
            return used;
        }
        sb.append("<").append(tag).append(">\n");
        int index = 1;
        Set<Long> seen = new LinkedHashSet<>();
        for (KnowledgeBaseService.SearchResult result : results) {
            if (result == null || !StringUtils.hasText(result.content())) continue;
            Long key = result.chunkId() != null ? result.chunkId() : result.docId();
            if (key != null && !seen.add(key)) continue;
            String content = truncate(result.content(), 420);
            if (used + content.length() + 120 > budget) break;
            sb.append("<ref id=\"").append(index++).append("\" title=\"")
                    .append(escapeXml(truncate(nonBlank(result.title(), "知识片段"), 80)))
                    .append("\" score=\"").append(String.format(java.util.Locale.ROOT, "%.2f", result.score()))
                    .append("\">")
                    .append(escapeXml(content))
                    .append("</ref>\n");
            used += content.length() + 120;
        }
        sb.append("</").append(tag).append(">\n");
        return used;
    }

    private List<KnowledgeBaseService.SearchResult> searchKb(Long userId, String kbName, String query) {
        ResolvedKb kb = resolveKb(userId, kbName);
        if (kb == null) return List.of();
        try {
            return knowledgeBaseService.hybridSearch(kb.kbId(), query, Math.max(1, searchTopK), kb.accessUserId(), null, true, true);
        } catch (Exception e) {
            log.debug("运营策略上下文检索跳过 kb={}, userId={}, err={}", kbName, userId, e.getMessage());
            return List.of();
        }
    }

    private List<KnowledgeBaseService.SearchResult> searchOfficialKb(Long userId, String kbName, String query) {
        ResolvedKb kb = resolveKb(userId, kbName);
        if (kb == null) return List.of();
        try {
            List<KnowledgeBaseService.SearchResult> candidates = knowledgeBaseService.hybridSearch(
                    kb.kbId(), query, Math.max(8, searchTopK * 8), kb.accessUserId(), null, true, true);
            List<KnowledgeBaseService.SearchResult> official = filterOfficialResults(candidates);
            if (!official.isEmpty()) {
                return official.stream().limit(Math.max(1, searchTopK)).toList();
            }
            return fallbackOfficialDocs(kb.kbId());
        } catch (Exception e) {
            log.debug("抖音官方上下文检索跳过 kb={}, userId={}, err={}", kbName, userId, e.getMessage());
            return fallbackOfficialDocs(kb.kbId());
        }
    }

    private List<KnowledgeBaseService.SearchResult> filterOfficialResults(List<KnowledgeBaseService.SearchResult> candidates) {
        if (candidates == null || candidates.isEmpty()) return List.of();
        List<Long> docIds = candidates.stream()
                .map(KnowledgeBaseService.SearchResult::docId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (docIds.isEmpty()) return List.of();
        Map<Long, AiKbDocument> docs = documentRepository.findByIdIn(docIds).stream()
                .collect(java.util.stream.Collectors.toMap(AiKbDocument::getId, d -> d, (a, b) -> a));
        return candidates.stream()
                .filter(result -> {
                    AiKbDocument doc = docs.get(result.docId());
                    return doc != null && "douyin_school_official".equals(doc.getSourceType());
                })
                .toList();
    }

    private List<KnowledgeBaseService.SearchResult> fallbackOfficialDocs(Long kbId) {
        if (kbId == null) return List.of();
        try {
            List<AiKbDocument> docs = documentRepository.findTopCitedByKbAndSourceType(
                    kbId, "douyin_school_official", 0L, PageRequest.of(0, Math.max(1, searchTopK)));
            if (docs == null || docs.isEmpty()) {
                docs = documentRepository.findByKbIdAndSourceTypeAndDeleted(kbId, "douyin_school_official", 0)
                        .stream()
                        .limit(Math.max(1, searchTopK))
                        .toList();
            }
            return docs.stream()
                    .map(doc -> new KnowledgeBaseService.SearchResult(
                            doc.getId(),
                            doc.getTitle(),
                            doc.getContent(),
                            0.01,
                            "official_fallback",
                            doc.getId() != null ? doc.getId() * 10000 : null,
                            List.of("douyin_school_official")
                    ))
                    .toList();
        } catch (Exception e) {
            log.debug("抖音官方文档兜底失败 kbId={}, err={}", kbId, e.getMessage());
            return List.of();
        }
    }

    private ResolvedKb resolveKb(Long userId, String kbName) {
        if (!StringUtils.hasText(kbName)) return null;
        Long ownKb = knowledgeBaseService.resolveKbIdByName(userId, kbName.trim());
        if (ownKb != null) return new ResolvedKb(ownKb, userId);
        if (!allowSharedKbFallback || sharedKbOwnerId == null || sharedKbOwnerId < 0) return null;
        Long sharedKb = knowledgeBaseService.resolveKbIdByName(sharedKbOwnerId, kbName.trim());
        return sharedKb == null ? null : new ResolvedKb(sharedKb, sharedKbOwnerId);
    }

    private Long ensureKb(Long userId, String kbName, String description) {
        Long kbId = knowledgeBaseService.resolveKbIdByName(userId, kbName);
        if (kbId != null) return kbId;
        return knowledgeBaseService.createKnowledgeBase(kbName, description, userId).getId();
    }

    private void writeKnowledgeDocument(Long userId, String kbName, String kbDescription, String title,
                                        String content, String sourceType, String contentType,
                                        Map<String, String> metadata) {
        if (!enabled || userId == null || userId <= 0 || !StringUtils.hasText(kbName) || !StringUtils.hasText(content)) {
            return;
        }
        try {
            Long kbId = ensureKb(userId, kbName, kbDescription);
            Map<String, String> meta = new LinkedHashMap<>();
            if (metadata != null) {
                meta.putAll(metadata);
            }
            meta.putIfAbsent("source_type", sourceType);
            meta.putIfAbsent("contentType", contentType);
            meta.putIfAbsent("kbPurpose", kbName);
            knowledgeBaseService.uploadDocument(
                    kbId,
                    truncate(title, 120),
                    content,
                    "md",
                    userId,
                    sourceType,
                    contentType,
                    meta
            );
        } catch (Exception e) {
            log.warn("运营闭环知识写入失败 kb={}, userId={}, err={}", kbName, userId, e.getMessage());
        }
    }

    private void seedEvolveTopics(Long kbId) {
        List<AiEvolveTopic> existing = evolveTopicService.listTopics(kbId, null, false);
        for (String topic : strategyTopics()) {
            if (evolveTopicService.isTopicDuplicate(topic, existing, 0.88)) {
                continue;
            }
            AiEvolveTopic row = new AiEvolveTopic();
            row.setKbId(kbId);
            row.setTopic(topic);
            row.setCategory("douyin_ops_strategy");
            row.setPriority(1);
            row.setSource(SOURCE_TYPE);
            row.setStatus(1);
            row.setDeleted(0);
            AiEvolveTopic saved = evolveTopicService.saveTopic(row);
            existing = new ArrayList<>(existing);
            existing.add(saved);
        }
    }

    private List<String> strategyTopics() {
        return List.of(
                "直播排品策略：爆品深度讲解、控单品憋单、利润品重点推介、亏品引流、平价品日常承接的排序与转化优化",
                "商品角色时长策略：按爆品、控单品、利润品、亏品、平价品和品类差异自动推荐讲解时长、词量和节奏",
                "抖音官方规则融合：把 douyin 官方学习资料作为事实依据，把 douyin_weigui 违规规则作为硬约束注入生成链路",
                "短视频从策划到成片：按选题、钩子、证据、分镜、素材、字幕、发布标签和评论互动形成可执行生产方案",
                "AI 学习闭环：根据话术效果、商品类型、品类、违规检查和用户反馈持续更新推荐排序、时长和脚本结构"
        );
    }

    private String strategySeedMarkdown() {
        return """
                # 抖音运营智能体策略种子：直播排品、商品时长、官方规则消费

                ## 直播推荐排序

                推荐排序：爆品（深度讲解） -> 控单品（憋单） -> 利润品（重点推介） -> 亏品（引流） -> 平价品（日常）。

                - 爆品：承担信任建立、卖点证明、评论互动和转化放大，适合深度讲解。
                - 控单品：承担节奏控制、福利悬念、库存/限量心智，适合憋单蓄水。
                - 利润品：承担利润和客单价，适合重点推介、组合装、复购理由。
                - 亏品：承担引流和拉新，适合短时强钩子，不宜长时间消耗主节奏。
                - 平价品：承担日常承接、补单和过渡，适合快速过品与场景化推荐。

                ## 商品角色默认时长

                - 爆品：180-300 秒，约 540-1200 字，必须讲透痛点、证据、差异、使用场景和下单理由。
                - 控单品：120-240 秒，约 360-960 字，必须强调节奏、福利节点、限时机制和互动口令。
                - 利润品：90-150 秒，约 270-600 字，必须讲清价值感、复购、组合和利润承接。
                - 亏品：30-60 秒，约 90-240 字，必须快速引流、明确利益点，避免虚假福利承诺。
                - 平价品：45-75 秒，约 135-300 字，必须口语化、轻决策、适合作为日常过渡。

                ## 品类时长建议

                - 美妆护肤：证据链更重要，爆品/利润品可上浮 20%，必须规避医疗化、绝对化和夸大功效。
                - 食品饮料：场景和口感更重要，亏品/平价品可短平快，必须规避治疗、保健虚假承诺。
                - 服饰鞋包：试穿、尺码、材质和搭配更重要，适合用场景分段，不宜堆参数。
                - 家清百货：痛点演示和对比更重要，控单品适合用限时机制，但不得编造库存。
                - 知识课程/服务：信任和案例更重要，必须规避保底收益、快速暴富、诱导转账。

                ## AI 学习中心消费规则

                - huashu：作为表达方式、节奏、顺口溜、歇后语、金句、互动玩法的素材来源。
                - douyin：作为抖音官方学习资料、运营规则、平台事实和行业玩法来源。
                - douyin_weigui：只作为硬约束和风险检查来源，禁止把违规案例包装成玩法。
                - douyin_ops_strategy：沉淀本系统的运营策略、排序策略、品类时长策略和使用反馈。

                ## 生成要求

                - 直播话术生成必须同时考虑商品角色、品类、场次目标、官方规则和违规风险。
                - 短视频脚本策划必须覆盖选题、前三秒钩子、证据、分镜、素材、字幕、发布和合规检查。
                - 每次生成后应把效果分、人工反馈、违规检查、商品角色和品类作为下一轮学习依据。
                """;
    }

    private String coreStrategyFallback() {
        return """
                推荐排序：爆品（深度讲解） -> 控单品（憋单） -> 利润品（重点推介） -> 亏品（引流） -> 平价品（日常）。
                直播生成必须按商品角色控制时长和词量：爆品 180-300 秒，控单品 120-240 秒，利润品 90-150 秒，亏品 30-60 秒，平价品 45-75 秒。
                美妆护肤、食品、服饰、家清、知识课程等品类需要按证据链、场景、尺码材质、演示、收益风险分别调整节奏。
                douyin 官方学习资料用于平台事实和运营方法；douyin_weigui 违规知识用于红线校验；huashu 用于表达素材；douyin_ops_strategy 用于沉淀系统自己的最佳实践。
                """;
    }

    private static String nonBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private static String truncate(String value, int max) {
        if (value == null) return "";
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }

    private static String escapeXml(String raw) {
        if (raw == null) return "";
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private record ResolvedKb(Long kbId, Long accessUserId) {
    }
}
