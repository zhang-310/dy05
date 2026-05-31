package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.service.LlmToolAugmentedChatHelper;
import cn.gaifan.douyinOperations.module.ai.service.OperationalStrategyKnowledgeService;
import cn.gaifan.douyinOperations.module.ai.tool.LlmToolContext;
import cn.gaifan.douyinOperations.module.live.config.LiveGenerationProperties;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptComplianceEnhancer;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptQualityService;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiResultVO;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.script.service.ComplianceWordService;
import cn.gaifan.douyinOperations.module.script.service.IndustryComplianceService;
import cn.gaifan.douyinOperations.module.script.service.ViolationWordService;
import cn.gaifan.douyinOperations.module.script.vo.ViolationCheckResultVO;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 直播话术质量服务实现（P2-1）
 */
@Service
public class LiveScriptQualityServiceImpl implements LiveScriptQualityService {

    private static final Logger log = LoggerFactory.getLogger(LiveScriptQualityServiceImpl.class);
    private static final Pattern PERFORMANCE_GUIDE_PATTERN = Pattern.compile("【([^】]{2,30})】");

    @Resource
    private ViolationWordService violationWordService;
    @Resource
    private LiveScriptRepository scriptRepository;
    @Resource
    private LiveAiModelHelper modelHelper;
    @Resource
    private LlmClient llmClient;
    @Resource
    private LlmToolAugmentedChatHelper llmToolAugmentedChatHelper;

    @Autowired(required = false)
    private ComplianceWordService complianceWordService;

    @Autowired(required = false)
    private IndustryComplianceService industryComplianceService;

    @Autowired(required = false)
    private LiveScriptComplianceEnhancer liveScriptComplianceEnhancer;

    @Autowired(required = false)
    private LiveGenerationProperties liveGenerationProperties;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Autowired(required = false)
    private OperationalStrategyKnowledgeService operationalStrategyKnowledgeService;

    /** 与 IndustryComplianceService / 商品话术合规 brain 对齐（sc_compliance_word + 行业正则 + 抖音公开规则摘要） */
    @Value("${app.live.compliance.industry-code:cosmetics}")
    private String liveComplianceIndustryCode;

    @Override
    public LiveAiResultVO.ViolationCheckResult checkViolation(String content, Long userId, String scope) {
        String text = content != null ? content : "";
        LinkedHashSet<String> lines = new LinkedHashSet<>();

        ViolationCheckResultVO checkResult = violationWordService.check(content, scope, userId);
        if (checkResult != null && checkResult.isHasViolation() && checkResult.getViolations() != null) {
            checkResult.getViolations().stream()
                    .map(v -> "[违规词库]" + v.getWord() + "(" + (v.getReason() != null ? v.getReason() : "") + ")")
                    .forEach(lines::add);
        }

        if (complianceWordService != null && complianceWordService.isLoadedFromDb() && !text.isBlank()) {
            for (String v : complianceWordService.getMedicalViolations()) {
                if (v != null && !v.isBlank() && text.contains(v)) {
                    lines.add("[合规词库·医疗]" + v);
                }
            }
            Map<String, String> abs = complianceWordService.getAbsoluteReplacements();
            if (abs != null) {
                for (Map.Entry<String, String> e : abs.entrySet()) {
                    if (e.getKey() != null && !e.getKey().isBlank() && text.contains(e.getKey())) {
                        String rep = e.getValue() != null ? e.getValue() : "合规表述";
                        lines.add("[合规词库·绝对化]" + e.getKey() + "→建议:" + rep);
                    }
                }
            }
        }

        if (industryComplianceService != null && !text.isBlank()) {
            String industry = (liveComplianceIndustryCode != null && !liveComplianceIndustryCode.isBlank())
                    ? liveComplianceIndustryCode.trim()
                    : "cosmetics";
            for (Map<String, Object> hit : industryComplianceService.checkCompliance(text, industry)) {
                String matched = String.valueOf(hit.getOrDefault("matchedText", ""));
                String level = String.valueOf(hit.getOrDefault("level", ""));
                String reason = String.valueOf(hit.getOrDefault("reason", ""));
                String reference = String.valueOf(hit.getOrDefault("reference", ""));
                String source = String.valueOf(hit.getOrDefault("source", "industry"));
                lines.add("[行业规则·" + source + "][" + level + "] " + matched + " — " + reason + " （" + reference + "）");
            }
        }

        List<String> violations = new ArrayList<>(lines);
        LiveAiResultVO.ViolationCheckResult result = new LiveAiResultVO.ViolationCheckResult();
        result.setViolations(violations);
        result.setViolationCount(violations.size());
        result.setPassed(violations.isEmpty());
        applyOfficialRuleGate(result, resolveOfficialViolationReferences(userId, text));
        return result;
    }

