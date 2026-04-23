package cn.gaifan.douyinOperations.module.douyinapi.service.impl;

import cn.gaifan.douyinOperations.module.douyinapi.client.DouyinApiClient;
import cn.gaifan.douyinOperations.module.douyinapi.entity.OAuthToken;
import cn.gaifan.douyinOperations.module.douyinapi.repository.OAuthTokenRepository;
import cn.gaifan.douyinOperations.module.douyinapi.service.OAuthTokenService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Optional;

@Service
public class OAuthTokenServiceImpl implements OAuthTokenService {

    private static final Logger log = LoggerFactory.getLogger(OAuthTokenServiceImpl.class);

    @Resource
    private OAuthTokenRepository tokenRepository;

    @Resource
    private DouyinApiClient douyinApiClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateToken(Long userId, String provider, String openId,
                                  String accessToken, String refreshToken, long expiresIn, String scope) {
        Optional<OAuthToken> existing = tokenRepository.findByUserIdAndProviderAndDeleted(userId, provider, 0);

        if (existing.isPresent()) {
            // 更新现有 token
            Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + expiresIn * 1000);
            int updated = tokenRepository.updateToken(userId, provider, accessToken, refreshToken, expiresAt);
            // 如果传入了 scope，也更新 scope
            if (scope != null && !scope.isBlank() && updated > 0) {
                OAuthToken token = existing.get();
                token.setScope(scope);
                tokenRepository.save(token);
            }
            if (updated > 0) {
                log.info("更新 OAuth token: userId={}, provider={}", userId, provider);
            }
        } else {
            // 创建新 token
            OAuthToken token = new OAuthToken();
            token.setUserId(userId);
            token.setProvider(provider);
            token.setOpenId(openId);
            token.setAccessToken(accessToken);
            token.setRefreshToken(refreshToken);
            token.setExpiresAt(new Timestamp(System.currentTimeMillis() + expiresIn * 1000));
            token.setScope(scope);
            tokenRepository.save(token);
            log.info("保存 OAuth token: userId={}, provider={}, openId={}", userId, provider, openId);
        }
    }

    @Override
    public Optional<OAuthToken> getToken(Long userId, String provider) {
        return tokenRepository.findByUserIdAndProviderAndDeleted(userId, provider, 0);
    }

    @Override
    public String getValidAccessToken(Long userId, String provider) {
        Optional<OAuthToken> tokenOpt = getToken(userId, provider);
        if (tokenOpt.isEmpty()) {
            log.warn("未找到 OAuth token: userId={}, provider={}", userId, provider);
            return null;
        }

        OAuthToken token = tokenOpt.get();

        // 如果 token 已过期或即将过期，尝试刷新
        if (token.isExpired() || token.isExpiringSoon()) {
            log.info("Token 已过期或即将过期，尝试刷新: userId={}, provider={}", userId, provider);
            boolean refreshed = refreshToken(userId, provider);
            if (!refreshed) {
                log.error("刷新 token 失败: userId={}, provider={}", userId, provider);
                return null;
            }
            // 重新获取刷新后的 token
            tokenOpt = getToken(userId, provider);
            if (tokenOpt.isEmpty()) {
                return null;
            }
            token = tokenOpt.get();
        }

        return token.getAccessToken();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean refreshToken(Long userId, String provider) {
        Optional<OAuthToken> tokenOpt = getToken(userId, provider);
        if (tokenOpt.isEmpty()) {
            log.warn("未找到 OAuth token，无法刷新: userId={}, provider={}", userId, provider);
            return false;
        }

        OAuthToken token = tokenOpt.get();
        if (token.getRefreshToken() == null || token.getRefreshToken().isBlank()) {
            log.warn("缺少 refresh_token，无法刷新: userId={}, provider={}", userId, provider);
            return false;
        }

        try {
            // 根据不同的 provider 调用不同的刷新接口
            if ("douyin".equals(provider)) {
                DouyinApiClient.AccessTokenResponse response = douyinApiClient.refreshAccessToken(token.getRefreshToken());
                if (response != null) {
                    saveOrUpdateToken(userId, provider, token.getOpenId(),
                            response.accessToken(), response.refreshToken(), response.expiresIn(), token.getScope());
                    log.info("刷新 token 成功: userId={}, provider={}", userId, provider);
                    return true;
                }
            }
            // TODO: 添加其他 provider 的刷新逻辑（wechat, qq 等）

            log.error("刷新 token 失败: userId={}, provider={}", userId, provider);
            return false;

        } catch (Exception e) {
            log.error("刷新 token 异常: userId={}, provider={}", userId, provider, e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteToken(Long userId, String provider) {
        int deleted = tokenRepository.deleteByUserIdAndProvider(userId, provider);
        if (deleted > 0) {
            log.info("删除 OAuth token: userId={}, provider={}", userId, provider);
        }
    }

    @Override
    public boolean isTokenValid(Long userId, String provider) {
        Optional<OAuthToken> tokenOpt = getToken(userId, provider);
        if (tokenOpt.isEmpty()) {
            return false;
        }
        OAuthToken token = tokenOpt.get();
        return !token.isExpired();
    }
}
