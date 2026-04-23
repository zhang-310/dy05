package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.*;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolvePendingDeepenRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveTopicRepository;
import cn.gaifan.douyinOperations.module.ai.service.*;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 进化主题处理器：负责上下文收集、进化角度选择、主题扩展等与主题相关的进化处理逻辑。
 */
@Component
public class EvolveTopicProcessor {

    private static final Logger log = LoggerFactory.getLogger(EvolveTopicProcessor.class);

    static final String[] EVOLVE_ANGLES = {
            "失败复盘", "可执行步骤", "数据量化", "行业标准", "跨域迁移",
            "工具实操", "防踩坑清单", "因果链", "前提显式", "反向思考", "自我纠错"
    };

    @Value("${app.ai.evolve.context-max-chars:32000}")
    private int contextMaxChars;

    @Value("${app.ai.evolve.topic-pool-max:1000}")
    private int topicPoolMax;

    @Value("${app.ai.evolve.angle-strategy:round_robin}")
    private String evolveAngleStrategy;

    @Resource
    private AiEvolveTopicRepository topicRepository;

    @Resource
    private cn.gaifan.douyinOperations.module.ai.repository.AiEvolveTaskRepository taskRepository;

    @Resource
    private AiEvolvePendingDeepenRepository pendingDeepenRepository;

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    private LlmClient llmClient;

    @Resource
    private AiPromptConfigService aiPromptConfigService;

    @Resource
    private EvolveTopicService evolveTopicService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ContentEffectivenessService contentEffectivenessService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private EvolveDocumentLockService evolveDocumentLockService;

    // ---- context gathering ----

    String gatherContext(Long kbId, List<AiEvolveTopic> topics, long effectiveUserId) {
        StringBuilder sb = new StringBuilder();
        Set<String> seen = new HashSet<>();
        Set<Long> citedDocIds = new HashSet<>();
        Map<Long, String> docLocks = new HashMap<>();
        try {
            for (AiEvolveTopic t : topics) {
                try {
                    String query = sanitizeTopicForSearch(t.getTopic());
                    if (query.isBlank()) continue;
                    // 主题已是短检索词：跳过 LLM 查询改写，显著降低 gatherContext 内多次 hybridSearch 的总耗时
                    var results = knowledgeBaseService.hybridSearch(kbId, query, 6, effectiveUserId, null, false, true);
                    for (var r : results) {
                        String key = r.title() + "|" + r.content().substring(0, Math.min(50, r.content().length()));
                        if (!seen.contains(key) && sb.length() < contextMaxChars) {
                            if (r.docId() != null && evolveDocumentLockService != null && !docLocks.containsKey(r.docId())) {
                                String lockVal = evolveDocumentLockService.tryLock(r.docId());
                                if (lockVal == null) {
                                    log.debug("文档 {} 已被其他进化任务锁定，跳过", r.docId());
                                    continue;
                                }
                                docLocks.put(r.docId(), lockVal);
                            }
                            seen.add(key);
                            sb.append("【").append(r.title()).append("】\n").append(r.content()).append("\n\n");
                            if (r.docId() != null) citedDocIds.add(r.docId());
                        }
                    }
                } catch (Exception e) {
                    log.warn("主题 {} 检索失败: {}", t.getTopic(), e.getMessage());
                }
            }
            if (contentEffectivenessService != null && !citedDocIds.isEmpty()) {
                contentEffectivenessService.recordCitation(citedDocIds);
            }
        } catch (Exception e) {
            log.error("gatherContext 异常，释放所有文档锁", e);
        } finally {
            if (evolveDocumentLockService != null) {
                for (var entry : docLocks.entrySet()) {
                    evolveDocumentLockService.unlock(entry.getKey(), entry.getValue());
                }
            }
        }
        String ctx = sb.toString();
        if (ctx.length() > contextMaxChars) {
            ctx = ctx.substring(0, contextMaxChars);
        }
        return ctx;
    }

    private String sanitizeTopicForSearch(String topic) {
        if (topic == null || topic.isBlank()) return "";
        return topic.replaceAll("\\t.*$", "").trim();
    }

    // ---- angle selection ----