    @Override
    public LiveAiResultVO.ViolationCheckResult checkViolationEnhanced(String content, DyProduct product, Long userId) {
        if (content == null || content.isBlank()) {
            LiveAiResultVO.ViolationCheckResult empty = new LiveAiResultVO.ViolationCheckResult();
            empty.setViolations(List.of());
            empty.setViolationCount(0);
            empty.setPassed(true);
            applyOfficialRuleGate(empty, resolveOfficialViolationReferences(resolveViolationUserId(userId, product), ""));
            return empty;
        }
        Long effectiveUserId = resolveViolationUserId(userId, product);

        LinkedHashSet<String> lines = new LinkedHashSet<>();

        // 第一层：基础违禁词检测（复用现有逻辑）
        ViolationCheckResultVO checkResult = violationWordService.check(content, "live", effectiveUserId);
        if (checkResult != null && checkResult.isHasViolation() && checkResult.getViolations() != null) {
            checkResult.getViolations().stream()
                    .map(v -> "[违规词库]" + v.getWord() + "(" + (v.getReason() != null ? v.getReason() : "") + ")")
                    .forEach(lines::add);
        }

        if (liveScriptComplianceEnhancer == null) {
            List<String> violations = new ArrayList<>(lines);
            LiveAiResultVO.ViolationCheckResult result = new LiveAiResultVO.ViolationCheckResult();
            result.setViolations(violations);
            result.setViolationCount(violations.size());
            result.setPassed(violations.isEmpty());
            applyOfficialRuleGate(result, resolveOfficialViolationReferences(effectiveUserId, content));
            return result;
        }

        // 第二层：品类禁售/限售校验
        try {
            LiveScriptComplianceEnhancer.CategoryCheckResult catResult =
                    liveScriptComplianceEnhancer.checkCategoryRestriction(content);
            if (catResult.forbidden()) {
                catResult.forbiddenKeywords().forEach(kw ->
                        lines.add("[品类禁售][高危] 话术包含禁售品类关键词「" + kw + "」，此类商品严禁在抖音直播间销售"));
            }
            if (catResult.restricted()) {
                catResult.restrictedCategories().forEach(cat ->
                        lines.add("[品类限售][中危] 检测到限售类目风险：" + cat + "，需提前申请平台资质并上传许可证"));
            }
        } catch (Exception e) {
            log.debug("品类合规检测异常，跳过: {}", e.getMessage());
        }

        // 第三层：价格一致性检测
        try {
            java.math.BigDecimal systemPrice = (product != null) ? product.getPrice() : null;
            String productName = (product != null) ? product.getProductName() : null;
            LiveScriptComplianceEnhancer.PriceCheckResult priceResult =
                    liveScriptComplianceEnhancer.checkPriceConsistency(content, systemPrice, productName);
            if (!priceResult.passed() && priceResult.hint() != null) {
                lines.add("[价格合规][中危] " + priceResult.hint());
            }
        } catch (Exception e) {
            log.debug("价格一致性检测异常，跳过: {}", e.getMessage());
        }

        // 第四层：售后承诺高风险检测
        try {
            LiveScriptComplianceEnhancer.AftersaleCheckResult aftersaleResult =
                    liveScriptComplianceEnhancer.checkAftersaleCommitment(content);
            if (aftersaleResult.hasRiskyCommitment() && aftersaleResult.hint() != null) {
                aftersaleResult.commitmentMatches().forEach(kw ->
                        lines.add("[售后承诺][中危] 包含「" + kw + "」：" + aftersaleResult.hint()));
            }
        } catch (Exception e) {
            log.debug("售后承诺检测异常，跳过: {}", e.getMessage());
        }

        List<String> violations = new ArrayList<>(lines);
        LiveAiResultVO.ViolationCheckResult result = new LiveAiResultVO.ViolationCheckResult();
        result.setViolations(violations);
        result.setViolationCount(violations.size());
        result.setPassed(violations.isEmpty());
        applyOfficialRuleGate(result, resolveOfficialViolationReferences(effectiveUserId, content));
        return result;
    }

    private Long resolveViolationUserId(Long userId, DyProduct product) {
        if (userId != null && userId > 0) {
            return userId;
        }
        return product != null && product.getUserId() != null && product.getUserId() > 0 ? product.getUserId() : null;
    }

