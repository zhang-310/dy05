package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.script.service.ViolationWordService;
import cn.gaifan.douyinOperations.module.script.vo.ViolationCheckResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoAiService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "短视频 AI 创作")
@RestController
@RequestMapping("/api/v1/short-video/ai")
public class ShortVideoAiController {

    @Resource
    private ShortVideoAiService aiService;
    @Resource
    private ViolationWordService violationWordService;

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
        return RESTResult.getSuccess(result);
    }

    @Operation(summary = "AI 生成文案")
    @PostMapping("/generate-copy")
    public RESTResult<String> generateCopy(@RequestBody AiCopyGenerateVO vo, HttpServletRequest request) {
        Long userId = requireUserId(request);
        String copy = aiService.generateCopy(vo, userId);
        return RESTResult.getSuccess(copy);
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

    private Long requireUserId(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return userId;
    }
}
