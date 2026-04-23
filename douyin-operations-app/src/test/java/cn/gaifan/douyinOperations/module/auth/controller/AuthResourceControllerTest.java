package cn.gaifan.douyinOperations.module.auth.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.auth.service.AuthResourceService;
import cn.gaifan.douyinOperations.module.auth.vo.AuthResourceVO;
import cn.gaifan.douyinOperations.module.auth.vo.MenuItemVO;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthResourceController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("AuthResourceController 集成测试")
class AuthResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthResourceService authResourceService;

    @Test
    @DisplayName("资源列表 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("rows", 30);

        PageResultVO<AuthResourceVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(10L);
        pageResult.setList(List.of());

        when(authResourceService.search(any())).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/auth/resource/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(10));
    }

    @Test
    @DisplayName("资源列表（非管理员）- 应返回 2002")
    void list_asUser_shouldReturn2002() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/auth/resource/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002));
    }

    @Test
    @DisplayName("资源列表（未登录）- 应返回 2001")
    void list_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/auth/resource/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("资源树 - 应返回 200")
    void tree_shouldReturn200() throws Exception {
        MenuItemVO menu1 = new MenuItemVO();
        menu1.setId(1L);
        menu1.setResourceName("系统管理");

        when(authResourceService.listMenuTree()).thenReturn(List.of(menu1));

        mockMvc.perform(post("/api/v1/auth/resource/tree")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].resourceName").value("系统管理"));
    }

    @Test
    @DisplayName("资源详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        AuthResourceVO resource = new AuthResourceVO();
        resource.setId(1L);
        resource.setResourceName("用户管理");

        when(authResourceService.getById(eq(1L))).thenReturn(resource);

        mockMvc.perform(post("/api/v1/auth/resource/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.resourceName").value("用户管理"));
    }

    @Test
    @DisplayName("保存资源 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("resourceName", "新资源");
        body.put("resourceCode", "new_resource");
        body.put("resourceType", "menu");

        when(authResourceService.save(any())).thenReturn(1L);

        mockMvc.perform(post("/api/v1/auth/resource/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("删除资源 - 应返回 204")
    void delete_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        doNothing().when(authResourceService).deleteById(eq(1L));

        mockMvc.perform(post("/api/v1/auth/resource/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }
}