    private List<LiveAiResultVO.OfficialReferenceVO> resolveOfficialViolationReferences(Long userId, String content) {
        if (operationalStrategyKnowledgeService == null || userId == null || userId <= 0) {
            return List.of();
        }
        try {
            String query = String.join(" ",
                    content != null ? content : "",
                    "直播违规 直播话术违规 绝对化用语 虚假宣传 商品规则 douyin_weigui 官方规则");
            OperationalStrategyKnowledgeService.PromptContext context =
                    operationalStrategyKnowledgeService.buildLiveGenerationContext(userId, query, "violation_check", 1600);
            OperationalStrategyKnowledgeService.PromptContext ruleContext =
                    operationalStrategyKnowledgeService.buildViolationRuleContext(userId, query, "live_violation_check", 1600);
            if (context == null || context.officialReferences() == null) {
                context = new OperationalStrategyKnowledgeService.PromptContext("", List.of(), List.of());
            }
            List<OperationalStrategyKnowledgeService.OfficialReference> merged = new ArrayList<>(context.officialReferences());
            if (ruleContext != null && ruleContext.officialReferences() != null) {
                merged.addAll(ruleContext.officialReferences());
            }
            return merged.stream()
                    .filter(ref -> "douyin_weigui".equals(ref.kbName()) || "violation_rule".equals(ref.refType()))
                    .collect(java.util.stream.Collectors.toMap(
                            ref -> String.valueOf(ref.docId()) + ":" + String.valueOf(ref.chunkId()),
                            ref -> ref,
                            (a, b) -> a,
                            LinkedHashMap::new))
                    .values().stream()
                    .map(ref -> {
                        LiveAiResultVO.OfficialReferenceVO vo = new LiveAiResultVO.OfficialReferenceVO();
                        vo.setKbName(ref.kbName());
                        vo.setRefType(ref.refType());
                        vo.setDocId(ref.docId());
                        vo.setChunkId(ref.chunkId());
                        vo.setTitle(ref.title());
                        vo.setContentPreview(ref.contentPreview());
                        vo.setScore(ref.score());
                        return vo;
                    })
                    .toList();
        } catch (Exception e) {
            log.debug("直播违规官方规则引用检索跳过: {}", e.getMessage());
            return List.of();
        }
    }

    private void applyOfficialRuleGate(
            LiveAiResultVO.ViolationCheckResult result,
            List<LiveAiResultVO.OfficialReferenceVO> officialReferences) {
        List<LiveAiResultVO.OfficialReferenceVO> refs = officialReferences != null ? officialReferences : List.of();
        boolean satisfied = refs.stream().anyMatch(ref ->
                "violation_rule".equals(ref.getRefType()) || "douyin_weigui".equals(ref.getKbName()));
        result.setOfficialReferences(refs);
        result.setOfficialReferenceRequired(true);
        result.setOfficialReferenceSatisfied(satisfied);
        result.setOfficialReferenceStatus(satisfied ? "satisfied" : "missing_douyin_weigui_reference");
        if (!satisfied) {
            List<String> violations = new ArrayList<>(result.getViolations() != null ? result.getViolations() : List.of());
            violations.add("[官方规则引用缺失][高危] 未检索到 douyin_weigui 官方违规规则引用，禁止判定为审核通过");
            result.setViolations(violations);
            result.setViolationCount(violations.size());
            result.setPassed(false);
        }
    }

    @Override
    public List<String> extractPerformanceGuides(String content) {
        if (content == null || content.isBlank()) return List.of();
        Matcher m = PERFORMANCE_GUIDE_PATTERN.matcher(content);
        List<String> guides = new ArrayList<>();
        while (m.find()) {
            String guide = m.group(1).trim();
            if (!guides.contains(guide)) guides.add(guide);
        }
        return guides;
    }

