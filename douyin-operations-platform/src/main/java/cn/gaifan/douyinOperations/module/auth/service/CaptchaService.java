package cn.gaifan.douyinOperations.module.auth.service;

import cn.gaifan.douyinOperations.module.auth.vo.CaptchaVO;

/**
 * 图片验证码：生成与校验（防暴力破解，首次失败后需验证码）
 */
public interface CaptchaService {

    /**
     * 生成验证码图片，返回 captchaId 与 base64 图片；验证码存入缓存，5 分钟有效
     */
    CaptchaVO generate();

    /**
     * 校验验证码，校验后该 captchaId 立即失效
     */
    boolean validate(String captchaId, String code);

    /**
     * 记录该 IP 登录失败，后续该 IP 在有效期内需验证码
     */
    void recordLoginFailure(String clientIp);

    /**
     * 该 IP 是否在「需验证码」有效期内
     */
    boolean requireCaptcha(String clientIp);

    /** 需验证码的有效期（分钟） */
    int getRequireCaptchaMinutes();
}
