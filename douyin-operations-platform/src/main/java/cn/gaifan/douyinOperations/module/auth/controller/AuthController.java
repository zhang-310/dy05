package cn.gaifan.douyinOperations.module.auth.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.event.LoginSuccessEvent;
import cn.gaifan.douyinOperations.common.util.IPUtils;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.auth.service.AuthLoginService;
import cn.gaifan.douyinOperations.module.auth.service.AuthOAuthService;
import cn.gaifan.douyinOperations.module.auth.service.AuthMenuService;
import cn.gaifan.douyinOperations.module.auth.service.AuthResourceService;
import cn.gaifan.douyinOperations.contract.auth.AuthTokenStore;
import cn.gaifan.douyinOperations.module.auth.service.AuthUserService;
import cn.gaifan.douyinOperations.module.auth.service.CaptchaService;
import cn.gaifan.douyinOperations.module.auth.vo.CaptchaVO;
import cn.gaifan.douyinOperations.module.auth.vo.ForgotPasswordVO;
import cn.gaifan.douyinOperations.module.auth.vo.LoginResultVO;
import cn.gaifan.douyinOperations.module.auth.vo.LoginVO;
import cn.gaifan.douyinOperations.module.auth.vo.OAuthUserInfo;
import cn.gaifan.douyinOperations.module.auth.vo.MenuItemVO;
import cn.gaifan.douyinOperations.module.auth.vo.ProfileVO;
import cn.gaifan.douyinOperations.module.auth.vo.ProfileUpdateVO;
import cn.gaifan.douyinOperations.module.auth.vo.ChangePasswordVO;
import cn.gaifan.douyinOperations.module.auth.vo.ResourceCodeVO;
import cn.gaifan.douyinOperations.module.auth.vo.SmsSendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 认证与个人信息管理：登录、个人信息、菜单、资源列表、OAuth 登录
 * Authentication & Personal Information Management: Login, Profile, Menu, Resources, OAuth Login
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "认证管理 / Authentication", description = "用户认证、个人信息、菜单和权限资源")
public class AuthController {

    @Resource
    private AuthLoginService authLoginService;
    @Resource
    private CaptchaService captchaService;
    @Resource
    private AuthUserService authUserService;
    @Resource
    private AuthMenuService authMenuService;
    @Resource
    private AuthResourceService authResourceService;
    @Resource
    private ApplicationEventPublisher applicationEventPublisher;
    @Resource
    private AuthOAuthService authOAuthService;
    @Resource
    private AuthTokenStore authTokenStore;