    /**
     * 有 hint 时沿用运营指定角度；否则按配置策略在 EVOLVE_ANGLES 上系统选角（E-2）。
     */
    String pickEvolveAngle(Long kbId, String evolveAngleHint) {
        if (evolveAngleHint != null && !evolveAngleHint.isBlank()) {
            return evolveAngleHint.trim();
        }
        String s = evolveAngleStrategy != null ? evolveAngleStrategy.trim().toLowerCase(Locale.ROOT) : "round_robin";
        if ("random".equals(s)) {
            return EVOLVE_ANGLES[ThreadLocalRandom.current().nextInt(EVOLVE_ANGLES.length)];
        }
        if ("least_used_recent".equals(s)) {
            return pickLeastUsedRecentAngle(kbId);
        }
        long cnt = kbId != null ? taskRepository.countByKbId(kbId) : 0L;
        return EVOLVE_ANGLES[(int) (Math.abs(cnt) % EVOLVE_ANGLES.length)];
    }

    private String pickLeastUsedRecentAngle(Long kbId) {
        Map<String, Integer> freq = new HashMap<>();
        for (String a : EVOLVE_ANGLES) {
            freq.put(a, 0);
        }
        if (kbId != null) {
            List<String> recent = taskRepository.findRecentEvolveAnglesByKbId(kbId, PageRequest.of(0, 80));
            for (String a : recent) {
                if (a == null || a.isBlank()) continue;
                String key = a.trim();
                if (freq.containsKey(key)) {
                    freq.merge(key, 1, Integer::sum);
                }
            }
        }
        String best = EVOLVE_ANGLES[0];
        int min = Integer.MAX_VALUE;
        for (String a : EVOLVE_ANGLES) {
            int n = freq.get(a);
            if (n < min) {
                min = n;
                best = a;
            }
        }
        return best;
    }

    // ---- topic expansion ----

