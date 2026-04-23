package cn.gaifan.douyinOperations.module.douyinapi.service;

import cn.gaifan.douyinOperations.module.douyinapi.entity.OAuthToken;

import java.util.Optional;

/**
 * OAuth Token 管理服务
 */
public interface OAuthTokenService {

    /**
     * 保存或更新 token
     */
    void saveOrUpdateToken(Long userId, String provider, String openId,
                          String accessToken, String refreshToken, long expiresIn, String scope);

    /**
     * 获取用户的 token
     */
    Optional<OAuthToken> getToken(Long userId, String provider);

    /**
     * 获取有效的 access_token（自动刷新）
     */
    String getValidAccessToken(Long userId, String provider);

    /**
     * 刷新 token
     */
    boolean refreshToken(Long userId, String provider);

    /**
     * 删除 token
     */
    void deleteToken(Long userId, String provider);

    /**
     * 检查 token 是否有效
     */
    boolean isTokenValid(Long userId, String provider);
}