    @PostMapping("/captcha")
    @Operation(
            summary = "获取验证码 / Get Captcha",
            description = "生成新的图形验证码用于登录 / Generate a new image captcha for login")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "成功 / Success"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<CaptchaVO> captcha(@RequestBody(required = false) java.util.Map<String, Object> body) {
        CaptchaVO data = captchaService.generate();
        RESTResult<CaptchaVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/login")
    @Operation(
            summary = "用户登录 / User Login",
            description = "使用用户名密码进行登录并获取 JWT 令牌 / Login with username and password to get JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "登录成功 / Login successful"),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误 / Invalid username or password"),
            @ApiResponse(responseCode = "403", description = "账号被禁用 / Account disabled"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<LoginResultVO> login(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "登录信息 / Login credentials",
            required = true,
            content = @Content(schema = @Schema(implementation = LoginVO.class))
    ) @RequestBody LoginVO vo) {
        String clientIp = IPUtils.getRealIP(request);
        LoginResultVO data = authLoginService.login(vo, clientIp);
        applicationEventPublisher.publishEvent(new LoginSuccessEvent(this,
                data.getUserId(), data.getUsername(),
                request.getRequestURI(), request.getMethod(),
                IPUtils.getRealIP(request), request.getHeader("User-Agent")));
        RESTResult<LoginResultVO> r = RESTResult.success("登录成功", data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/sms/send")
    @Operation(
            summary = "发送短信验证码 / Send SMS Verification Code",
            description = "发送短信验证码到指定手机号 / Send SMS verification code to specified phone number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "发送成功 / Send successful"),
            @ApiResponse(responseCode = "400", description = "请求参数错误 / Invalid request parameters"),
            @ApiResponse(responseCode = "429", description = "请求过于频繁 / Too many requests"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Void> smsSend(@Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "短信发送信息 / SMS send info",
            required = true,
            content = @Content(schema = @Schema(implementation = SmsSendVO.class))
    ) @RequestBody SmsSendVO vo) {
        authLoginService.sendVerifyCode(vo);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "忘记密码 / Forgot Password",
            description = "通过手机验证码重置密码 / Reset password via SMS verification code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "重置成功 / Reset successful"),
            @ApiResponse(responseCode = "400", description = "验证码无效或已过期 / Invalid or expired verification code"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Void> forgotPassword(@Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "忘记密码信息 / Forgot password info",
            required = true,
            content = @Content(schema = @Schema(implementation = ForgotPasswordVO.class))
    ) @RequestBody ForgotPasswordVO vo) {
        authLoginService.forgotPassword(vo);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/profile")
    @Operation(
            summary = "获取个人资料 / Get User Profile",
            description = "获取当前登录用户的个人资料信息 / Get current user's profile information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "获取成功 / Get successful"),
            @ApiResponse(responseCode = "401", description = "未登录或令牌无效 / Not logged in or invalid token"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<ProfileVO> profile(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(cn.gaifan.douyinOperations.common.constant.ErrorCode.UNAUTHORIZED, "未登录");
        }
        ProfileVO data = authUserService.getProfile(userId);
        RESTResult<ProfileVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/profile/update")
    @Operation(
            summary = "更新个人资料 / Update User Profile",
            description = "更新当前登录用户的个人资料信息 / Update current user's profile information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "更新成功 / Update successful"),
            @ApiResponse(responseCode = "400", description = "请求参数错误 / Invalid request parameters"),
            @ApiResponse(responseCode = "401", description = "未登录或令牌无效 / Not logged in or invalid token"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Void> profileUpdate(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "个人资料更新信息 / Profile update info",
            required = true,
            content = @Content(schema = @Schema(implementation = ProfileUpdateVO.class))
    ) @RequestBody ProfileUpdateVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(cn.gaifan.douyinOperations.common.constant.ErrorCode.UNAUTHORIZED, "未登录");
        }
        authUserService.updateProfile(userId, vo);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/profile/change-password")
    @Operation(
            summary = "修改密码 / Change Password",
            description = "修改当前登录用户的登录密码 / Change current user's login password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "修改成功 / Change successful"),
            @ApiResponse(responseCode = "400", description = "原密码错误 / Old password incorrect"),
            @ApiResponse(responseCode = "401", description = "未登录或令牌无效 / Not logged in or invalid token"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Void> changePassword(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "修改密码信息 / Change password info",
            required = true,
            content = @Content(schema = @Schema(implementation = ChangePasswordVO.class))
    ) @RequestBody ChangePasswordVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(cn.gaifan.douyinOperations.common.constant.ErrorCode.UNAUTHORIZED, "未登录");
        }
        authUserService.changePassword(userId, vo);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/menu/search")
    @Operation(
            summary = "获取权限菜单 / Get Permission Menu",
            description = "获取当前登录用户有权限的菜单列表 / Get menu list that current user has permission to access")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "获取成功 / Get successful"),
            @ApiResponse(responseCode = "401", description = "未登录或令牌无效 / Not logged in or invalid token"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<List<MenuItemVO>> menuSearch(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(cn.gaifan.douyinOperations.common.constant.ErrorCode.UNAUTHORIZED, "未登录");
        }
        List<MenuItemVO> data = authMenuService.getMenuList(userId);
        RESTResult<List<MenuItemVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/resource/search")
    @Operation(
            summary = "获取权限资源 / Get Permission Resources",
            description = "获取当前登录用户有权限的资源编码列表 / Get resource code list that current user has permission to access")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "获取成功 / Get successful"),
            @ApiResponse(responseCode = "401", description = "未登录或令牌无效 / Not logged in or invalid token"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<ResourceCodeVO> resourceSearch(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(cn.gaifan.douyinOperations.common.constant.ErrorCode.UNAUTHORIZED, "未登录");
        }
        ResourceCodeVO data = authResourceService.getResourceCodes(userId);
        RESTResult<ResourceCodeVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // --------------- 第三方 OAuth 登录与绑定 ---------------
    @GetMapping("/oauth/authorize")
    @Operation(
            summary = "获取 OAuth 授权 URL / Get OAuth Authorization URL",
            description = "获取第三方平台（微信、QQ、抖音等）的授权登录 URL / Get third-party platform (WeChat, QQ, Douyin, etc.) authorization login URL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "获取成功 / Get successful"),
            @ApiResponse(responseCode = "400", description = "该第三方登录暂未配置或未启用 / Third-party login not configured or disabled"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<java.util.Map<String, String>> oauthAuthorize(HttpServletRequest request,
            @Parameter(description = "OAuth 提供者 / OAuth provider (wechat, qq, douyin, volcano)", required = true)
            @RequestParam String provider,
            @Parameter(description = "状态参数 / State parameter (login or bind)", required = false)
            @RequestParam(required = false, defaultValue = "login") String state) {
        if ("bind".equals(state)) {
            Long userId = AuthTokenFilter.getUserId(request);
            if (userId != null) state = "bind:" + userId;
        }
        String url = authOAuthService.getAuthorizeUrl(provider, state);
        if (url == null) {
            return RESTResult.error(cn.gaifan.douyinOperations.common.constant.ErrorCode.VALIDATION_FAIL, "该第三方登录暂未配置或未启用");
        }
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("url", url);
        RESTResult<java.util.Map<String, String>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @GetMapping("/oauth/callback")
    @Operation(
            summary = "OAuth 回调处理 / OAuth Callback Handler",
            description = "处理第三方平台的 OAuth 回调，完成登录或绑定 / Handle OAuth callback from third-party platform to complete login or binding")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "重定向到前端页面 / Redirect to frontend page"),
            @ApiResponse(responseCode = "400", description = "缺少授权码 / Missing authorization code"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RedirectView oauthCallback(
            @Parameter(description = "授权码 / Authorization code from provider", required = false)
            @RequestParam(required = false) String code,
            @Parameter(description = "状态参数 / State parameter from provider", required = false)
            @RequestParam(required = false) String state) {
        try {
            if (code == null || code.isEmpty()) {
                throw new cn.gaifan.douyinOperations.common.exception.BusinessException(cn.gaifan.douyinOperations.common.constant.ErrorCode.TOKEN_INVALID, "缺少授权码");
            }
            String provider = (state != null && state.contains(":")) ? state.substring(0, state.indexOf(":")) : "wechat";
            cn.gaifan.douyinOperations.module.auth.vo.LoginResultVO result = authOAuthService.handleCallback(provider, code, state);
            String token = result.getToken();
            String redirectPath = "/pages/auth/oauth-callback.html?token=" + java.net.URLEncoder.encode(token, "UTF-8")
                    + "&userId=" + result.getUserId() + "&username=" + java.net.URLEncoder.encode(result.getUsername() != null ? result.getUsername() : "", "UTF-8");
            if (state != null && state.contains("bind:")) redirectPath += "&from=bind";
            return new RedirectView(redirectPath);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "登录失败";
            try {
                return new RedirectView("/pages/auth/login.html?oauth_error=" + java.net.URLEncoder.encode(msg, "UTF-8"));
            } catch (java.io.UnsupportedEncodingException ue) {
                return new RedirectView("/pages/auth/login.html?oauth_error=login_failed");
            }
        }
    }

    @PostMapping("/oauth/bindings")
    @Operation(
            summary = "获取绑定的 OAuth 提供者列表 / Get Bound OAuth Providers",
            description = "获取当前登录用户已绑定的第三方平台列表 / Get list of third-party platforms bound to current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "获取成功 / Get successful"),
            @ApiResponse(responseCode = "401", description = "请先登录 / Please login first"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<java.util.List<String>> oauthBindings(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(cn.gaifan.douyinOperations.common.constant.ErrorCode.UNAUTHORIZED, "请先登录");
        }
        java.util.List<String> data = authOAuthService.listBoundProviders(userId);
        RESTResult<java.util.List<String>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/oauth/bind")
    @Operation(
            summary = "绑定 OAuth 账号 / Bind OAuth Account",
            description = "将第三方账号绑定到当前登录用户 / Bind third-party account to current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "绑定成功 / Bind successful"),
            @ApiResponse(responseCode = "401", description = "请先登录 / Please login first"),
            @ApiResponse(responseCode = "400", description = "授权码无效 / Invalid authorization code"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<OAuthUserInfo> oauthBind(HttpServletRequest request,
                                              @Parameter(description = "OAuth 提供者 / OAuth provider", required = true)
                                              @RequestParam String provider,
                                              @Parameter(description = "授权码 / Authorization code", required = true)
                                              @RequestParam String code) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(cn.gaifan.douyinOperations.common.constant.ErrorCode.UNAUTHORIZED, "请先登录");
        }
        OAuthUserInfo data = authOAuthService.bind(userId, provider, code);
        RESTResult<OAuthUserInfo> r = RESTResult.success("绑定成功", data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/oauth/unbind")
    @Operation(
            summary = "解绑 OAuth 账号 / Unbind OAuth Account",
            description = "解除当前登录用户与第三方账号的绑定关系 / Unbind third-party account from current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "解绑成功 / Unbind successful"),
            @ApiResponse(responseCode = "401", description = "请先登录 / Please login first"),
            @ApiResponse(responseCode = "400", description = "该提供者未绑定 / Provider not bound"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Void> oauthUnbind(HttpServletRequest request,
            @Parameter(description = "OAuth 提供者 / OAuth provider", required = true)
            @RequestParam String provider) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(cn.gaifan.douyinOperations.common.constant.ErrorCode.UNAUTHORIZED, "请先登录");
        }
        authOAuthService.unbind(userId, provider);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // --------------- 登出 ---------------
    @PostMapping("/logout")
    @Operation(
            summary = "用户登出 / User Logout",
            description = "登出当前用户并撤销其 JWT 令牌 / Logout current user and revoke JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "登出成功 / Logout successful"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Void> logout(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            authTokenStore.removeToken(token);
        }
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    private String extractToken(HttpServletRequest request) {
        String h = request.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) return h.substring(7).trim();
        if (h != null && !h.isEmpty()) return h.trim();
        return request.getParameter("token");
    }
}
