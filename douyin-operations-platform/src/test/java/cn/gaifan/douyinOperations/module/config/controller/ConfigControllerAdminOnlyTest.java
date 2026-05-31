package cn.gaifan.douyinOperations.module.config.controller;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.module.config.service.ConfigService;
import cn.gaifan.douyinOperations.module.config.vo.ConfigSearchVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
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
 * <p>使用 @WebMvcTest 只加载 Web 层，避免加载完整的 Spring 上下文
 *
 * <p><b>DISABLED</b>: @WebMvcTest 无法完全隔离 Filter 依赖链（AuthTokenFilter → AuthPermissionService → DataScopeService → ...）
 * <p>TODO: 重构为集成测试或使用 @AutoConfigureMockMvc(addFilters = false)
 */
@Disabled("@WebMvcTest 依赖链过于复杂，需重构为集成测试")
@WebMvcTest(
    controllers = ConfigController.class,
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
    },
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Repository.class),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "cn\\.gaifan\\.douyinOperations\\.common\\.aspect\\..*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "cn\\.gaifan\\.douyinOperations\\.common\\.filter\\..*")
    }
)
class ConfigControllerAdminOnlyTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConfigService configService;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

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
