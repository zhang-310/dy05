package cn.gaifan.douyinOperations.module.douyinapi.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.douyin.entity.DouyinAccount;
import cn.gaifan.douyinOperations.module.douyin.repository.DouyinAccountRepository;
import cn.gaifan.douyinOperations.module.douyinapi.client.DouyinApiClient;
import cn.gaifan.douyinOperations.module.douyinapi.entity.OAuthToken;
import cn.gaifan.douyinOperations.module.douyinapi.service.OAuthTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

/**
 * 抖音 OAuth 授权控制器
 */
@RestController
@RequestMapping("/api/v1/douyin/oauth")
@Tag(name = "抖音授权 / Douyin OAuth", description = "抖音开放平台 OAuth 2.0 授权")
public class DouyinOAuthController {

    private static final Logger log = LoggerFactory.getLogger(DouyinOAuthController.class);

    @Resource
    private DouyinApiClient douyinApiClient;

    @Resource
    private OAuthTokenService oauthTokenService;

    @Resource
    private DouyinAccountRepository douyinAccountRepository;

    @Value("${douyin.api.callback-url}")
    private String callbackUrl;

    @Value("${douyin.api.client-secret}")
    private String clientSecret;

    @Value("${douyin.api.oauth.scope:user_info,video.list,live.room}")
    private String oauthScope;

    /** State 有效期：10 分钟 */
    private static final long STATE_TTL_MS = 10 * 60 * 1000L;