    @Override
    public void analyzeAndRefineFullScript(Long sessionId, Long userId, Long modelId) {
        List<LiveScript> scripts = scriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(sessionId, 0);
        if (scripts.size() < 2) return;
        List<AiModel> models = modelHelper.resolveModels(modelId);
        if (models == null || models.isEmpty()) return;

        StringBuilder full = new StringBuilder();
        for (int i = 0; i < scripts.size(); i++) {
            full.append("【第").append(i + 1).append("段】\n");
            full.append(scripts.get(i).getScriptContent() != null ? scripts.get(i).getScriptContent() : "").append("\n\n");
        }
        String systemPrompt = """
            你是直播话术成篇优化专家。通读整场话术，优化衔接与语气统一。
            原则：① 保持每段核心意图和卖点不变 ② 只优化段与段之间的衔接、语气统一 ③ 避免过度改写
            输出格式：每段以【第N段】开头，后跟该段话术，段与段之间空一行。段落数量与顺序必须与输入一致。
            """;
        String userPrompt = "请优化以下整场话术的衔接与语气，保持各段核心内容不变：\n\n" + full;

        try {
            LlmClient.LlmResponse resp = llmToolAugmentedChatHelper.chatWithFallbackAndOptionalTools(
                    models, systemPrompt, userPrompt, LlmToolContext.forUser(userId),
                    llmToolAugmentedChatHelper.isLiveScriptQualityToolsEnabled());
            if (!resp.success() || resp.content() == null || resp.content().isBlank()) return;
            List<String> refined = parseRefinedSegments(resp.content().trim(), scripts.size());
            for (int i = 0; i < scripts.size() && i < refined.size(); i++) {
                String text = refined.get(i).trim();
                if (!text.isBlank()) {
                    LiveScript s = scripts.get(i);
                    String old = s.getScriptContent();
                    s.setScriptContent(text);
                    if (old == null || !old.equals(text)) {
                        s.setGenerationPromptHash(null);
                    }
                    applyGenerationQualityHints(s);
                    scriptRepository.save(s);
                }
            }
        } catch (Exception e) {
            log.warn("分析成篇失败 sessionId={}: {}", sessionId, e.getMessage());
        }
    }

    @Override
    public Map<String, Object> checkDurationFit(String content, Integer slotDurationSeconds) {
        Map<String, Object> out = new LinkedHashMap<>();
        String text = content != null ? content : "";
        int chars = text.length();
        double estSec = chars / 4.5;
        out.put("charCount", chars);
        out.put("estimatedSpeechSeconds", Math.round(estSec * 10) / 10.0);
        if (slotDurationSeconds == null || slotDurationSeconds <= 0) {
            out.put("checked", false);
            out.put("ok", true);
            return out;
        }
        out.put("checked", true);
        out.put("slotSeconds", slotDurationSeconds);
        boolean ok = estSec <= slotDurationSeconds * 1.12;
        out.put("ok", ok);
        if (!ok) {
            out.put("hint", String.format("估算口播约 %.1f 秒，超过槽位 %d 秒的 12%% 容差，建议缩句或拆分", estSec, slotDurationSeconds));
        }
        return out;
    }

    @Override
    public Map<String, Object> mergeDurationFitHint(LiveScript script) {
        if (script == null) {
            return checkDurationFit(null, null);
        }
        Map<String, Object> fit = checkDurationFit(script.getScriptContent(), script.getDurationLimitSec());
        boolean checked = Boolean.TRUE.equals(fit.get("checked"));
        boolean ok = Boolean.TRUE.equals(fit.get("ok"));
        if (checked && !ok) {
            String hint = Objects.toString(fit.get("hint"), "");
            if (!hint.isBlank()) {
                String addition = "[时长] " + hint;
                String existing = script.getAiSuggestion();
                if (existing == null || existing.isBlank()) {
                    script.setAiSuggestion(addition);
                } else if (!existing.contains(hint)) {
                    script.setAiSuggestion(existing + "\n" + addition);
                }
            }
            log.warn("live-script duration fit not ok scriptId={} slotSec={} estSec={}",
                    script.getId(),
                    fit.get("slotSeconds"),
                    fit.get("estimatedSpeechSeconds"));
        }
        return fit;
    }

    @Override
    public Map<String, Object> checkCorpusDuplicate(Long userId, Long excludeScriptId, String content) {
        Map<String, Object> out = new LinkedHashMap<>();
        LiveGenerationProperties.CorpusDuplicateCheck cfg = effectiveDuplicateConfig();
        if (!cfg.isEnabled() || userId == null || content == null) {
            out.put("checked", false);
            out.put("suspected", false);
            return out;
        }
        String norm = normalizeForDuplicate(content);
        if (norm.length() < cfg.getMinChars()) {
            out.put("checked", false);
            out.put("suspected", false);
            out.put("reason", "below_min_chars");
            return out;
        }
        int max = Math.max(1, cfg.getMaxCorpusScripts());
        List<LiveScript> corpus = scriptRepository.findRecentScriptsForDuplicateCheck(
                userId, excludeScriptId, PageRequest.of(0, max));
        double threshold = cfg.getSimilarityThreshold();
        double best = 0;
        Long matchId = null;
        for (LiveScript o : corpus) {
            if (o == null || o.getScriptContent() == null) {
                continue;
            }
            String other = normalizeForDuplicate(o.getScriptContent());
            if (other.length() < cfg.getMinChars()) {
                continue;
            }
            double sim = bigramJaccard(norm, other);
            if (sim > best) {
                best = sim;
                matchId = o.getId();
            }
        }
        boolean suspected = best >= threshold;
        out.put("checked", true);
        out.put("suspected", suspected);
        out.put("maxSimilarity", Math.round(best * 1000) / 1000.0);
        out.put("threshold", threshold);
        if (matchId != null) {
            out.put("matchScriptId", matchId);
        }
        if (suspected) {
            out.put("hint", String.format("与历史话术（scriptId=%s）估算重复度约 %.0f%%，建议改写措辞或结构", matchId, best * 100));
        }
        return out;
    }

