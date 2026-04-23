package cn.gaifan.douyinOperations.module.script.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.script.service.UserViolationWordService;
import cn.gaifan.douyinOperations.module.script.vo.UserViolationWordVO;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("UserViolationWordController 集成测试")
class UserViolationWordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserViolationWordService userViolationWordService;

    @Test
    @DisplayName("查询用户违规词 - 应返回 200")
    void search_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("rows", 10);

        PageResultVO<UserViolationWordVO> result = new PageResultVO<>();
        result.setTotal(1L);
        result.setList(List.of());

        when(userViolationWordService.search(any()))
                .thenReturn(result);

        mockMvc.perform(post("/api/v1/script/user-violation/search")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("查询用户违规词（未登录）- 应返回 2001")
    void search_unauthorized_shouldReturn2001() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("page", 0);
        body.put("rows", 10);

        mockMvc.perform(post("/api/v1/script/user-violation/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("获取违规词详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        UserViolationWordVO word = new UserViolationWordVO();
        word.setId(1L);
        word.setWord("测试违规词");

        when(userViolationWordService.getById(anyLong()))
                .thenReturn(word);

        mockMvc.perform(post("/api/v1/script/user-violation/get")
                        .requestAttr("userId", 1L)
                        .param("id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("保存用户违规词 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", 1L);
        body.put("word", "测试违规词");
        body.put("severity", "high");

        when(userViolationWordService.save(any()))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/script/user-violation/save")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("删除用户违规词 - 应返回 204")
    void delete_shouldReturn204() throws Exception {
        mockMvc.perform(post("/api/v1/script/user-violation/delete")
                        .requestAttr("userId", 1L)
                        .param("id", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("获取有效违规词 - 应返回 200")
    void listActive_shouldReturn200() throws Exception {
        UserViolationWordVO word = new UserViolationWordVO();
        word.setId(1L);

        when(userViolationWordService.listActiveByUserId(anyLong()))
                .thenReturn(List.of(word));

        mockMvc.perform(post("/api/v1/script/user-violation/active")
                        .requestAttr("userId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }
}
