package cn.gaifan.douyinOperations.module.auth.service;

import cn.gaifan.douyinOperations.module.auth.vo.ForgotPasswordVO;
import cn.gaifan.douyinOperations.module.auth.vo.LoginResultVO;
import cn.gaifan.douyinOperations.module.auth.vo.LoginVO;
import cn.gaifan.douyinOperations.module.auth.vo.SmsSendVO;

/**
 * 登录：校验账号密码、生成 token、写登录记录；发送验证码、忘记密码
 */
public interface AuthLoginService {

    /**
     * 用户名+密码登录，成功返回 token 与用户信息
     *
     * @param vo 登录参数
     * @return 成功时返回 token 与用户信息；失败抛 BusinessException（如 2003 密码错误可用业务码）
     */
    LoginResultVO login(LoginVO vo);

    /**
     * 登录（带客户端 IP，用于首次失败后需验证码逻辑）
     */
    LoginResultVO login(LoginVO vo, String clientIp);

    /**
     * 发送验证码（短信/邮箱）；验证码落库，有效期 5 分钟；首版不真实发送，仅落库供忘记密码校验
     *
     * @param vo target=手机/邮箱，type=login|forgot_password
     */
    void sendVerifyCode(SmsSendVO vo);

    /**
     * 忘记密码：校验验证码后更新密码（通过 target 查找用户：手机或邮箱）
     *
     * @param vo target、code、newPassword
     */
    void forgotPassword(ForgotPasswordVO vo);
}
