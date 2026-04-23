package cn.gaifan.douyinOperations.module.auth.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.auth.entity.AuthLoginLog;
import cn.gaifan.douyinOperations.module.auth.entity.AuthOrganization;
import cn.gaifan.douyinOperations.module.auth.entity.AuthUser;
import cn.gaifan.douyinOperations.module.auth.entity.AuthVerifyCode;
import cn.gaifan.douyinOperations.module.auth.repository.AuthLoginLogRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthOrganizationRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthUserRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthVerifyCodeRepository;
import cn.gaifan.douyinOperations.module.auth.service.AuthLoginLogService;
import cn.gaifan.douyinOperations.module.auth.service.AuthLoginService;
import cn.gaifan.douyinOperations.contract.auth.AuthTokenStore;
import cn.gaifan.douyinOperations.module.auth.service.CaptchaService;
import cn.gaifan.douyinOperations.module.auth.vo.ForgotPasswordVO;
import cn.gaifan.douyinOperations.module.auth.vo.LoginResultVO;
import cn.gaifan.douyinOperations.module.auth.vo.LoginVO;
import cn.gaifan.douyinOperations.module.auth.vo.SmsSendVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.sql.Timestamp;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 登录：校验密码、生成 token、更新最后登录时间、写登录记录
 */
@Service
public class AuthLoginServiceImpl implements AuthLoginService {

    private static final int VERIFY_CODE_EXPIRE_MINUTES = 5;
    private static final int VERIFY_CODE_LENGTH = 6;

