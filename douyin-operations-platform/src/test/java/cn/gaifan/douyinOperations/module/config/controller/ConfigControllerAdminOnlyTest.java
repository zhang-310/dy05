package cn.gaifan.douyinOperations.module.config.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.config.service.ConfigService;
import cn.gaifan.douyinOperations.module.config.vo.ConfigSearchVO;
import cn.gaifan.douyinOperations.module.config.vo.ConfigVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ConfigController 权限校验测试
 *
 * <p>验证 @AdminOnly 切面是否正确拦截未授权访问
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConfigControllerAdminOnlyTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConfigService configService;

    @BeforeEach
    void setUp() {
        // Mock ConfigService 返回空结果，避免实际数据库操作
        when(configService.search(any(ConfigSearchVO.class)))
                .thenReturn(new cn.gaifan.douyinOperations.common.vo.PageResultVO<>(0L, java.util.Collections.emptyList(), 0, 30));
    }

    @Test
    void testListWithoutLogin_ShouldReturn401() throws Exception {
        // 未登录用户访问 /list 接口
        mockMvc.perform(post("/api/v1/config/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ErrorCode.UNAUTHORIZED))
                .andExpect(jsonPath("$.message").value("未登录"));
    }

    @Test
    void testListWithNonAdminUser_ShouldReturn403() throws Exception {
        // 非管理员用户访问 /list 接口
        mockMvc.perform(post("/api/v1/config/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Authorization", "Bearer mock-token")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")) // 普通用户角色
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ErrorCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("无权限访问"));
    }

    @Test
    void testGetWithoutLogin_ShouldReturn401() throws Exception {
        // 未登录用户访问 /get 接口
        mockMvc.perform(post("/api/v1/config/get")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\":\"test.key\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ErrorCode.UNAUTHORIZED))
                .andExpect(jsonPath("$.message").value("未登录"));
    }

    @Test
    void testSaveWithoutLogin_ShouldReturn401() throws Exception {
        // 未登录用户访问 /save 接口
        mockMvc.perform(post("/api/v1/config/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configKey\":\"test.key\",\"configValue\":\"test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ErrorCode.UNAUTHORIZED))
                .andExpect(jsonPath("$.message").value("未登录"));
    }

    @Test
    void testDeleteWithoutLogin_ShouldReturn401() throws Exception {
        // 未登录用户访问 /delete 接口
        mockMvc.perform(post("/api/v1/config/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ErrorCode.UNAUTHORIZED))
                .andExpect(jsonPath("$.message").value("未登录"));
    }
}
