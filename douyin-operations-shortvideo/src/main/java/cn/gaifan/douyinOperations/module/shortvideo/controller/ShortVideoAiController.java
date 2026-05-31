package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.script.service.ViolationWordService;
import cn.gaifan.douyinOperations.module.script.vo.ViolationCheckResultVO;
import cn.gaifan.douyinOperations.module.ai.service.OperationalStrategyKnowledgeService;
import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoAiService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@Tag(name = "短视频 AI 创作")
@RestController
@RequestMapping("/api/v1/short-video/ai")
public class ShortVideoAiController {

    @Resource
    private ShortVideoAiService aiService;
    @Resource
    private ViolationWordService violationWordService;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private OperationalStrategyKnowledgeService operationalStrategyKnowledgeService;

    @Operation(summary = "文案违规检测（scope=video）")
    @PostMapping("/check-violation")
    public RESTResult<ViolationCheckResultVO> checkViolation(@RequestBody(required = false) java.util.Map<String, String> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        String text = body != null ? body.get("text") : null;
        if (text == null || text.isBlank()) {
            text = body != null ? body.get("content") : null;
        }
        if (text == null || text.isBlank()) {
            ViolationCheckResultVO empty = new ViolationCheckResultVO();
            empty.setHasViolation(false);
            empty.setTotalCount(0);
            empty.setViolations(java.util.Collections.emptyList());
            return RESTResult.getSuccess(empty);
        }
        ViolationCheckResultVO result = violationWordService.check(text, "video", userId);
        List<DouyinOfficialReferenceVO> refs = officialViolationRefs(userId, text);
        if (!hasViolationRuleRef(refs)) {
            result.setHasViolation(true);
            result.setViolations(appendOfficialReferenceGateHit(result.getViolations()));
            result.setTotalCount(result.getViolations() != null ? result.getViolations().size() : 1);
        }
        return RESTResult.getSuccess(result);
    }

    @Operation(summary = "文案违规检测（scope=video，含抖音官方规则引用）")
    @PostMapping("/check-violation-rich")
    public RESTResult<Map<String, Object>> checkViolationRich(@RequestBody(required = false) java.util.Map<String, String> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        String text = body != null ? body.get("text") : null;
        if (text == null || text.isBlank()) {
            text = body != null ? body.get("content") : null;
        }
        if (text == null || text.isBlank()) {
            ViolationCheckResultVO empty = new ViolationCheckResultVO();
            empty.setHasViolation(false);
            empty.setTotalCount(0);
            empty.setViolations(java.util.Collections.emptyList());
            return RESTResult.getSuccess(Map.of(
                    "violationCheck", empty,
                    "officialReferences", List.of(),
                    "officialReferenceRequired", true,
                    "officialReferenceSatisfied", false,
                    "officialReferenceStatus", "missing_text"
            ));
        }
        ViolationCheckResultVO result = violationWordService.check(text, "video", userId);
        List<DouyinOfficialReferenceVO> refs = officialViolationRefs(userId, text);
        boolean officialSatisfied = hasViolationRuleRef(refs);
        if (!officialSatisfied) {
            result.setHasViolation(true);
            result.setViolations(appendOfficialReferenceGateHit(result.getViolations()));
            result.setTotalCount(result.getViolations() != null ? result.getViolations().size() : 1);
        }
        return RESTResult.getSuccess(Map.of(
                "violationCheck", result,
                "officialReferences", refs,
                "officialReferenceRequired", true,
                "officialReferenceSatisfied", officialSatisfied,
                "officialReferenceStatus", officialSatisfied ? "satisfied" : "missing_douyin_weigui_reference"
        ));
    }

    @Operation(summary = "AI 生成文案")
    @PostMapping("/generate-copy")
    public RESTResult<String> generateCopy(@RequestBody AiCopyGenerateVO vo, HttpServletRequest request) {
        Long userId = requireUserId(request);
        String copy = aiService.generateCopy(vo, userId);
        return RESTResult.getSuccess(copy);
    }

    @Operation(summary = "AI 生成文案（含抖音官方规则引用）")
    @PostMapping("/generate-copy-rich")
    public RESTResult<AiTextGenerateResultVO> generateCopyRich(@RequestBody AiCopyGenerateVO vo, HttpServletRequest request) {
        Long userId = requireUserId(request);
        return RESTResult.getSuccess(aiService.generateCopyRich(vo, userId, null));
    }

