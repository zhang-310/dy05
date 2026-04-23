package cn.gaifan.douyinOperations.module.sms.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.sms.vo.*;

public interface SmsService {

    // ==================== 服务商配置 ====================
    PageResultVO<SmsProviderConfigVO> searchProviderConfigs(SmsProviderConfigSearchVO vo);

    SmsProviderConfigVO getProviderConfigById(Long id);

    long saveProviderConfig(SmsProviderConfigSaveVO vo);

    void deleteProviderConfig(Long id);

    void updateProviderConfigStatus(Long id, Integer status);

    void setDefaultProviderConfig(Long id);

    // ==================== 短信模板 ====================
    PageResultVO<SmsTemplateVO> searchTemplates(SmsTemplateSearchVO vo);

    SmsTemplateVO getTemplateById(Long id);

    long saveTemplate(SmsTemplateSaveVO vo);

    void deleteTemplate(Long id);

    void updateTemplateStatus(Long id, Integer status);

    // ==================== 短信发送日志 ====================
    PageResultVO<SmsSendLogVO> searchSendLogs(SmsSendLogSearchVO vo);

    SmsSendLogVO getSendLogById(Long id);

    // ==================== 短信验证码 ====================
    void sendVerificationCode(SmsVerificationCodeSendVO vo);

    void verifyCode(SmsVerificationCodeVerifyVO vo);

    SmsVerificationCodeVO getLatestVerificationCode(String phoneNumber, String bizType);
}