    @Override
    public Map<String, Object> mergeCorpusDuplicateHint(LiveScript script) {
        if (script == null) {
            return checkCorpusDuplicate(null, null, null);
        }
        Map<String, Object> m = checkCorpusDuplicate(script.getUserId(), script.getId(), script.getScriptContent());
        if (!Boolean.TRUE.equals(m.get("checked")) || !Boolean.TRUE.equals(m.get("suspected"))) {
            return m;
        }
        String hint = Objects.toString(m.get("hint"), "");
        if (!hint.isBlank()) {
            String addition = "[查重] " + hint;
            String existing = script.getAiSuggestion();
            if (existing == null || existing.isBlank()) {
                script.setAiSuggestion(addition);
            } else if (!existing.contains(hint)) {
                script.setAiSuggestion(existing + "\n" + addition);
            }
        }
        log.warn("live-script corpus duplicate suspected userId={} scriptId={} maxSimilarity={}",
                script.getUserId(), script.getId(), m.get("maxSimilarity"));
        return m;
    }

    @Override
    public Map<String, Object> applyGenerationQualityHints(LiveScript script) {
        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("durationFit", mergeDurationFitHint(script));
        bundle.put("duplicateCheck", mergeCorpusDuplicateHint(script));
        bundle.put("sentimentLexicon", mergeSentimentLexiconHints(script));
        bundle.put("performanceCueHeuristics", mergePerformanceCueHeuristicHints(script));
        bundle.put("performanceCueLlm", mergePerformanceCueLlmHints(script));
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("tier1", "rules_and_heuristics");
        plan.put("tier2", "sentiment_lexicon");
        plan.put("tier3", "optional_llm_performance_cue");
        plan.put("order", java.util.List.of(
                "durationFit", "duplicateCheck", "sentimentLexicon", "performanceCueHeuristics", "performanceCueLlm"));
        bundle.put("performanceCueStrategyPlan", plan);
        return bundle;
    }

    private static final String[] SENTIMENT_POS = {
            "好", "棒", "喜欢", "满意", "开心", "感谢", "划算", "推荐", "超值", "温柔"
    };
    private static final String[] SENTIMENT_NEG = {
            "差", "坑", "骗", "失望", "贵", "不要", "后悔", "假", "差评", "冷静"
    };

