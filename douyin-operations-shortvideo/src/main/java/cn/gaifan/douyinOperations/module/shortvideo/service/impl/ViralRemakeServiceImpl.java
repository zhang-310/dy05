package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvScript;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvScriptRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvViralVideoRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.PersonaViralFusionService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvShootingTaskService;
import cn.gaifan.douyinOperations.module.shortvideo.service.ViralRemakeService;
import cn.gaifan.douyinOperations.module.script.service.ViolationWordService;
import cn.gaifan.douyinOperations.module.script.vo.ViolationCheckResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.service.ViralVideoService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShootingTaskSaveVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ViralRemakeServiceImpl implements ViralRemakeService {

    private static final Logger log = LoggerFactory.getLogger(ViralRemakeServiceImpl.class);

    private static final String DEFAULT_SUGGESTIONS = """
            [{"remakeType":"form_imitation","angle":"保持原结构，替换产品","brief":"模仿视频结构和节奏","matchScore":0.8},
             {"remakeType":"content_flip","angle":"反转原视频观点","brief":"从相反角度解读同一话题","matchScore":0.6},
             {"remakeType":"element_recombination","angle":"提取爆点重新组合","brief":"保留核心爆点，替换场景和产品","matchScore":0.7}]
            """;

    @Resource
    private SvViralVideoRepository viralVideoRepository;

    @Resource
    private SvScriptRepository scriptRepository;

    @Resource
    private ViralVideoService viralVideoService;

    @Resource
    private AiModelRepository aiModelRepository;

    @Resource
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private LlmClient llmClient;

    @Autowired(required = false)
    private SvShootingTaskService shootingTaskService;

    @Autowired(required = false)
    private PersonaViralFusionService personaViralFusionService;

    @Autowired(required = false)
    private ViolationWordService violationWordService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recommendRemake(Long viralVideoId, Long userId) {
        SvViralVideo viral = viralVideoService.getViralVideo(viralVideoId, userId);
        int st = viral.getRemakeStatus() != null ? viral.getRemakeStatus() : 0;
        if (st > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "该视频已有二创推荐或已进入后续流程");
        }
        viral.setRemakeSuggestions(generateRemakeSuggestions(viral));
        viral.setMatchedPersonaIds("[]");
        viral.setIndustryTags(inferIndustryTagsJson(viral));
        viral.setRemakeStatus(1);
        viralVideoRepository.save(viral);
        log.info("[二创推荐] viralId={}, userId={}", viralVideoId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchRecommend(Long userId, double scoreThreshold, int limit) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        int lim = Math.min(Math.max(limit, 1), 100);
        int minScore = (int) Math.round(scoreThreshold);
        Specification<SvViralVideo> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("ownerId"), userId));
            preds.add(cb.equal(root.get("deleted"), 0));
            preds.add(cb.or(cb.isNull(root.get("remakeStatus")), cb.equal(root.get("remakeStatus"), 0)));
            preds.add(cb.greaterThanOrEqualTo(root.get("viralScore"), minScore));
            return cb.and(preds.toArray(new Predicate[0]));
        };
        List<SvViralVideo> candidates = viralVideoRepository.findAll(spec,
                PageRequest.of(0, lim, Sort.by(Sort.Direction.DESC, "viralScore"))).getContent();
        int count = 0;
        for (SvViralVideo viral : candidates) {
            try {
                recommendRemake(viral.getId(), userId);
                count++;
            } catch (Exception e) {
                log.warn("[二创批量推荐] 跳过 viralId={}: {}", viral.getId(), e.getMessage());
            }
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmRemake(Long viralVideoId, String selectedRemakeType, Long personaId, Long userId) {
        SvViralVideo viral = viralVideoService.getViralVideo(viralVideoId, userId);
        if (viral.getRemakeStatus() == null || viral.getRemakeStatus() != 1) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "当前状态不可确认，需先完成 AI 推荐（状态应为已推荐）");
        }
        viral.setRemakeStatus(2);
        viral.setConfirmedBy(userId);
        viral.setConfirmedAt(Timestamp.valueOf(LocalDateTime.now()));
        if (personaId != null) {
            viral.setMatchedPersonaIds("[" + personaId + "]");
        }
        if (StringUtils.hasText(selectedRemakeType)) {
            viral.setConfirmedRemakeType(selectedRemakeType.trim());
        }
        viralVideoRepository.save(viral);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateRemakeScript(Long viralVideoId, Long userId, String scriptMode) {
        SvViralVideo viral = viralVideoService.getViralVideo(viralVideoId, userId);
        if (viral.getRemakeStatus() == null || viral.getRemakeStatus() != 2) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "需先运营确认后才能生成脚本");
        }
        String mode = StringUtils.hasText(scriptMode) ? scriptMode.trim() : "sop";
        if ("persona_fusion".equalsIgnoreCase(mode)) {
            return generateRemakeScriptPersonaFusion(viral, userId);
        }
        return generateRemakeScriptSop(viral, userId);
    }

    private Long generateRemakeScriptSop(SvViralVideo viral, Long userId) {
        String content = viralVideoService.replicateViral(viral.getId(), userId);
        SvScript script = new SvScript();
        script.setOwnerId(userId);
        String title = viral.getTitle() != null ? viral.getTitle() : "爆款二创脚本";
        if (title.length() > 80) {
            title = title.substring(0, 80);
        }
        script.setTitle("二创: " + title);
        script.setContent(content != null ? content : "");
        script.setScriptType("viral_clone");
        script.setGenerationType("ai");
        script.setReferenceViralId(viral.getId());
        attachViolationTags(script, content, userId);
        SvScript saved = scriptRepository.save(script);

        viral.setRemakeStatus(3);
        viral.setRemakeScriptId(saved.getId());
        viralVideoRepository.save(viral);
        return saved.getId();
    }

    private Long generateRemakeScriptPersonaFusion(SvViralVideo viral, Long userId) {
        if (personaViralFusionService == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "人设融合服务不可用");
        }
        Long personaId = firstPersonaIdFromMatchedJson(viral.getMatchedPersonaIds());
        if (personaId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "人设融合模式需先在确认二创时选择人设");
        }
        String remakeType = StringUtils.hasText(viral.getConfirmedRemakeType())
                ? viral.getConfirmedRemakeType()
                : "form_imitation";
        Map<String, Object> fused = personaViralFusionService.generatePersonaFusedScript(
                viral.getId(), personaId, remakeType, userId);
        if (fused.containsKey("error")) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, String.valueOf(fused.get("error")));
        }
        Object scriptObj = fused.get("script");
        String content = scriptObj != null ? String.valueOf(scriptObj) : "";

        SvScript script = new SvScript();
        script.setOwnerId(userId);
        String title = viral.getTitle() != null ? viral.getTitle() : "爆款二创脚本";
        if (title.length() > 80) {
            title = title.substring(0, 80);
        }
        script.setTitle("二创(人设融合): " + title);
        script.setContent(content);
        script.setScriptType("viral_fusion");
        script.setGenerationType("ai");
        script.setReferenceViralId(viral.getId());
        Object vc = fused.get("violationCheck");
        if (vc != null) {
            try {
                script.setTags(objectMapper.writeValueAsString(Map.of("violationCheck", vc)));
            } catch (Exception e) {
                log.warn("[二创人设融合脚本] 合规标签写入失败: {}", e.getMessage());
            }
        } else {
            attachViolationTags(script, content, userId);
        }
        SvScript saved = scriptRepository.save(script);

        viral.setRemakeStatus(3);
        viral.setRemakeScriptId(saved.getId());
        viralVideoRepository.save(viral);
        return saved.getId();
    }

    private void attachViolationTags(SvScript script, String text, Long userId) {
        if (violationWordService == null || !StringUtils.hasText(text)) {
            return;
        }
        try {
            ViolationCheckResultVO check = violationWordService.check(text, "video", userId);
            if (check != null && check.isHasViolation()) {
                script.setTags(objectMapper.writeValueAsString(Map.of("violationCheck", check)));
            }
        } catch (Exception e) {
            log.warn("[二创脚本] 合规检测写入标签失败: {}", e.getMessage());
        }
    }

    private static Long firstPersonaIdFromMatchedJson(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        String t = json.trim();
        try {
            if (t.startsWith("[")) {
                String inner = t.substring(1, t.length() - 1).trim();
                if (inner.isEmpty()) {
                    return null;
                }
                String first = inner.split(",")[0].trim();
                return Long.parseLong(first);
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long assignToShootingTask(Long viralVideoId, Long photographerId, String shootDate, Long userId) {
        if (shootingTaskService == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "拍摄任务服务不可用");
        }
        SvViralVideo viral = viralVideoService.getViralVideo(viralVideoId, userId);
        if (viral.getRemakeStatus() == null || viral.getRemakeStatus() != 3) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "需先生成脚本才能分配拍摄任务");
        }
        if (viral.getRemakeScriptId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "缺少二创脚本 ID");
        }
        if (!StringUtils.hasText(shootDate)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "shootDate 不能为空（yyyy-MM-dd）");
        }

        SvShootingTaskSaveVO taskVo = new SvShootingTaskSaveVO();
        String vt = viral.getTitle() != null ? viral.getTitle() : "爆款二创";
        taskVo.setTitle("二创: " + (vt.length() > 50 ? vt.substring(0, 50) : vt));
        taskVo.setScriptId(viral.getRemakeScriptId());
        taskVo.setPhotographerId(photographerId);
        taskVo.setShootDate(shootDate.trim());
        taskVo.setDescription("基于爆款视频二创，原视频: " + (viral.getVideoUrl() != null ? viral.getVideoUrl() : ""));
        Long taskId = shootingTaskService.save(taskVo, userId);

        viral.setRemakeStatus(4);
        viral.setRemakeTaskId(taskId);
        viralVideoRepository.save(viral);
        return taskId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markCompleted(Long viralVideoId, Long userId) {
        SvViralVideo viral = viralVideoService.getViralVideo(viralVideoId, userId);
        if (viral.getRemakeStatus() == null || viral.getRemakeStatus() != 4) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "仅「已进拍摄」状态可标记完成");
        }
        viral.setRemakeStatus(5);
        viralVideoRepository.save(viral);
    }

    private String inferIndustryTagsJson(SvViralVideo viral) {
        try {
            if (StringUtils.hasText(viral.getHashtags())) {
                JsonNode h = objectMapper.readTree(viral.getHashtags());
                if (h.isArray() && !h.isEmpty()) {
                    List<String> labels = new ArrayList<>();
                    for (JsonNode n : h) {
                        String s = n.asText("").trim();
                        if (StringUtils.hasText(s) && labels.size() < 8) {
                            labels.add(s);
                        }
                    }
                    if (!labels.isEmpty()) {
                        return objectMapper.writeValueAsString(labels);
                    }
                }
            }
            if (StringUtils.hasText(viral.getTags())) {
                String[] parts = viral.getTags().split("[,，\\s]+");
                List<String> labels = new ArrayList<>();
                for (String p : parts) {
                    if (StringUtils.hasText(p) && labels.size() < 8) {
                        labels.add(p.trim());
                    }
                }
                if (!labels.isEmpty()) {
                    return objectMapper.writeValueAsString(labels);
                }
            }
            return objectMapper.writeValueAsString(List.of("通用内容"));
        } catch (Exception e) {
            log.debug("[行业标签] 推断失败，使用默认: {}", e.getMessage());
            try {
                return objectMapper.writeValueAsString(List.of("通用内容"));
            } catch (Exception e2) {
                return "[\"通用内容\"]";
            }
        }
    }

    private String generateRemakeSuggestions(SvViralVideo viral) {
        if (llmClient == null) {
            return DEFAULT_SUGGESTIONS.trim();
        }
        AiModel model = pickDefaultTextModel();
        if (model == null) {
            return DEFAULT_SUGGESTIONS.trim();
        }
        String deep = StringUtils.hasText(viral.getDeepAnalysisResult())
                ? viral.getDeepAnalysisResult()
                : viral.getAnalysisResult();
        StringBuilder ctx = new StringBuilder(deep != null ? deep : "无");
        if (StringUtils.hasText(viral.getRemakeVariableTable())) {
            ctx.append("\n\n【可复刻变量表】\n").append(viral.getRemakeVariableTable());
        }
        String best = extractBestRemakeTypeFromJson(deep);
        if (StringUtils.hasText(best)) {
            ctx.append("\n\n【深度分析建议优先二创类型】").append(best);
        }
        String prompt = String.format("""
                分析以下爆款视频，生成 3 种二创建议。

                视频标题: %s
                深度/综合分析上下文:
                %s
                爆款分数: %s

                请输出 JSON 数组，每个元素包含:
                - remakeType: form_imitation / content_flip / element_recombination / dimensional_upgrade
                - angle: 具体切入角度
                - brief: 50字以内简述
                - matchScore: 0-1 匹配度

                只输出 JSON，不要其他文字。
                """,
                viral.getTitle(),
                ctx.length() > 12000 ? ctx.substring(0, 12000) : ctx.toString(),
                viral.getViralScore());
        try {
            LlmClient.LlmResponse resp = llmClient.chat(model, "你是短视频二创策划", prompt);
            if (resp != null && resp.success() && StringUtils.hasText(resp.content())) {
                return resp.content().trim();
            }
        } catch (Exception e) {
            log.warn("[二创建议生成] LLM 调用失败: {}", e.getMessage());
        }
        return DEFAULT_SUGGESTIONS.trim();
    }

    private static String extractBestRemakeTypeFromJson(String deepJson) {
        if (!StringUtils.hasText(deepJson)) {
            return null;
        }
        int i = deepJson.indexOf("\"bestRemakeType\"");
        if (i < 0) {
            return null;
        }
        int colon = deepJson.indexOf(':', i);
        if (colon < 0) {
            return null;
        }
        int q1 = deepJson.indexOf('"', colon + 1);
        if (q1 < 0) {
            return null;
        }
        int q2 = deepJson.indexOf('"', q1 + 1);
        if (q2 < 0) {
            return null;
        }
        return deepJson.substring(q1 + 1, q2).trim();
    }

    private AiModel pickDefaultTextModel() {
        List<AiModel> models = aiModelRepository.findByStatusAndDeleted(1, 0);
        return models.isEmpty() ? null : models.get(0);
    }
}