    /**
     * 获取授权 URL
     */
    @GetMapping("/authorize-url")
    @Operation(summary = "获取授权 URL / Get Authorization URL")
    public RESTResult<Map<String, String>> getAuthorizeUrl(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        long timestamp = System.currentTimeMillis();
        String payload = userId + "_" + timestamp;
        String signature = hmacSha256(payload);
        String state = payload + "_" + signature;
        String authUrl = douyinApiClient.getAuthUrl(callbackUrl, state);

        RESTResult<Map<String, String>> r = RESTResult.getSuccess(Map.of(
                "authUrl", authUrl,
                "state", state
        ));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * OAuth 回调端点
     * 抖音授权后会重定向到此端点，携带 code 和 state
     */
    @GetMapping("/callback")
    @Operation(summary = "OAuth 回调 / OAuth Callback")
    public String callback(@RequestParam String code, @RequestParam String state) {
        log.info("收到抖音 OAuth 回调: code={}, state={}", code, state);

        try {
            // 1. 使用 code 换取 access_token
            DouyinApiClient.AccessTokenResponse tokenResponse = douyinApiClient.getAccessToken(code);
            if (tokenResponse == null) {
                log.error("获取 access_token 失败");
                return "授权失败：无法获取 access_token";
            }

            // 2. 从 state 中提取 userId
            String userId = extractUserIdFromState(state);
            if (userId == null) {
                log.error("无效的 state: {}", state);
                return "授权失败：无效的 state";
            }

            // 3. 存储 access_token 到数据库
            oauthTokenService.saveOrUpdateToken(
                    Long.parseLong(userId),
                    "douyin",
                    tokenResponse.openId(),
                    tokenResponse.accessToken(),
                    tokenResponse.refreshToken(),
                    tokenResponse.expiresIn(),
                    oauthScope
            );

            log.info("抖音授权成功: userId={}, openId={}", userId, tokenResponse.openId());

            // 4. 重定向到前端成功页面
            return "<html><body><h2>授权成功！</h2><p>您可以关闭此页面。</p><script>window.close();</script></body></html>";

        } catch (Exception e) {
            log.error("处理 OAuth 回调失败", e);
            return "授权失败：" + e.getMessage();
        }
    }

    /**
     * 刷新 access_token
     */
    @PostMapping("/refresh-token")
    @Operation(summary = "刷新 Token / Refresh Token")
    public RESTResult<Map<String, Object>> refreshToken(HttpServletRequest request,
                                                         @RequestParam String refreshToken) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        DouyinApiClient.AccessTokenResponse tokenResponse = douyinApiClient.refreshAccessToken(refreshToken);
        if (tokenResponse == null) {
            return RESTResult.error(ErrorCode.INTERNAL_ERROR, "刷新 token 失败");
        }

        // 更新数据库中的 token，保留原有 scope
        var existingToken = oauthTokenService.getToken(userId, "douyin");
        String existingScope = existingToken.map(t -> t.getScope()).orElse(null);
        oauthTokenService.saveOrUpdateToken(
                userId,
                "douyin",
                tokenResponse.openId(),
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                tokenResponse.expiresIn(),
                existingScope
        );

        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(Map.of(
                "accessToken", tokenResponse.accessToken(),
                "expiresIn", tokenResponse.expiresIn()
        ));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 撤销 OAuth 授权（删除 token）
     */
    @PostMapping("/revoke")
    @Operation(summary = "撤销授权 / Revoke Token")
    public RESTResult<Void> revokeToken(HttpServletRequest request,
                                         @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        // 支持按 accountId 撤销
        if (body != null && body.get("accountId") != null) {
            Long accountId = ((Number) body.get("accountId")).longValue();
            Optional<DouyinAccount> accountOpt = douyinAccountRepository.findById(accountId);
            if (accountOpt.isPresent()) {
                userId = accountOpt.get().getUserId();
            }
        }
        oauthTokenService.deleteToken(userId, "douyin");
        log.info("用户 {} 撤销了抖音授权", userId);
        RESTResult<Void> r = RESTResult.getSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 获取 Token 状态（前端轮询用）
     */
    @PostMapping("/token-status")
    @Operation(summary = "查询 Token 状态 / Token Status")
    public RESTResult<Map<String, Object>> tokenStatus(HttpServletRequest request,
                                                        @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long accountId = body.get("accountId") != null ? ((Number) body.get("accountId")).longValue() : null;
        if (accountId != null) {
            Optional<DouyinAccount> accountOpt = douyinAccountRepository.findById(accountId);
            if (accountOpt.isPresent()) {
                userId = accountOpt.get().getUserId();
            }
        }

        Optional<OAuthToken> tokenOpt = oauthTokenService.getToken(userId, "douyin");
        if (tokenOpt.isEmpty()) {
            RESTResult<Map<String, Object>> r = RESTResult.getSuccess(Map.of("status", "expired"));
            r.setTraceId(MDC.get("traceId"));
            return r;
        }

        OAuthToken token = tokenOpt.get();
        String status;
        if (token.isExpired()) {
            status = "expired";
        } else {
            status = "valid";
        }

        long daysLeft = 0;
        String expireTime = null;
        if (token.getExpiresAt() != null) {
            LocalDateTime expiry = token.getExpiresAt().toLocalDateTime();
            expireTime = expiry.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            daysLeft = Math.max(0, ChronoUnit.DAYS.between(LocalDateTime.now(), expiry));
        }

        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(Map.of(
                "status", status,
                "expireTime", expireTime != null ? expireTime : "",
                "daysLeft", daysLeft
        ));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 刷新 Token（前端按钮用）
     */
    @PostMapping("/token-refresh")
    @Operation(summary = "刷新 Token（按账号）/ Refresh Token by Account")
    public RESTResult<Void> tokenRefresh(HttpServletRequest request,
                                          @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        Long accountId = body.get("accountId") != null ? ((Number) body.get("accountId")).longValue() : null;
        if (accountId != null) {
            Optional<DouyinAccount> accountOpt = douyinAccountRepository.findById(accountId);
            if (accountOpt.isPresent()) {
                userId = accountOpt.get().getUserId();
            }
        }

        boolean success = oauthTokenService.refreshToken(userId, "douyin");
        if (!success) {
            return RESTResult.error(ErrorCode.INTERNAL_ERROR, "刷新 Token 失败，请重新授权");
        }
        RESTResult<Void> r = RESTResult.getSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 获取授权 URL（POST，前端统一 POST 调用）
     */
    @PostMapping("/auth-url")
    @Operation(summary = "获取授权 URL（POST）/ Get Authorization URL (POST)")
    public RESTResult<Map<String, String>> getAuthUrlPost(HttpServletRequest request,
                                                           @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        long timestamp = System.currentTimeMillis();
        String payload = userId + "_" + timestamp;
        String signature = hmacSha256(payload);
        String state = payload + "_" + signature;
        String authUrl = douyinApiClient.getAuthUrl(callbackUrl, state);

        RESTResult<Map<String, String>> r = RESTResult.getSuccess(Map.of(
                "authUrl", authUrl,
                "state", state
        ));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ─── 工具方法 ──────────────────────────────────────

    /**
     * 从 state 中提取并验证 userId。
     * state 格式: {userId}_{timestamp}_{hmac}
     * 校验 HMAC 签名 + 时间戳过期
     */
    private String extractUserIdFromState(String state) {
        try {
            if (state == null || state.isBlank()) return null;
            int lastUnderscore = state.lastIndexOf('_');
            if (lastUnderscore <= 0) return null;

            String payload = state.substring(0, lastUnderscore);
            String receivedSig = state.substring(lastUnderscore + 1);

            // 验证 HMAC 签名
            String expectedSig = hmacSha256(payload);
            if (!expectedSig.equals(receivedSig)) {
                log.warn("OAuth state 签名校验失败: {}", state);
                return null;
            }

            // 解析 payload: {userId}_{timestamp}
            String[] parts = payload.split("_");
            if (parts.length != 2) return null;

            String userId = parts[0];
            long timestamp = Long.parseLong(parts[1]);

            // 验证时间戳未过期
            if (System.currentTimeMillis() - timestamp > STATE_TTL_MS) {
                log.warn("OAuth state 已过期: timestamp={}", timestamp);
                return null;
            }

            return userId;
        } catch (Exception e) {
            log.error("解析 state 失败: {}", state, e);
        }
        return null;
    }

    /** HMAC-SHA256 签名 */
    private String hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 签名失败", e);
        }
    }
}
