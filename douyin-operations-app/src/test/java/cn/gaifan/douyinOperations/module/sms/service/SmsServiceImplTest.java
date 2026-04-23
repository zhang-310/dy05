package cn.gaifan.douyinOperations.module.sms.service;

import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.sms.entity.*;
import cn.gaifan.douyinOperations.module.sms.repository.*;
import cn.gaifan.douyinOperations.module.sms.service.impl.SmsServiceImpl;
import cn.gaifan.douyinOperations.module.sms.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmsService 单元测试")
class SmsServiceImplTest {

    @Mock
    private SmsProviderConfigRepository providerConfigRepository;
    @Mock
    private SmsTemplateRepository templateRepository;
    @Mock
    private SmsSendLogRepository sendLogRepository;
    @Mock
    private SmsVerificationCodeRepository verificationCodeRepository;

    @InjectMocks
    private SmsServiceImpl smsService;

    private Long ownerId = 1L;
    private SmsProviderConfig mockProvider;
    private SmsTemplate mockTemplate;
    private SmsSendLog mockLog;
    private SmsVerificationCode mockCode;

    @BeforeEach
    void setUp() {
        // Mock Provider
        mockProvider = new SmsProviderConfig();
        mockProvider.setId(1L);
        mockProvider.setOwnerId(ownerId);
        mockProvider.setProviderCode("aliyun");
        mockProvider.setProviderName("阿里云短信");
        mockProvider.setStatus(1);
        mockProvider.setIsDefault(1);
        mockProvider.setDailyQuota(1000);
        mockProvider.setDailySentCount(100);

        // Mock Template
        mockTemplate = new SmsTemplate();
        mockTemplate.setId(1L);
        mockTemplate.setOwnerId(ownerId);
        mockTemplate.setTemplateCode("SMS_001");
        mockTemplate.setTemplateName("验证码模板");
        mockTemplate.setContent("您的验证码是{code}");
        mockTemplate.setProviderCode("aliyun");
        mockTemplate.setStatus(1);
        mockTemplate.setTemplateType("verification");

        // Mock Log
        mockLog = new SmsSendLog();
        mockLog.setId(1L);
        mockLog.setOwnerId(ownerId);
        mockLog.setPhoneNumber("13800138000");
        mockLog.setTemplateCode("SMS_001");
        mockLog.setProviderCode("aliyun");
        mockLog.setStatus("success");
        mockLog.setBizType("login");

        // Mock Code
        mockCode = new SmsVerificationCode();
        mockCode.setId(1L);
        mockCode.setOwnerId(ownerId);
        mockCode.setPhoneNumber("13800138000");
        mockCode.setBizType("login");
        mockCode.setCode("123456");
        mockCode.setExpiresAt(new Timestamp(System.currentTimeMillis() + 300000)); // 5分钟后
        mockCode.setIsVerified(0);
        mockCode.setAttemptCount(0);
        mockCode.setMaxAttempts(3);
    }

    // ==================== 服务商配置测试 ====================

    @Test
    @DisplayName("搜索服务商配置 - 应返回分页结果")
    void searchProviderConfigs_shouldReturnPageResult() {
        // Given
        SmsProviderConfigSearchVO searchVO = new SmsProviderConfigSearchVO();
        searchVO.setOwnerId(ownerId);
        searchVO.setPage(0);
        searchVO.setRows(10);

        Page<SmsProviderConfig> mockPage = new PageImpl<>(List.of(mockProvider));
        when(providerConfigRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        // When
        PageResultVO<SmsProviderConfigVO> result = smsService.searchProviderConfigs(searchVO);

        // Then
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getProviderCode()).isEqualTo("aliyun");
    }

    @Test
    @DisplayName("根据ID获取服务商配置 - 存在应返回VO")
    void getProviderConfigById_exists_shouldReturnVO() {
        // Given
        when(providerConfigRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockProvider));