    @Resource
    private AuthUserRepository authUserRepository;
    @Resource
    private AuthLoginLogRepository authLoginLogRepository;
    @Resource
    private AuthLoginLogService authLoginLogService;
    @Resource
    private AuthVerifyCodeRepository authVerifyCodeRepository;
    @Resource
    private AuthTokenStore authTokenStore;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private CaptchaService captchaService;
    @Resource
    private AuthOrganizationRepository authOrganizationRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResultVO login(LoginVO vo) {
        return login(vo, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResultVO login(LoginVO vo, String clientIp) {
        AuthUser user;
        String loginType = (vo.getLoginType() != null && !vo.getLoginType().isEmpty()) ? vo.getLoginType().trim().toLowerCase() : "password";

        if ("sms".equals(loginType) || "email".equals(loginType)) {
            // 手机号/邮箱验证码登录
            String target = vo.getTarget() != null ? vo.getTarget().trim() : "";
            String code = vo.getCode() != null ? vo.getCode().trim() : "";
            if (target.isEmpty() || code.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "手机号/邮箱和验证码不能为空");
            }
            final String smsLoginType = loginType;
            Timestamp now = new Timestamp(System.currentTimeMillis());
            AuthVerifyCode codeEntity = authVerifyCodeRepository
                    .findFirstByTargetAndTypeAndUsedAndExpireAtAfterOrderByCreateTimeDesc(target, "login", 0, now)
                    .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID, "验证码无效或已过期"));
            if (!codeEntity.getCode().equals(code)) {
                authLoginLogService.logFailedLogin(target, smsLoginType, clientIp, "验证码错误");
                throw new BusinessException(ErrorCode.TOKEN_INVALID, "验证码错误");
            }
            if ("sms".equals(smsLoginType)) {
                user = authUserRepository.findByMobileAndDeleted(target, 0)
                        .orElseThrow(() -> {
                            saveLoginLog(null, target, smsLoginType, clientIp, 0, "该手机号未绑定账号");
                            return new BusinessException(ErrorCode.USER_NOT_FOUND, "该手机号未绑定账号");
                        });
            } else {
                user = authUserRepository.findByEmailAndDeleted(target, 0)
                        .orElseThrow(() -> {
                            saveLoginLog(null, target, smsLoginType, clientIp, 0, "该邮箱未绑定账号");
                            return new BusinessException(ErrorCode.USER_NOT_FOUND, "该邮箱未绑定账号");
                        });
            }
            codeEntity.setUsed(1);
            authVerifyCodeRepository.save(codeEntity);
        } else {
            // 账号密码登录（首次失败后需图片验证码）
            String username = vo.getUsername() != null ? vo.getUsername().trim() : "";
            String password = vo.getPassword();
            if (username.isEmpty() || password == null || password.isEmpty()) {
                throw new BusinessException(ErrorCode.TOKEN_INVALID, "用户名和密码不能为空");
            }
            boolean needCaptcha = false; // 开发环境禁用验证码
            if (needCaptcha) {
                String cid = vo.getCaptchaId() != null ? vo.getCaptchaId().trim() : "";
                String ccode = vo.getCaptchaCode() != null ? vo.getCaptchaCode().trim() : "";
                if (cid.isEmpty() || ccode.isEmpty()) {
                    throw new BusinessException(ErrorCode.CAPTCHA_REQUIRED, "请先完成验证码");
                }
                if (!captchaService.validate(cid, ccode)) {
                    throw new BusinessException(ErrorCode.TOKEN_INVALID, "验证码错误");
                }
            }
            user = authUserRepository.findByUsernameAndDeleted(username, 0)
                    .orElseThrow(() -> {
                        authLoginLogService.logFailedLogin(username, "password", clientIp, "用户不存在");
                        return new BusinessException(ErrorCode.LOGIN_FAILED, "用户名或密码错误");
                    });
            if (user.getStatus() != null && user.getStatus() == 1) {
                authLoginLogService.logFailedLogin(user.getUsername(), loginType, clientIp, "账号已封禁");
                throw new BusinessException(ErrorCode.USER_DISABLED, "账号已封禁");
            }
            if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                captchaService.recordLoginFailure(clientIp);
                authLoginLogService.logFailedLogin(user.getUsername(), "password", clientIp, "密码错误");
                throw new BusinessException(ErrorCode.LOGIN_FAILED, "用户名或密码错误，请完成验证码后重试");
            }
            loginType = "password";
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            authLoginLogService.logFailedLogin(user.getUsername(), loginType, clientIp, "账号已封禁");
            throw new BusinessException(ErrorCode.USER_DISABLED, "账号已封禁");
        }

        String token = authTokenStore.createToken(user.getId(), user.getRoleCode(), user.getOrganizationId());
        user.setLastLoginAt(new Timestamp(System.currentTimeMillis()));
        authUserRepository.save(user);

        // 使用独立事务记录成功登录
        authLoginLogService.logSuccessLogin(user.getId(), user.getUsername(), loginType, clientIp);

        LoginResultVO result = new LoginResultVO();
        result.setToken(token);
        result.setUserId(user.getId());
        result.setUsername(user.getUsername());
        result.setNickname(user.getNickname());
        result.setAvatarUrl(user.getAvatarUrl());
        result.setRoleCode(user.getRoleCode());
        result.setOrganizationId(user.getOrganizationId());
        if (user.getOrganizationId() != null) {
            authOrganizationRepository.findById(user.getOrganizationId())
                    .ifPresent(org -> result.setOrganizationName(org.getOrgName()));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendVerifyCode(SmsSendVO vo) {
        String type = "forgot_password".equals(vo.getType()) ? "forgot_password" : "login";
        String code = generateNumericCode(VERIFY_CODE_LENGTH);
        long expireMs = System.currentTimeMillis() + VERIFY_CODE_EXPIRE_MINUTES * 60 * 1000L;
        AuthVerifyCode entity = new AuthVerifyCode();
        entity.setTarget(vo.getTarget().trim());
        entity.setType(type);
        entity.setCode(code);
        entity.setExpireAt(new Timestamp(expireMs));
        entity.setUsed(0);
        entity.setTryCount(0);
        authVerifyCodeRepository.save(entity);
        // 首版不真实发送短信/邮件，仅落库供忘记密码校验；后续可接短信/邮件网关
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forgotPassword(ForgotPasswordVO vo) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        AuthVerifyCode codeEntity = authVerifyCodeRepository
                .findFirstByTargetAndTypeAndUsedAndExpireAtAfterOrderByCreateTimeDesc(
                        vo.getTarget().trim(), "forgot_password", 0, now)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID, "验证码无效或已过期"));
        if (!codeEntity.getCode().equals(vo.getCode().trim())) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "验证码错误");
        }
        AuthUser user = authUserRepository.findByMobileAndDeleted(vo.getTarget().trim(), 0)
                .orElseGet(() -> authUserRepository.findByEmailAndDeleted(vo.getTarget().trim(), 0)
                        .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID, "未找到该手机号或邮箱对应的账号")));
        user.setPasswordHash(passwordEncoder.encode(vo.getNewPassword()));
        user.setUpdateTime(now);
        authUserRepository.save(user);
        codeEntity.setUsed(1);
        authVerifyCodeRepository.save(codeEntity);
    }

    private void saveLoginLog(Long userId, String username, String loginType, String ip, int status, String failReason) {
        AuthLoginLog log = new AuthLoginLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setLoginType(loginType);
        log.setDeviceType("web");
        log.setIp(ip);
        log.setStatus(status);
        log.setFailReason(failReason);
        authLoginLogRepository.save(log);
    }

    private static String generateNumericCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) sb.append(r.nextInt(10));
        return sb.toString();
    }
}
