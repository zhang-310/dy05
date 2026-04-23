package cn.gaifan.douyinOperations.module.messaging.controller;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.messaging.entity.MsgPlatformConfig;
import cn.gaifan.douyinOperations.module.messaging.service.MessagingPlatformService;
import cn.gaifan.douyinOperations.module.messaging.service.MessagingWebhookHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 企微/飞书 Webhook 回调端点（无需登录，验签通过即可）
 * URL 示例：/api/v1/messaging/webhook/feishu?token=xxx  或  /api/v1/messaging/webhook/wecom?token=xxx
 */
@RestController
@RequestMapping("/api/v1/messaging/webhook")
@Tag(name = "企微/飞书 Webhook", description = "入站回调（公开端点，通过 token 验签）")
public class MessagingWebhookController {

    @Resource
    private MessagingPlatformService messagingPlatformService;
    @Resource
    private MessagingWebhookHandler messagingWebhookHandler;

    @PostMapping(value = "/feishu", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "飞书事件回调（POST）")
    public Object feishuPost(@RequestParam(required = false) String token,
            @RequestBody(required = false) String body) {
        MsgPlatformConfig config = resolveConfig("feishu", token);
        if (config == null) return RESTResult.error(ErrorCode.WECOM_AUTH_FAIL, "无效的 token");
        if (body == null || body.isBlank()) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "请求体为空");
        try {
            if (body.contains("\"type\":\"url_verification\"") || body.contains("\"type\": \"url_verification\"")) {
                return messagingWebhookHandler.handleFeishuUrlVerify(body);
            }
            messagingWebhookHandler.handleFeishuEvent(body, config);
            return RESTResult.success();
        } catch (BusinessException e) {
            return RESTResult.error(e.getCode(), e.getMessage());
        }
    }

    @GetMapping("/wecom")
    @Operation(summary = "企微 URL 验证（GET）")
    public String wecomVerify(@RequestParam String msg_signature,
            @RequestParam String timestamp,
            @RequestParam String nonce,
            @RequestParam String echostr,
            @RequestParam(required = false) String token) {
        MsgPlatformConfig config = resolveConfig("wecom", token);
        if (config == null) throw new BusinessException(ErrorCode.WECOM_AUTH_FAIL, "无效的 token");
        return messagingWebhookHandler.handleWecomUrlVerify(msg_signature, timestamp, nonce, echostr, config);
    }

    @PostMapping(value = "/wecom", produces = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "企微消息回调（POST）")
    public Object wecomPost(@RequestParam(required = false) String token,
            @RequestParam(required = false) String msg_signature,
            @RequestParam(required = false) String timestamp,
            @RequestParam(required = false) String nonce,
            @RequestBody(required = false) String body) {
        MsgPlatformConfig config = resolveConfig("wecom", token);
        if (config == null) return RESTResult.error(ErrorCode.WECOM_AUTH_FAIL, "无效的 token");
        if (body == null || body.isBlank()) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "请求体为空");
        try {
            messagingWebhookHandler.handleWecomMessage(body, msg_signature, timestamp, nonce, config);
            return "success";
        } catch (BusinessException e) {
            return RESTResult.error(e.getCode(), e.getMessage());
        }
    }

    private MsgPlatformConfig resolveConfig(String platform, String token) {
        if (token == null || token.isBlank()) return null;
        return messagingPlatformService.getConfigEntityByPlatformAndToken(platform, token);
    }
}