        // When
        SmsProviderConfigVO result = smsService.getProviderConfigById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getProviderCode()).isEqualTo("aliyun");
    }

    @Test
    @DisplayName("根据ID获取服务商配置 - 不存在应抛出异常")
    void getProviderConfigById_notExists_shouldThrowException() {
        // Given
        when(providerConfigRepository.findByIdAndDeleted(999L, 0))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> smsService.getProviderConfigById(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("短信服务商配置不存在");
    }

    @Test
    @DisplayName("保存服务商配置 - 新增应创建记录")
    void saveProviderConfig_create_shouldCreateNew() {
        // Given
        SmsProviderConfigSaveVO saveVO = new SmsProviderConfigSaveVO();
        saveVO.setOwnerId(ownerId);
        saveVO.setProviderCode("aliyun");
        saveVO.setProviderName("阿里云短信");
        saveVO.setApiKey("test-key");
        saveVO.setApiSecret("test-secret");

        when(providerConfigRepository.save(any(SmsProviderConfig.class)))
                .thenReturn(mockProvider);

        // When
        long result = smsService.saveProviderConfig(saveVO);

        // Then
        assertThat(result).isEqualTo(1L);
        verify(providerConfigRepository).save(argThat(config ->
                config.getProviderCode().equals("aliyun") &&
                config.getApiKey().equals("test-key")
        ));
    }

    @Test
    @DisplayName("保存服务商配置 - 更新应修改现有记录")
    void saveProviderConfig_update_shouldModifyExisting() {
        // Given
        SmsProviderConfigSaveVO saveVO = new SmsProviderConfigSaveVO();
        saveVO.setId(1L);
        saveVO.setProviderName("更新后的名称");
        saveVO.setApiKey("new-key");
        saveVO.setApiSecret("new-secret");

        when(providerConfigRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockProvider));
        when(providerConfigRepository.save(any(SmsProviderConfig.class)))
                .thenReturn(mockProvider);

        // When
        long result = smsService.saveProviderConfig(saveVO);

        // Then
        verify(providerConfigRepository).save(argThat(config ->
                config.getProviderName().equals("更新后的名称") &&
                config.getApiKey().equals("new-key")
        ));
    }

    @Test
    @DisplayName("删除服务商配置 - 应标记为已删除")
    void deleteProviderConfig_shouldMarkAsDeleted() {
        // Given
        when(providerConfigRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockProvider));

        // When
        smsService.deleteProviderConfig(1L);

        // Then
        verify(providerConfigRepository).save(argThat(config ->
                config.getDeleted() == 1
        ));
    }

    @Test
    @DisplayName("更新服务商配置状态 - 应调用repository更新方法")
    void updateProviderConfigStatus_shouldCallRepositoryUpdate() {
        // Given
        when(providerConfigRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockProvider));

        // When
        smsService.updateProviderConfigStatus(1L, 0);

        // Then
        verify(providerConfigRepository).updateStatus(1L, 0);
    }

    @Test
    @DisplayName("设置默认服务商 - 应清除其他默认并设置新默认")
    void setDefaultProviderConfig_shouldClearOthersAndSetNew() {
        // Given
        when(providerConfigRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockProvider));

        // When
        smsService.setDefaultProviderConfig(1L);

        // Then
        verify(providerConfigRepository).clearDefaultForOwner(ownerId);
        verify(providerConfigRepository).setAsDefault(1L);
    }

    // ==================== 短信模板测试 ====================

    @Test
    @DisplayName("搜索短信模板 - 应返回分页结果")
    void searchTemplates_shouldReturnPageResult() {
        // Given
        SmsTemplateSearchVO searchVO = new SmsTemplateSearchVO();
        searchVO.setOwnerId(ownerId);
        searchVO.setPage(0);
        searchVO.setRows(10);

        Page<SmsTemplate> mockPage = new PageImpl<>(List.of(mockTemplate));
        when(templateRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        // When
        PageResultVO<SmsTemplateVO> result = smsService.searchTemplates(searchVO);

        // Then
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getTemplateCode()).isEqualTo("SMS_001");
    }

    @Test
    @DisplayName("根据ID获取模板 - 存在应返回VO")
    void getTemplateById_exists_shouldReturnVO() {
        // Given
        when(templateRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockTemplate));

        // When
        SmsTemplateVO result = smsService.getTemplateById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTemplateCode()).isEqualTo("SMS_001");
    }

    @Test
    @DisplayName("保存模板 - 新增应创建记录")
    void saveTemplate_create_shouldCreateNew() {
        // Given
        SmsTemplateSaveVO saveVO = new SmsTemplateSaveVO();
        saveVO.setOwnerId(ownerId);
        saveVO.setTemplateCode("SMS_002");
        saveVO.setTemplateName("新模板");
        saveVO.setContent("测试内容");
        saveVO.setProviderCode("aliyun");

        when(templateRepository.save(any(SmsTemplate.class)))
                .thenReturn(mockTemplate);

        // When
        long result = smsService.saveTemplate(saveVO);

        // Then
        assertThat(result).isEqualTo(1L);
        verify(templateRepository).save(argThat(template ->
                template.getTemplateCode().equals("SMS_002") &&
                template.getTemplateName().equals("新模板")
        ));
    }

    @Test
    @DisplayName("删除模板 - 应标记为已删除")
    void deleteTemplate_shouldMarkAsDeleted() {
        // Given
        when(templateRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockTemplate));

        // When
        smsService.deleteTemplate(1L);

        // Then
        verify(templateRepository).save(argThat(template ->
                template.getDeleted() == 1
        ));
    }

    @Test
    @DisplayName("更新模板状态 - 应调用repository更新方法")
    void updateTemplateStatus_shouldCallRepositoryUpdate() {
        // Given
        when(templateRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockTemplate));

        // When
        smsService.updateTemplateStatus(1L, 0);

        // Then
        verify(templateRepository).updateStatus(1L, 0);
    }

    // ==================== 短信发送日志测试 ====================

    @Test
    @DisplayName("搜索发送日志 - 应返回分页结果")
    void searchSendLogs_shouldReturnPageResult() {
        // Given
        SmsSendLogSearchVO searchVO = new SmsSendLogSearchVO();
        searchVO.setOwnerId(ownerId);
        searchVO.setPage(0);
        searchVO.setRows(10);

        Page<SmsSendLog> mockPage = new PageImpl<>(List.of(mockLog));
        when(sendLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        // When
        PageResultVO<SmsSendLogVO> result = smsService.searchSendLogs(searchVO);

        // Then
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getPhoneNumber()).isEqualTo("13800138000");
    }

    @Test
    @DisplayName("根据ID获取发送日志 - 存在应返回VO")
    void getSendLogById_exists_shouldReturnVO() {
        // Given
        when(sendLogRepository.findById(1L))
                .thenReturn(Optional.of(mockLog));

        // When
        SmsSendLogVO result = smsService.getSendLogById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPhoneNumber()).isEqualTo("13800138000");
    }

    // ==================== 短信验证码测试 ====================

    @Test
    @DisplayName("发送验证码 - 应生成6位验证码并保存")
    void sendVerificationCode_shouldGenerateAndSave() {
        // Given
        SmsVerificationCodeSendVO sendVO = new SmsVerificationCodeSendVO();
        sendVO.setOwnerId(ownerId);
        sendVO.setPhoneNumber("13800138000");
        sendVO.setBizType("login");
        sendVO.setCreatedIp("127.0.0.1");

        // When
        smsService.sendVerificationCode(sendVO);

        // Then
        verify(verificationCodeRepository).save(argThat(code ->
                code.getPhoneNumber().equals("13800138000") &&
                code.getCode().length() == 6 &&
                code.getExpiresAt() != null
        ));
    }

    @Test
    @DisplayName("验证验证码 - 正确应标记为已验证")
    void verifyCode_correct_shouldMarkAsVerified() {
        // Given
        SmsVerificationCodeVerifyVO verifyVO = new SmsVerificationCodeVerifyVO();
        verifyVO.setPhoneNumber("13800138000");
        verifyVO.setBizType("login");
        verifyVO.setCode("123456");
        verifyVO.setVerifiedIp("127.0.0.1");

        when(verificationCodeRepository.findTopByPhoneNumberAndBizTypeOrderByCreatedAtDesc("13800138000", "login"))
                .thenReturn(Optional.of(mockCode));

        // When
        smsService.verifyCode(verifyVO);

        // Then
        verify(verificationCodeRepository).markAsVerified(eq(1L), any(Timestamp.class), eq("127.0.0.1"));
    }

    @Test
    @DisplayName("验证验证码 - 不存在应抛出异常")
    void verifyCode_notExists_shouldThrowException() {
        // Given
        SmsVerificationCodeVerifyVO verifyVO = new SmsVerificationCodeVerifyVO();
        verifyVO.setPhoneNumber("13800138000");
        verifyVO.setBizType("login");
        verifyVO.setCode("123456");

        when(verificationCodeRepository.findTopByPhoneNumberAndBizTypeOrderByCreatedAtDesc("13800138000", "login"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> smsService.verifyCode(verifyVO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("验证码不存在或已过期");
    }

    @Test
    @DisplayName("验证验证码 - 已过期应抛出异常")
    void verifyCode_expired_shouldThrowException() {
        // Given
        mockCode.setExpiresAt(new Timestamp(System.currentTimeMillis() - 1000)); // 已过期

        SmsVerificationCodeVerifyVO verifyVO = new SmsVerificationCodeVerifyVO();
        verifyVO.setPhoneNumber("13800138000");
        verifyVO.setBizType("login");
        verifyVO.setCode("123456");

        when(verificationCodeRepository.findTopByPhoneNumberAndBizTypeOrderByCreatedAtDesc("13800138000", "login"))
                .thenReturn(Optional.of(mockCode));

        // When & Then
        assertThatThrownBy(() -> smsService.verifyCode(verifyVO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("验证码已过期");
    }

    @Test
    @DisplayName("验证验证码 - 已使用应抛出异常")
    void verifyCode_alreadyUsed_shouldThrowException() {
        // Given
        mockCode.setIsVerified(1);

        SmsVerificationCodeVerifyVO verifyVO = new SmsVerificationCodeVerifyVO();
        verifyVO.setPhoneNumber("13800138000");
        verifyVO.setBizType("login");
        verifyVO.setCode("123456");

        when(verificationCodeRepository.findTopByPhoneNumberAndBizTypeOrderByCreatedAtDesc("13800138000", "login"))
                .thenReturn(Optional.of(mockCode));

        // When & Then
        assertThatThrownBy(() -> smsService.verifyCode(verifyVO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("验证码已被使用");
    }

    @Test
    @DisplayName("验证验证码 - 超过尝试次数应抛出异常")
    void verifyCode_attemptLimitExceeded_shouldThrowException() {
        // Given
        mockCode.setAttemptCount(3);

        SmsVerificationCodeVerifyVO verifyVO = new SmsVerificationCodeVerifyVO();
        verifyVO.setPhoneNumber("13800138000");
        verifyVO.setBizType("login");
        verifyVO.setCode("123456");

        when(verificationCodeRepository.findTopByPhoneNumberAndBizTypeOrderByCreatedAtDesc("13800138000", "login"))
                .thenReturn(Optional.of(mockCode));

        // When & Then
        assertThatThrownBy(() -> smsService.verifyCode(verifyVO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("验证码尝试次数超限");
    }

    @Test
    @DisplayName("验证验证码 - 错误应增加尝试次数")
    void verifyCode_incorrect_shouldIncrementAttemptCount() {
        // Given
        SmsVerificationCodeVerifyVO verifyVO = new SmsVerificationCodeVerifyVO();
        verifyVO.setPhoneNumber("13800138000");
        verifyVO.setBizType("login");
        verifyVO.setCode("999999"); // 错误的验证码

        when(verificationCodeRepository.findTopByPhoneNumberAndBizTypeOrderByCreatedAtDesc("13800138000", "login"))
                .thenReturn(Optional.of(mockCode));

        // When & Then
        assertThatThrownBy(() -> smsService.verifyCode(verifyVO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("验证码错误");

        verify(verificationCodeRepository).incrementAttemptCount(1L);
    }

    @Test
    @DisplayName("获取最新验证码 - 存在应返回VO")
    void getLatestVerificationCode_exists_shouldReturnVO() {
        // Given
        when(verificationCodeRepository.findTopByPhoneNumberAndBizTypeOrderByCreatedAtDesc("13800138000", "login"))
                .thenReturn(Optional.of(mockCode));

        // When
        SmsVerificationCodeVO result = smsService.getLatestVerificationCode("13800138000", "login");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPhoneNumber()).isEqualTo("13800138000");
    }

    @Test
    @DisplayName("获取最新验证码 - 不存在应返回null")
    void getLatestVerificationCode_notExists_shouldReturnNull() {
        // Given
        when(verificationCodeRepository.findTopByPhoneNumberAndBizTypeOrderByCreatedAtDesc("13800138000", "login"))
                .thenReturn(Optional.empty());

        // When
        SmsVerificationCodeVO result = smsService.getLatestVerificationCode("13800138000", "login");

        // Then
        assertThat(result).isNull();
    }
}
