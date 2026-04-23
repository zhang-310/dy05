package cn.gaifan.douyinOperations.module.copy.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoAiService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AiCopyGenerateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 文案模块 AI 写文案
 */
@Tag(name = "文案 AI / Copy AI", description = "AI 写文案（需登录）")
@RestController
@RequestMapping("/api/v1/copy/ai")
public class CopyAiController {

    @Resource
    private ShortVideoAiService shortVideoAiService;

    @Operation(summary = "AI 生成文案")
    @PostMapping("/generate")
    public RESTResult<String> generate(@RequestBody(required = false) java.util.Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");

        AiCopyGenerateVO vo = new AiCopyGenerateVO();
        if (body != null) {
            String topic = body.get("topic") instanceof String ? (String) body.get("topic") : null;
            if ((topic == null || topic.isBlank()) && body.get("category") instanceof String) topic = (String) body.get("category");
            vo.setTopic(topic);
            if (body.get("style") instanceof String) vo.setStyle((String) body.get("style"));
            if (body.get("keywords") instanceof String) vo.setKeywords((String) body.get("keywords"));
            if (body.get("personaId") instanceof Number) vo.setPersonaId(((Number) body.get("personaId")).longValue());
            if (body.get("length") instanceof Number) vo.setLength(((Number) body.get("length")).intValue());
        }
        if (vo.getTopic() == null || vo.getTopic().isBlank()) vo.setTopic("通用文案");

        String content = shortVideoAiService.generateCopy(vo, userId, "copy_processing");
        return RESTResult.getSuccess(content);
    }
}
