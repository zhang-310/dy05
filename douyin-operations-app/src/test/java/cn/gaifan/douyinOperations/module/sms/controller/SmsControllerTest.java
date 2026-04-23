package cn.gaifan.douyinOperations.module.sms.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.sms.service.SmsService;
import cn.gaifan.douyinOperations.module.sms.vo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("SmsController 集成测试")
class SmsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SmsService smsService;

    // ==================== 服务商配置 ====================

    @Test
    @DisplayName("服务商列表 - 应返回 200")
    void providerList_shouldReturn200() throws Exception {
        SmsProviderConfigSearchVO searchVO = new SmsProviderConfigSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);

        SmsProviderConfigVO configVO = new SmsProviderConfigVO();
        configVO.setId(1L);
        configVO.setProviderName("阿里云");

        PageResultVO<SmsProviderConfigVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(configVO));

        when(smsService.searchProviderConfigs(any(SmsProviderConfigSearchVO.class)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/sms/provider/list")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("服务商列表（未登录）- 应返回 2001")
    void providerList_unauthorized_shouldReturn2001() throws Exception {
        SmsProviderConfigSearchVO searchVO = new SmsProviderConfigSearchVO();

        mockMvc.perform(post("/api/v1/sms/provider/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("服务商配置详情 - 应返回 200")
    void providerGet_shouldReturn200() throws Exception {
        SmsProviderConfigVO configVO = new SmsProviderConfigVO();
        configVO.setId(1L);
        configVO.setProviderName("阿里云");

        when(smsService.getProviderConfigById(eq(1L)))
                .thenReturn(configVO);

        mockMvc.perform(post("/api/v1/sms/provider/get")
                        .requestAttr("userId", 1L)
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.providerName").value("阿里云"));
    }

    @Test
    @DisplayName("服务商配置详情（未登录）- 应返回 2001")
    void providerGet_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/sms/provider/get")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("新增/更新服务商配置 - 应返回 200")
    void providerSave_shouldReturn200() throws Exception {
        SmsProviderConfigSaveVO saveVO = new SmsProviderConfigSaveVO();
        saveVO.setOwnerId(1L);
        saveVO.setProviderName("阿里云");
        saveVO.setProviderCode("aliyun");
        saveVO.setApiKey("test-key");
        saveVO.setApiSecret("test-secret");

        when(smsService.saveProviderConfig(any(SmsProviderConfigSaveVO.class)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/sms/provider/save")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("新增/更新服务商配置（未登录）- 应返回 1001")
    void providerSave_unauthorized_shouldReturn1001() throws Exception {
        SmsProviderConfigSaveVO saveVO = new SmsProviderConfigSaveVO();
        saveVO.setProviderName("阿里云");

        mockMvc.perform(post("/api/v1/sms/provider/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("删除服务商配置 - 应返回 204")
    void providerDelete_shouldReturn204() throws Exception {
        doNothing().when(smsService).deleteProviderConfig(eq(1L));

        mockMvc.perform(post("/api/v1/sms/provider/delete")
                        .requestAttr("userId", 1L)
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除服务商配置（未登录）- 应返回 2001")
    void providerDelete_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/sms/provider/delete")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("启用/禁用服务商配置 - 应返回 204")
    void providerStatus_shouldReturn204() throws Exception {
        doNothing().when(smsService).updateProviderConfigStatus(eq(1L), eq(1));

        mockMvc.perform(post("/api/v1/sms/provider/update-status")
                        .requestAttr("userId", 1L)
                        .param("id", "1")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("启用/禁用服务商配置（未登录）- 应返回 2001")
    void providerStatus_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/sms/provider/update-status")
                        .param("id", "1")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("设置为默认服务商 - 应返回 204")
    void setDefaultProvider_shouldReturn204() throws Exception {
        doNothing().when(smsService).setDefaultProviderConfig(eq(1L));

        mockMvc.perform(post("/api/v1/sms/provider/set-default")
                        .requestAttr("userId", 1L)
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("设置为默认服务商（未登录）- 应返回 2001")
    void setDefaultProvider_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/sms/provider/set-default")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    // ==================== 短信模板 ====================

    @Test
    @DisplayName("模板列表 - 应返回 200")
    void templateList_shouldReturn200() throws Exception {
        SmsTemplateSearchVO searchVO = new SmsTemplateSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);

        SmsTemplateVO templateVO = new SmsTemplateVO();
        templateVO.setId(1L);
        templateVO.setTemplateName("验证码模板");

        PageResultVO<SmsTemplateVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(templateVO));

        when(smsService.searchTemplates(any(SmsTemplateSearchVO.class)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/sms/template/list")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("模板列表（未登录）- 应返回 2001")
    void templateList_unauthorized_shouldReturn2001() throws Exception {
        SmsTemplateSearchVO searchVO = new SmsTemplateSearchVO();

        mockMvc.perform(post("/api/v1/sms/template/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("模板详情 - 应返回 200")
    void templateGet_shouldReturn200() throws Exception {
        SmsTemplateVO templateVO = new SmsTemplateVO();
        templateVO.setId(1L);
        templateVO.setTemplateName("验证码模板");

        when(smsService.getTemplateById(eq(1L)))
                .thenReturn(templateVO);

        mockMvc.perform(post("/api/v1/sms/template/get")
                        .requestAttr("userId", 1L)
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.templateName").value("验证码模板"));
    }

    @Test
    @DisplayName("模板详情（未登录）- 应返回 2001")
    void templateGet_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/sms/template/get")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("新增/更新模板 - 应返回 200")
    void templateSave_shouldReturn200() throws Exception {
        SmsTemplateSaveVO saveVO = new SmsTemplateSaveVO();
        saveVO.setOwnerId(1L);
        saveVO.setTemplateName("验证码模板");
        saveVO.setTemplateCode("SMS_123456");
        saveVO.setContent("您的验证码是${code}");
        saveVO.setProviderCode("aliyun");

        when(smsService.saveTemplate(any(SmsTemplateSaveVO.class)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/sms/template/save")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("新增/更新模板（未登录）- 应返回 1001")
    void templateSave_unauthorized_shouldReturn1001() throws Exception {
        SmsTemplateSaveVO saveVO = new SmsTemplateSaveVO();
        saveVO.setTemplateName("验证码模板");

        mockMvc.perform(post("/api/v1/sms/template/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("删除模板 - 应返回 204")
    void templateDelete_shouldReturn204() throws Exception {
        doNothing().when(smsService).deleteTemplate(eq(1L));

        mockMvc.perform(post("/api/v1/sms/template/delete")
                        .requestAttr("userId", 1L)
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("删除模板（未登录）- 应返回 2001")
    void templateDelete_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/sms/template/delete")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("启用/禁用模板 - 应返回 204")
    void templateStatus_shouldReturn204() throws Exception {
        doNothing().when(smsService).updateTemplateStatus(eq(1L), eq(1));

        mockMvc.perform(post("/api/v1/sms/template/update-status")
                        .requestAttr("userId", 1L)
                        .param("id", "1")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("启用/禁用模板（未登录）- 应返回 2001")
    void templateStatus_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/sms/template/update-status")
                        .param("id", "1")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    // ==================== 短信发送日志 ====================

    @Test
    @DisplayName("发送日志列表 - 应返回 200")
    void logList_shouldReturn200() throws Exception {
        SmsSendLogSearchVO searchVO = new SmsSendLogSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);

        SmsSendLogVO logVO = new SmsSendLogVO();
        logVO.setId(1L);
        logVO.setPhoneNumber("13800138000");

        PageResultVO<SmsSendLogVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(logVO));

        when(smsService.searchSendLogs(any(SmsSendLogSearchVO.class)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/sms/log/list")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("发送日志列表（未登录）- 应返回 2001")
    void logList_unauthorized_shouldReturn2001() throws Exception {
        SmsSendLogSearchVO searchVO = new SmsSendLogSearchVO();

        mockMvc.perform(post("/api/v1/sms/log/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("发送日志详情 - 应返回 200")
    void logGet_shouldReturn200() throws Exception {
        SmsSendLogVO logVO = new SmsSendLogVO();
        logVO.setId(1L);
        logVO.setPhoneNumber("13800138000");

        when(smsService.getSendLogById(eq(1L)))
                .thenReturn(logVO);

        mockMvc.perform(post("/api/v1/sms/log/get")
                        .requestAttr("userId", 1L)
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.phoneNumber").value("13800138000"));
    }

    @Test
    @DisplayName("发送日志详情（未登录）- 应返回 2001")
    void logGet_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/sms/log/get")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    // ==================== 短信验证码 ====================

    @Test
    @DisplayName("发送验证码 - 应返回 204")
    void sendVerificationCode_shouldReturn204() throws Exception {
        SmsVerificationCodeSendVO sendVO = new SmsVerificationCodeSendVO();
        sendVO.setOwnerId(1L);
        sendVO.setPhoneNumber("13800138000");
        sendVO.setBizType("login");

        doNothing().when(smsService).sendVerificationCode(any(SmsVerificationCodeSendVO.class));

        mockMvc.perform(post("/api/v1/sms/code/send")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("发送验证码（未登录）- 应返回 1001")
    void sendVerificationCode_unauthorized_shouldReturn1001() throws Exception {
        SmsVerificationCodeSendVO sendVO = new SmsVerificationCodeSendVO();
        sendVO.setPhoneNumber("13800138000");

        mockMvc.perform(post("/api/v1/sms/code/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("验证码验证 - 应返回 204")
    void verifyCode_shouldReturn204() throws Exception {
        SmsVerificationCodeVerifyVO verifyVO = new SmsVerificationCodeVerifyVO();
        verifyVO.setOwnerId(1L);
        verifyVO.setPhoneNumber("13800138000");
        verifyVO.setBizType("login");
        verifyVO.setCode("123456");

        doNothing().when(smsService).verifyCode(any(SmsVerificationCodeVerifyVO.class));

        mockMvc.perform(post("/api/v1/sms/code/verify")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("验证码验证（未登录）- 应返回 1001")
    void verifyCode_unauthorized_shouldReturn1001() throws Exception {
        SmsVerificationCodeVerifyVO verifyVO = new SmsVerificationCodeVerifyVO();
        verifyVO.setPhoneNumber("13800138000");

        mockMvc.perform(post("/api/v1/sms/code/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }

    @Test
    @DisplayName("获取最新验证码信息 - 应返回 200")
    void getLatestCode_shouldReturn200() throws Exception {
        SmsVerificationCodeVO codeVO = new SmsVerificationCodeVO();
        codeVO.setId(1L);
        codeVO.setPhoneNumber("13800138000");

        when(smsService.getLatestVerificationCode(eq("13800138000"), eq("login")))
                .thenReturn(codeVO);

        mockMvc.perform(post("/api/v1/sms/code/get-latest")
                        .requestAttr("userId", 1L)
                        .param("phoneNumber", "13800138000")
                        .param("bizType", "login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.phoneNumber").value("13800138000"));
    }

    @Test
    @DisplayName("获取最新验证码信息（未登录）- 应返回 2001")
    void getLatestCode_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/sms/code/get-latest")
                        .param("phoneNumber", "13800138000")
                        .param("bizType", "login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