    /**
     * P2 Q-3：极轻量中文情感词频（非深度学习），写入 ai_suggestion 供运营扫一眼。
     */
    private Map<String, Object> mergeSentimentLexiconHints(LiveScript script) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("checked", false);
        if (script == null) {
            return out;
        }
        if (liveGenerationProperties == null || liveGenerationProperties.getSentimentLexicon() == null
                || !liveGenerationProperties.getSentimentLexicon().isEnabled()) {
            return out;
        }
        String content = script.getScriptContent() != null ? script.getScriptContent() : "";
        SentimentStat st = computeSentimentLexiconStat(content);
        if (st.sum() == 0) {
            return out;
        }
        out.put("checked", true);
        out.put("positiveHits", st.pos());
        out.put("negativeHits", st.neg());
        appendAiSuggestionUnique(script, "[情感] 词典粗判：" + st.label() + "（正/负命中 " + st.pos() + "/" + st.neg() + "，非模型）");
        return out;
    }

    private record SentimentStat(int pos, int neg, String label) {
        int sum() {
            return pos + neg;
        }
    }

    /** Q-3 与 Q-5 LLM user 块共用：词频与粗标签（正文 &lt;12 字视为无信号） */
    private SentimentStat computeSentimentLexiconStat(String content) {
        if (content == null || content.length() < 12) {
            return new SentimentStat(0, 0, null);
        }
        int pos = 0;
        int neg = 0;
        for (String w : SENTIMENT_POS) {
            int i = 0;
            while ((i = content.indexOf(w, i)) >= 0) {
                pos++;
                i += w.length();
            }
        }
        for (String w : SENTIMENT_NEG) {
            int i = 0;
            while ((i = content.indexOf(w, i)) >= 0) {
                neg++;
                i += w.length();
            }
        }
        int sum = pos + neg;
        if (sum == 0) {
            return new SentimentStat(0, 0, null);
        }
        String label;
        if (neg == 0) {
            label = "偏正面";
        } else if (pos == 0) {
            label = "偏负面";
        } else {
            double r = (double) pos / (double) sum;
            label = r >= 0.65 ? "偏正面" : r <= 0.35 ? "偏负面" : "正负面词并存";
        }
        return new SentimentStat(pos, neg, label);
    }

    private void recordPerfCueLlm(String outcome) {
        if (meterRegistry == null) {
            return;
        }
        try {
            meterRegistry.counter("live.performance_cue_llm", "outcome", outcome).increment();
        } catch (Exception ignored) {
        }
    }

    /**
     * P2 Q-5 可选：LLM 补充表演建议（默认关闭，见 app.live.generation.performance-cue-llm）。
     * 支持 Redis 缓存、日配额、最小间隔；user 提示附带轻量情感/句法特征。
     */
    private Map<String, Object> mergePerformanceCueLlmHints(LiveScript script) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("used", false);
        if (script == null || liveGenerationProperties == null) {
            return out;
        }
        LiveGenerationProperties.PerformanceCueLlm cfg = liveGenerationProperties.getPerformanceCueLlm();
        if (cfg == null || !cfg.isEnabled()) {
            return out;
        }
        String content = script.getScriptContent() != null ? script.getScriptContent() : "";
        if (content.isBlank() || content.length() > cfg.getMaxScriptChars()) {
            return out;
        }
        Long ownerId = script.getUserId();
        String hash = sha256Hex(String.valueOf(ownerId) + "\n" + cfg.getPromptVersion() + "\n" + content);
        String cacheKey = "live:perf-cue-llm:cache:v1:" + hash;
        try {
            if (cfg.isCacheEnabled() && cfg.getCacheTtlSeconds() > 0 && stringRedisTemplate != null) {
                String hit = stringRedisTemplate.opsForValue().get(cacheKey);
                if ("__none__".equals(hit)) {
                    recordPerfCueLlm("skipped_cache_empty");
                    return out;
                }
                if (StringUtils.hasText(hit)) {
                    int added = applyPerformanceCueLlmResponse(script, hit, cfg);
                    out.put("used", added > 0);
                    out.put("cacheHit", true);
                    recordPerfCueLlm(added > 0 ? "cache_hit_used" : "cache_hit_unused");
                    return out;
                }
            }

            if (cfg.getMinIntervalMs() > 0 && stringRedisTemplate != null && ownerId != null) {
                String rlKey = "live:perf-cue-llm:rl:" + ownerId;
                Boolean ok = stringRedisTemplate.opsForValue()
                        .setIfAbsent(rlKey, "1", Duration.ofMillis(cfg.getMinIntervalMs()));
                if (!Boolean.TRUE.equals(ok)) {
                    out.put("skipped", "rate_limit");
                    recordPerfCueLlm("rate_limit");
                    return out;
                }
            }
            if (cfg.getMaxInvocationsPerDayPerOwner() > 0 && stringRedisTemplate != null && ownerId != null) {
                String day = LocalDate.now().toString();
                String qKey = "live:perf-cue-llm:quota:" + ownerId + ":" + day;
                Long c = stringRedisTemplate.opsForValue().increment(qKey);
                if (c != null && c == 1L) {
                    stringRedisTemplate.expire(qKey, Duration.ofDays(2));
                }
                if (c != null && c > cfg.getMaxInvocationsPerDayPerOwner()) {
                    stringRedisTemplate.opsForValue().decrement(qKey);
                    out.put("skipped", "quota_exceeded");
                    recordPerfCueLlm("quota_exceeded");
                    return out;
                }
            }

            AiModel model = modelHelper.findAvailableModel(null);
            if (model == null) {
                rollbackPerfCueQuota(ownerId, cfg);
                recordPerfCueLlm("no_model");
                return out;
            }
            String sys = cfg.isStructuredJsonEnabled()
                    ? "你是直播话术导演助理。请只输出一行 JSON 数组（不要 markdown、不要解释），元素为不超过24字的表演或节奏提示，最多3条。若无必要输出 []。"
                    : "你是直播话术导演助理。请根据正文与特征摘要给出不超过3条表演或节奏提示，每条单独一行，每行不超过24字。"
                    + "不要编号。若无必要只输出一个字：无";
            String userBlock = buildPerformanceCueLlmUserBlock(content);
            LlmClient.LlmResponse resp = llmClient.chat(model, sys, userBlock);
            String txt = resp != null && resp.content() != null ? resp.content().trim() : "";
            int added = applyPerformanceCueLlmResponse(script, txt, cfg);
            out.put("used", added > 0);
            recordPerfCueLlm(added > 0 ? "used" : "unused");

            if (cfg.isCacheEnabled() && cfg.getCacheTtlSeconds() > 0 && stringRedisTemplate != null) {
                if (added == 0 || txt.isBlank() || "无".equals(txt)
                        || ("[]".equals(txt) && cfg.isStructuredJsonEnabled())) {
                    stringRedisTemplate.opsForValue()
                            .set(cacheKey, "__none__", Duration.ofSeconds(cfg.getCacheTtlSeconds()));
                } else {
                    stringRedisTemplate.opsForValue()
                            .set(cacheKey, txt, Duration.ofSeconds(cfg.getCacheTtlSeconds()));
                }
            }
        } catch (Exception e) {
            log.debug("performance cue llm skipped: {}", e.getMessage());
            rollbackPerfCueQuota(script.getUserId(), liveGenerationProperties.getPerformanceCueLlm());
            recordPerfCueLlm("error");
        }
        return out;
    }

    private void rollbackPerfCueQuota(Long ownerId, LiveGenerationProperties.PerformanceCueLlm cfg) {
        if (ownerId == null || cfg == null || stringRedisTemplate == null) {
            return;
        }
        if (cfg.getMaxInvocationsPerDayPerOwner() <= 0) {
            return;
        }
        try {
            String day = LocalDate.now().toString();
            stringRedisTemplate.opsForValue().decrement("live:perf-cue-llm:quota:" + ownerId + ":" + day);
        } catch (Exception ignored) {
        }
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }

    /** 与模型约定的正文包装：附 Q-3 同源情感摘要、问句密度等（非重型 NLP） */
    private String buildPerformanceCueLlmUserBlock(String content) {
        SentimentStat st = computeSentimentLexiconStat(content);
        long qm = content.chars().filter(ch -> ch == '？' || ch == '?').count();
        int len = content.length();
        StringBuilder feat = new StringBuilder();
        feat.append(String.format(Locale.CHINA, "[特征] 字数=%d 问句标点数=%d 情感词粗略正/负=%d/%d%n",
                len, qm, st.pos(), st.neg()));
        if (st.sum() > 0) {
            feat.append(String.format(Locale.CHINA, "[Q-3同源] 词典情感：%s（正/负命中 %d/%d，非模型）%n",
                    st.label(), st.pos(), st.neg()));
        }
        feat.append("正文如下：\n").append(content);
        return feat.toString();
    }

    private int applyPerformanceCueLlmResponse(LiveScript script, String txt, LiveGenerationProperties.PerformanceCueLlm cfg) {
        if (txt == null || txt.isBlank()) {
            return 0;
        }
        if (!cfg.isStructuredJsonEnabled()) {
            return applyPerformanceCueLlmLinesFromModelText(script, txt);
        }
        String trimmed = txt.trim();
        if ("无".equals(trimmed) || "[]".equals(trimmed)) {
            return 0;
        }
        try {
            JSONArray arr = JSON.parseArray(trimmed);
            if (arr != null) {
                if (arr.isEmpty()) {
                    return 0;
                }
                int added = applyPerformanceCueLlmJsonArray(script, arr);
                if (added > 0) {
                    return added;
                }
            }
        } catch (Exception e) {
            recordPerfCueLlm("parse_error");
        }
        return applyPerformanceCueLlmLinesFromModelText(script, txt);
    }

    private int applyPerformanceCueLlmJsonArray(LiveScript script, JSONArray arr) {
        int added = 0;
        for (int i = 0; i < arr.size(); i++) {
            Object o = arr.get(i);
            if (o == null) {
                continue;
            }
            String t = String.valueOf(o).trim();
            if (t.length() >= 2 && t.length() <= 80 && !"无".equals(t)) {
                appendAiSuggestionUnique(script, "[表演·LLM] " + t);
                added++;
                if (added >= 4) {
                    break;
                }
            }
        }
        return added;
    }

    private int applyPerformanceCueLlmLinesFromModelText(LiveScript script, String txt) {
        if (txt == null || txt.isBlank() || "无".equals(txt.trim())) {
            return 0;
        }
        int added = 0;
        for (String line : txt.split("\n")) {
            String t = line.trim();
            if (t.length() >= 2 && t.length() <= 80 && !"无".equals(t)) {
                appendAiSuggestionUnique(script, "[表演·LLM] " + t);
                added++;
                if (added >= 4) {
                    break;
                }
            }
        }
        return added;
    }

    private LiveGenerationProperties.PerformanceCueHeuristics effectivePerformanceCueConfig() {
        if (liveGenerationProperties != null && liveGenerationProperties.getPerformanceCueHeuristics() != null) {
            return liveGenerationProperties.getPerformanceCueHeuristics();
        }
        return new LiveGenerationProperties.PerformanceCueHeuristics();
    }

    /**
     * Q-5 增量：长段无合法【】表演标注、或【】括号不成对时合并 ai_suggestion（与正则提取并存，非 LLM）。
     */
    private Map<String, Object> mergePerformanceCueHeuristicHints(LiveScript script) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("checked", false);
        if (script == null) {
            return out;
        }
        LiveGenerationProperties.PerformanceCueHeuristics cfg = effectivePerformanceCueConfig();
        if (!cfg.isEnabled()) {
            return out;
        }
        String content = script.getScriptContent() != null ? script.getScriptContent() : "";
        if (content.isBlank()) {
            return out;
        }
        long open = content.chars().filter(ch -> ch == '【').count();
        long close = content.chars().filter(ch -> ch == '】').count();
        if (open != close) {
            appendAiSuggestionUnique(script, "[表演] 【】括号数量不一致，请检查表演标注是否成对闭合。");
            out.put("checked", true);
            out.put("bracketMismatch", true);
        }
        int minLen = cfg.getMinCharsForMissingCueHint();
        if (minLen > 0 && content.length() >= minLen) {
            List<String> guides = extractPerformanceGuides(content);
            if (guides.isEmpty()) {
                appendAiSuggestionUnique(script,
                        "[表演] 话术较长但未见【】表演/语气标注（建议 2～30 字，如【微笑停顿】【压低声音】），便于提词高亮与节奏。");
                out.put("checked", true);
                out.put("missingPerformanceCueHint", true);
            }
        }
        return out;
    }

    private static void appendAiSuggestionUnique(LiveScript script, String line) {
        if (script == null || line == null || line.isBlank()) {
            return;
        }
        String existing = script.getAiSuggestion();
        if (existing == null || existing.isBlank()) {
            script.setAiSuggestion(line);
        } else if (!existing.contains(line)) {
            script.setAiSuggestion(existing + "\n" + line);
        }
    }

    private LiveGenerationProperties.CorpusDuplicateCheck effectiveDuplicateConfig() {
        if (liveGenerationProperties != null && liveGenerationProperties.getCorpusDuplicateCheck() != null) {
            return liveGenerationProperties.getCorpusDuplicateCheck();
        }
        return new LiveGenerationProperties.CorpusDuplicateCheck();
    }

    private static String normalizeForDuplicate(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.toLowerCase(Locale.ROOT);
        s = s.replaceAll("【[^】]{0,120}】", "");
        s = s.replaceAll("\\s+", "");
        return s;
    }

    private static double bigramJaccard(String a, String b) {
        Set<String> A = bigrams(a);
        Set<String> B = bigrams(b);
        if (A.isEmpty() && B.isEmpty()) {
            return 1.0;
        }
        if (A.isEmpty() || B.isEmpty()) {
            return 0;
        }
        int inter = 0;
        for (String x : A) {
            if (B.contains(x)) {
                inter++;
            }
        }
        int union = A.size() + B.size() - inter;
        return union <= 0 ? 0 : (double) inter / union;
    }

    private static Set<String> bigrams(String s) {
        Set<String> set = new HashSet<>();
        if (s == null || s.length() < 2) {
            return set;
        }
        for (int i = 0; i < s.length() - 1; i++) {
            set.add(s.substring(i, i + 2));
        }
        return set;
    }

    private List<String> parseRefinedSegments(String content, int expectedCount) {
        List<String> segments = new ArrayList<>();
        String[] parts = content.split("【第\\d+段】");
        for (String p : parts) {
            String s = p.trim();
            if (!s.isBlank()) segments.add(s);
        }
        if (segments.size() >= expectedCount) {
            return segments.subList(0, expectedCount);
        }
        return segments;
    }
}