    int expandTopics(AiEvolveReport report, AiEvolveTask task, Long kbId, AiKnowledgeBase kb, boolean isHuashu, List<AiModel> models) {
        if (models == null || models.isEmpty()) return 0;

        String reportSummary = report.getFullContent().length() > 1500
                ? report.getFullContent().substring(0, 1500) + "..." : report.getFullContent();
        String pendingStr = "";
        List<AiEvolvePendingDeepen> pendingList = kbId != null
                ? pendingDeepenRepository.findByKbIdAndStatusAndDeletedOrderByPriorityLevelAscCreateTimeAsc(kbId, "pending", 0, PageRequest.of(0, 5))
                : pendingDeepenRepository.findByStatusOrderByPriorityLevelAscCreateTimeAsc("pending", PageRequest.of(0, 5));
        if (!pendingList.isEmpty()) {
            pendingStr = "P0 待深化问题：\n" + pendingList.stream()
                    .map(AiEvolvePendingDeepen::getQuestionText)
                    .collect(Collectors.joining("\n"));
        }

        String kbType = kb != null && kb.getKbType() != null ? kb.getKbType().trim() : "general";
        boolean isZhishi = "zhishi".equalsIgnoreCase(kbType);
        String categoryList;
        String domainHint;
        String exampleLines;
        Set<String> allowedCategories;
        if (isHuashu) {
            categoryList = "huashu_emotional,huashu_humor,huashu_literary,huashu_persuasion,huashu_scenario";
            domainHint = "领域提示：直播话术相关（高扎心/搞笑/鸡汤/歇后语/家庭/爱情/抒情/人生语录/开场卖点互动促单等）";
            exampleLines = "高扎心促单话术 情感共鸣\\thuashu_emotional\n搞笑转场话术 段子\\thuashu_humor\n";
            allowedCategories = Set.of("huashu_emotional", "huashu_humor", "huashu_literary", "huashu_persuasion", "huashu_scenario");
        } else if (isZhishi) {
            categoryList = "zhishi_backend,zhishi_frontend,zhishi_data,zhishi_devops,zhishi_security";
            domainHint = "领域提示：技术知识相关（后端/前端/数据库/运维/安全/架构/最佳实践）";
            exampleLines = "Spring Boot 缓存 最佳实践\\tzhishi_backend\n前端 性能优化 工程化\\tzhishi_frontend\n";
            allowedCategories = Set.of("zhishi_backend", "zhishi_frontend", "zhishi_data", "zhishi_devops", "zhishi_security");
        } else {
            categoryList = "douyin_live,douyin_basic,douyin_vertical,douyin_shortvideo,douyin_algorithm,douyin_commercial,douyin_compliance,douyin_data,douyin_team,douyin_ai";
            domainHint = "领域提示：抖音运营相关（直播/短视频/电商/数据分析/AI工具/平台规则/垂类策略/商业化）";
            exampleLines = "抖音 直播 话术 优化\\tdouyin_live\n短视频 黄金3秒 留存率\\tdouyin_basic\n垂类 美妆 科普\\tdouyin_vertical\n";
            allowedCategories = Set.of("douyin_live", "douyin_basic", "douyin_vertical", "douyin_shortvideo", "douyin_algorithm", "douyin_commercial", "douyin_compliance", "douyin_data", "douyin_team", "douyin_ai");
        }
        String prompt = "最新进化报告：\n" + reportSummary + "\n\n" + pendingStr +
                "\n\n" + domainHint + "\n\n" +
                "请输出 3-5 个新搜索主题短语，每行格式：主题短语\\t分类\n分类从以下选1：" + categoryList + "\n示例：\n" + exampleLines + "...";
        String systemPrompt = isHuashu
                ? aiPromptConfigService.getPrompt("ai.prompt.evolve.huashu.system", "你是一个直播话术知识体系架构师，擅长高停留、高共鸣话术主题提炼。")
                : (isZhishi
                        ? aiPromptConfigService.getPrompt("ai.prompt.evolve.zhishi.system", "你是一个技术知识体系架构师，擅长提炼可执行的技术最佳实践。")
                        : aiPromptConfigService.getPrompt("ai.prompt.evolve.system", "你是一个抖音运营知识体系架构师。"));
        LlmClient.LlmResponse resp = llmClient.chatWithFallback(models, systemPrompt, prompt);
        if (!resp.success() || resp.content() == null) return 0;

        int added = 0;
        List<AiEvolveTopic> existingTopics = topicRepository.findByKbIdAndStatusAndDeletedOrderByPriorityAsc(kbId, 1, 0);
        List<AiEvolveTopic> globalTopics = topicRepository.findGlobalTopicsOrderByPriorityAscCreateTimeDesc();
        List<AiEvolveTopic> allExisting = new ArrayList<>(existingTopics);
        allExisting.addAll(globalTopics);
        long topicCount = allExisting.size();

        for (String line : resp.content().split("\n")) {
            String topic;
            String category = isZhishi ? "zhishi_backend" : (isHuashu ? "huashu_emotional" : "douyin_live");
            if (line.contains("\t")) {
                String[] parts = line.split("\t", 2);
                topic = parts[0].replaceAll("^[-*•\\d.]\\s*", "").trim();
                if (parts.length > 1) {
                    String c = parts[1].trim().toLowerCase().replaceAll("\\s+", "_");
                    if (allowedCategories.contains(c)) category = c;
                }
            } else {
                topic = line.replaceAll("^[-*•\\d.]\\s*", "").trim();
            }
            if (topic.length() >= 4 && topic.length() <= 50) {
                if (evolveTopicService.isTopicDuplicate(topic, allExisting, 0.75)) continue;
                if (topicCount + added >= topicPoolMax) {
                    String evictPattern = evolveTopicService.resolveCategoryPatternForKb(kb);
                    List<AiEvolveTopic> toEvict = evictPattern != null
                            ? topicRepository.findTailTopicsForEvictionByCategoryPrefix(kbId, evictPattern, PageRequest.of(0, 12))
                            : topicRepository.findTailTopicsForEviction(kbId, PageRequest.of(0, 12));
                    for (AiEvolveTopic e : toEvict) {
                        e.setDeleted(1);
                        topicRepository.save(e);
                        allExisting.removeIf(x -> x.getId().equals(e.getId()));
                        topicCount--;
                    }
                }
                if (topicCount + added >= topicPoolMax) break;
                AiEvolveTopic t = new AiEvolveTopic();
                t.setKbId(kbId);
                t.setTopic(topic);
                t.setCategory(category);
                t.setPriority(100);
                t.setSource("expanded");
                topicRepository.save(t);
                allExisting.add(t);
                added++;
                topicCount++;
            }
        }
        return added;
    }
}
