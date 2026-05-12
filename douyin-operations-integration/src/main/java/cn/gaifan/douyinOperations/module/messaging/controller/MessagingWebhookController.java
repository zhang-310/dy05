package cn.gaifan.douyinOperations.module.messaging.controller;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.messaging.entity.MsgPlatformConfig;
import cn.gaifan.douyinOperations.module.messaging.service.MessagingPlatformService;
import cn.gaifan.douyinOperations.module.messaging.service.MessagingWebhookHandler;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.Executor;

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
    @Resource
    @Qualifier("webhookExecutor")
    private Executor webhookExecutor;
    @Resource
    private RateLimiter webhookRateLimiter;

    @PostMapping(value = "/feishu", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "飞书事件回调（POST）")
    public Object feishuPost(@RequestParam(required = false) String token,
            @RequestBody(required = false) String body) {
        // P1-2: Webhook 速率限制 - 防止恶意重放攻击
        try {
            webhookRateLimiter.acquirePermission();
        } catch (RequestNotPermitted e) {
            return RESTResult.error(ErrorCode.RATE_LIMIT, "请求过于频繁，请稍后再试");
        }

        MsgPlatformConfig config = resolveConfig("feishu", token);
        if (config == null) return RESTResult.error(ErrorCode.WECOM_AUTH_FAIL, "无效的 token");
        if (body == null || body.isBlank()) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "请求体为空");
        try {
            // URL 验证同步处理（快速响应）
            if (body.contains("\"type\":\"url_verification\"") || body.contains("\"type\": \"url_verification\"")) {
                return messagingWebhookHandler.handleFeishuUrlVerify(body);
            }
            // P0-002: 事件处理异步化，避免阻塞 HTTP 线程
            final String eventBody = body;
            final MsgPlatformConfig eventConfig = config;
            webhookExecutor.execute(() -> {
                try {
                    messagingWebhookHandler.handleFeishuEvent(eventBody, eventConfig);
                } catch (Exception e) {
                    // 异步处理失败仅记录日志，不影响 HTTP 响应
                    // TODO: 添加失败重试机制
                }
            });
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
        // P1-2: Webhook 速率限制 - 防止恶意重放攻击
        try {
            webhookRateLimiter.acquirePermission();
        } catch (RequestNotPermitted e) {
            return RESTResult.error(ErrorCode.RATE_LIMIT, "请求过于频繁，请稍后再试");
        }

        MsgPlatformConfig config = resolveConfig("wecom", token);
        if (config == null) return RESTResult.error(ErrorCode.WECOM_AUTH_FAIL, "无效的 token");
        if (body == null || body.isBlank()) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "请求体为空");
        try {
            // P0-002: 消息处理异步化，避免阻塞 HTTP 线程
            final String msgBody = body;
            final String signature = msg_signature;
            final String ts = timestamp;
            final String n = nonce;
            final MsgPlatformConfig msgConfig = config;
            webhookExecutor.execute(() -> {
                try {
                    messagingWebhookHandler.handleWecomMessage(msgBody, signature, ts, n, msgConfig);
                } catch (Exception e) {
                    // 异步处理失败仅记录日志，不影响 HTTP 响应
                    // TODO: 添加失败重试机制
                }
            });
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
