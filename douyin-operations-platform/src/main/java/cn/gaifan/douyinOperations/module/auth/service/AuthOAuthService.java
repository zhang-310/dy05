package cn.gaifan.douyinOperations.module.auth.service;

import cn.gaifan.douyinOperations.module.auth.vo.LoginResultVO;
import cn.gaifan.douyinOperations.module.auth.vo.OAuthUserInfo;

/**
 * 第三方 OAuth 登录与绑定：授权 URL、回调（code 换 token、取用户信息、登录/绑定）
 */
public interface AuthOAuthService {

    /**
     * 获取跳转至第三方的授权 URL；未配置或未启用时返回 null
     *
     * @param provider 平台：wechat / qq / douyin / volcano
     * @param state    前端传入，如 login 或 bind:userId，回调时原样带回
     * @return 完整授权 URL，或 null
     */
    String getAuthorizeUrl(String provider, String state);

    /**
     * 处理 OAuth 回调：用 code 换 access_token、拉取用户信息，再登录或绑定
     *
     * @param provider 平台
     * @param code    授权码
     * @param state   与授权时一致，login 或 bind:userId
     * @return 登录结果（含 token），绑定场景也返回当前用户 token
     */
    LoginResultVO handleCallback(String provider, String code, String state);

    /**
     * 当前用户绑定指定第三方（需已登录）
     *
     * @param userId   当前用户 ID
     * @param provider 平台
     * @param code     OAuth 回调的 code（由前端从回调 URL 取到后调用本接口）
     * @return 绑定成功后的用户信息摘要，便于前端刷新
     */
    OAuthUserInfo bind(Long userId, String provider, String code);

    /**
     * 当前用户解绑指定第三方
     */
    void unbind(Long userId, String provider);

    /**
     * 当前用户已绑定的第三方列表（provider 标识）
     */
    java.util.List<String> listBoundProviders(Long userId);
}