    @Operation(summary = "AI 生成脚本")
    @PostMapping("/generate-script")
    public RESTResult<String> generateScript(@RequestBody AiScriptGenerateVO vo, HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (vo.getCopyText() == null || vo.getCopyText().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "文案内容不能为空");
        }
        String script = aiService.generateScript(vo, userId);
        return RESTResult.getSuccess(script);
    }

    @Operation(summary = "AI 生成脚本（含抖音官方规则引用）")
    @PostMapping("/generate-script-rich")
    public RESTResult<AiTextGenerateResultVO> generateScriptRich(@RequestBody AiScriptGenerateVO vo, HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (vo.getCopyText() == null || vo.getCopyText().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "文案内容不能为空");
        }
        return RESTResult.getSuccess(aiService.generateScriptRich(vo, userId));
    }

    @Operation(summary = "AI 生成标题")
    @PostMapping("/generate-title")
    public RESTResult<List<String>> generateTitles(@RequestBody AiTitleGenerateVO vo, HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (vo.getCopyText() == null || vo.getCopyText().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "文案内容不能为空");
        }
        List<String> titles = aiService.generateTitles(vo, userId);
        return RESTResult.getSuccess(titles);
    }

    @Operation(summary = "AI 生成视频方案")
    @PostMapping("/generate-plan")
    public RESTResult<String> generateVideoPlan(@RequestBody AiVideoPlanGenerateVO vo, HttpServletRequest request) {
        Long userId = requireUserId(request);
        String plan = aiService.generateVideoPlan(vo, userId);
        return RESTResult.getSuccess(plan);
    }

    @Operation(summary = "AI 生成视频方案（含抖音官方规则引用）")
    @PostMapping("/generate-plan-rich")
    public RESTResult<AiTextGenerateResultVO> generateVideoPlanRich(@RequestBody AiVideoPlanGenerateVO vo, HttpServletRequest request) {
        Long userId = requireUserId(request);
        return RESTResult.getSuccess(aiService.generateVideoPlanRich(vo, userId));
    }

    private Long requireUserId(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return userId;
    }

    private List<DouyinOfficialReferenceVO> officialViolationRefs(Long userId, String text) {
        if (operationalStrategyKnowledgeService == null || userId == null || text == null || text.isBlank()) {
            return List.of();
        }
        try {
            var context = operationalStrategyKnowledgeService.buildShortVideoGenerationContext(
                    userId,
                    text + " 短视频违规 抖音官方规则 千川素材违规 商品宣传违规",
                    1800
            );
            var ruleContext = operationalStrategyKnowledgeService.buildViolationRuleContext(
                    userId,
                    text + " 短视频违规 千川素材违规 商品宣传违规 官方规则",
                    "short_video_violation_check",
                    1800
            );
            List<cn.gaifan.douyinOperations.module.ai.service.OperationalStrategyKnowledgeService.OfficialReference> refs =
                    new java.util.ArrayList<>();
            if (context != null && context.officialReferences() != null) {
                refs.addAll(context.officialReferences());
            }
            if (ruleContext != null && ruleContext.officialReferences() != null) {
                refs.addAll(ruleContext.officialReferences());
            }
            return refs.stream()
                    .filter(ref -> "violation_rule".equals(ref.refType()))
                    .collect(java.util.stream.Collectors.toMap(
                            ref -> String.valueOf(ref.docId()) + ":" + String.valueOf(ref.chunkId()),
                            ref -> ref,
                            (a, b) -> a,
                            java.util.LinkedHashMap::new))
                    .values().stream()
                    .map(ref -> {
                        DouyinOfficialReferenceVO vo = new DouyinOfficialReferenceVO();
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
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private boolean hasViolationRuleRef(List<DouyinOfficialReferenceVO> refs) {
        if (refs == null || refs.isEmpty()) {
            return false;
        }
        return refs.stream().anyMatch(ref ->
                "violation_rule".equals(ref.getRefType()) || "douyin_weigui".equals(ref.getKbName()));
    }

    private List<ViolationCheckResultVO.ViolationHitVO> appendOfficialReferenceGateHit(
            List<ViolationCheckResultVO.ViolationHitVO> existing) {
        List<ViolationCheckResultVO.ViolationHitVO> hits = new java.util.ArrayList<>(
                existing != null ? existing : List.of());
        ViolationCheckResultVO.ViolationHitVO hit = new ViolationCheckResultVO.ViolationHitVO();
        hit.setWord("官方规则引用缺失");
        hit.setPosition(0);
        hit.setLength(0);
        hit.setReason("未检索到 douyin_weigui 官方违规规则引用，禁止判定为审核通过");
        hit.setLevel(3);
        hit.setSource("official_rule_gate");
        hits.add(hit);
        return hits;
    }
}
