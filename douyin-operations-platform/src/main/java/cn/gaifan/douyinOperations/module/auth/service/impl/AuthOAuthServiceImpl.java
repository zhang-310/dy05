package cn.gaifan.douyinOperations.module.auth.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.auth.config.OAuthProviderProperties;
import cn.gaifan.douyinOperations.module.auth.entity.AuthThirdPartyBind;
import cn.gaifan.douyinOperations.module.auth.entity.AuthUser;
import cn.gaifan.douyinOperations.module.auth.repository.AuthThirdPartyBindRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthUserRepository;
import cn.gaifan.douyinOperations.module.auth.service.AuthOAuthService;
import cn.gaifan.douyinOperations.contract.auth.AuthTokenStore;
import cn.gaifan.douyinOperations.module.auth.vo.LoginResultVO;
import cn.gaifan.douyinOperations.module.auth.vo.OAuthUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.annotation.Resource;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

/**
 * 第三方 OAuth：授权 URL 构建、code 换 token、拉取用户信息、登录/绑定
 * 各平台 OAuth 参数与响应格式不同，此处做通用封装；未配置 appId 时明确失败。
 */
@Service
public class AuthOAuthServiceImpl implements AuthOAuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthOAuthServiceImpl.class);
    private static final String CALLBACK_PATH = "/api/v1/auth/oauth/callback";
    private static final String STATE_SEP = ":";

    @Resource
    private OAuthProviderProperties oauthProperties;
    @Resource
    private AuthThirdPartyBindRepository thirdPartyBindRepository;
    @Resource
    private AuthUserRepository authUserRepository;
    @Resource
    private AuthTokenStore authTokenStore;
    @Resource
    private RestTemplate restTemplate;

    @Override
    public String getAuthorizeUrl(String provider, String state) {
        OAuthProviderProperties.ProviderConfig config = getConfig(provider);
        if (config == null || !config.isEnabled() || !StringUtils.hasText(config.getAppId())) {
            return null;
        }
        String baseUrl = StringUtils.hasText(oauthProperties.getCallbackBaseUrl())
                ? oauthProperties.getCallbackBaseUrl().replaceAll("/$", "")
                : "http://localhost:8091";
        String redirectUri = baseUrl + CALLBACK_PATH;
        String fullState = provider + STATE_SEP + (StringUtils.hasText(state) ? state : "login");

        return UriComponentsBuilder.fromHttpUrl(config.getAuthorizeUrl())
                .queryParam("response_type", "code")
                .queryParam("client_id", config.getAppId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", StringUtils.hasText(config.getScope()) ? config.getScope() : "user_info")
                .queryParam("state", fullState)
                .build()
                .toUriString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResultVO handleCallback(String provider, String code, String state) {
        if (!StringUtils.hasText(provider) || !StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "缺少 provider 或 code");
        }
        String innerState = state != null && state.contains(STATE_SEP) ? state.substring(state.indexOf(STATE_SEP) + 1) : "login";
        OAuthUserInfo oauthUser = fetchOAuthUserInfo(provider, code);
        if (oauthUser == null || !StringUtils.hasText(oauthUser.getOpenId())) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "获取第三方用户信息失败");
        }

        if (innerState.startsWith("bind:")) {
            String userIdStr = innerState.substring("bind:".length()).trim();
            long userId = Long.parseLong(userIdStr);
            doBind(userId, provider, oauthUser);
            AuthUser user = authUserRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID, "用户不存在"));
            String token = authTokenStore.createToken(user.getId(), user.getRoleCode());
            return new LoginResultVO(token, user.getId(), user.getUsername(), user.getNickname(), user.getAvatarUrl(), user.getRoleCode(), userId, token);
        }

        // login: 已绑定则直接登录，未绑定则创建用户并绑定
        return loginOrRegister(provider, oauthUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OAuthUserInfo bind(Long userId, String provider, String code) {
        if (userId == null || !StringUtils.hasText(provider) || !StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "参数不完整");
        }
        if (thirdPartyBindRepository.existsByUserIdAndProviderAndDeleted(userId, provider, 0)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "该第三方已绑定");
        }
        OAuthUserInfo oauthUser = fetchOAuthUserInfo(provider, code);
        if (oauthUser == null || !StringUtils.hasText(oauthUser.getOpenId())) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "获取第三方用户信息失败");
        }
        doBind(userId, provider, oauthUser);
        return oauthUser;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbind(Long userId, String provider) {
        AuthThirdPartyBind bind = thirdPartyBindRepository.findByUserIdAndProviderAndDeleted(userId, provider, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAIL, "未绑定该第三方"));
        bind.setDeleted(1);
        bind.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        thirdPartyBindRepository.save(bind);
    }

    @Override
    public java.util.List<String> listBoundProviders(Long userId) {
        return thirdPartyBindRepository.findByUserIdAndDeleted(userId, 0).stream()
                .map(AuthThirdPartyBind::getProvider)
                .collect(java.util.stream.Collectors.toList());
    }

    private OAuthProviderProperties.ProviderConfig getConfig(String provider) {
        if (provider == null) return null;
        return oauthProperties.getProviders().get(provider.toLowerCase());
    }

    private void doBind(Long userId, String provider, OAuthUserInfo oauthUser) {
        if (thirdPartyBindRepository.findByProviderAndOpenIdAndDeleted(provider, oauthUser.getOpenId(), 0).isPresent()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "该第三方账号已被其他用户绑定");
        }
        AuthThirdPartyBind bind = new AuthThirdPartyBind();
        bind.setUserId(userId);
        bind.setProvider(provider);
        bind.setOpenId(oauthUser.getOpenId());
        bind.setUnionId(oauthUser.getUnionId());
        bind.setDeleted(0);
        thirdPartyBindRepository.save(bind);
    }

    private LoginResultVO loginOrRegister(String provider, OAuthUserInfo oauthUser) {
        java.util.Optional<AuthThirdPartyBind> bindOpt = thirdPartyBindRepository.findByProviderAndOpenIdAndDeleted(provider, oauthUser.getOpenId(), 0);
        AuthUser user;
        if (bindOpt.isPresent()) {
            user = authUserRepository.findById(bindOpt.get().getUserId()).orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID, "用户不存在"));
        } else {
            user = createUserFromOAuth(provider, oauthUser);
            AuthThirdPartyBind bind = new AuthThirdPartyBind();
            bind.setUserId(user.getId());
            bind.setProvider(provider);
            bind.setOpenId(oauthUser.getOpenId());
            bind.setUnionId(oauthUser.getUnionId());
            bind.setDeleted(0);
            thirdPartyBindRepository.save(bind);
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已封禁");
        }
        String token = authTokenStore.createToken(user.getId(), user.getRoleCode());
        user.setLastLoginAt(new Timestamp(System.currentTimeMillis()));
        authUserRepository.save(user);
        return new LoginResultVO(token, user.getId(), user.getUsername(), user.getNickname(), user.getAvatarUrl(), user.getRoleCode(), null, token);
    }

    private AuthUser createUserFromOAuth(String provider, OAuthUserInfo oauthUser) {
        String username = provider + "_" + oauthUser.getOpenId();
        if (username.length() > 64) username = username.substring(0, 64);
        if (authUserRepository.findByUsernameAndDeleted(username, 0).isPresent()) {
            username = provider + "_" + oauthUser.getOpenId() + "_" + UUID.randomUUID().toString().substring(0, 8);
            if (username.length() > 64) username = username.substring(0, 64);
        }
        AuthUser user = new AuthUser();
        user.setUsername(username);
        user.setPasswordHash(UUID.randomUUID().toString());
        user.setNickname(StringUtils.hasText(oauthUser.getNickname()) ? oauthUser.getNickname() : username);
        user.setAvatarUrl(oauthUser.getAvatarUrl());
        user.setRoleCode("user");
        user.setStatus(0);
        user.setDeleted(0);
        authUserRepository.save(user);
        return user;
    }

    /**
     * 用 code 换 token 并拉取用户信息；未配置 appId 时明确失败，不创建模拟用户。
     */
    private OAuthUserInfo fetchOAuthUserInfo(String provider, String code) {
        OAuthProviderProperties.ProviderConfig config = getConfig(provider);
        if (config == null) return null;
        if (!StringUtils.hasText(config.getAppId())) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "第三方登录未配置 appId: " + provider);
        }
        String baseUrl = StringUtils.hasText(oauthProperties.getCallbackBaseUrl())
                ? oauthProperties.getCallbackBaseUrl().replaceAll("/$", "") : "http://localhost:8091";
        String redirectUri = baseUrl + CALLBACK_PATH;

        String accessToken = exchangeCodeForToken(provider, config, code, redirectUri);
        if (accessToken == null) return null;
        return fetchUserInfo(provider, config, accessToken);
    }

    private String exchangeCodeForToken(String provider, OAuthProviderProperties.ProviderConfig config, String code, String redirectUri) {
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("code", code);
            params.add("redirect_uri", redirectUri);
            params.add("client_id", config.getAppId());
            params.add("client_secret", config.getAppSecret());
            if ("wechat".equalsIgnoreCase(provider)) {
                params.remove("client_id");
                params.remove("client_secret");
                params.add("appid", config.getAppId());
                params.add("secret", config.getAppSecret());
            }
            String resp = restTemplate.postForObject(config.getTokenUrl(), params, String.class);
            if (resp == null) return null;
            if (resp.contains("access_token")) {
                for (String part : resp.split("&")) {
                    if (part.startsWith("access_token=")) return part.substring("access_token=".length()).trim();
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("OAuth token exchange failed, provider={}", provider, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private OAuthUserInfo fetchUserInfo(String provider, OAuthProviderProperties.ProviderConfig config, String accessToken) {
        try {
            String url = config.getUserInfoUrl() + (config.getUserInfoUrl().contains("?") ? "&" : "?") + "access_token=" + URLEncoder.encode(accessToken, "UTF-8");
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            if (resp == null) return null;
            String openId = getString(resp, "openid", "open_id");
            String unionId = getString(resp, "unionid", "union_id");
            String nickname = getString(resp, "nickname", "nickname");
            String avatar = getString(resp, "avatar", "avatar_url", "figureurl", "figureurl_2");
            return new OAuthUserInfo(openId, unionId, nickname, avatar);
        } catch (Exception e) {
            log.warn("OAuth userInfo fetch failed, provider={}", provider, e);
            return null;
        }
    }

    private static String getString(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v != null) return v.toString();
        }
        return null;
    }
}
