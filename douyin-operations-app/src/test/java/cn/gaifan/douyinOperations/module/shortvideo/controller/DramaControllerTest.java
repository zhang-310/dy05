package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDrama;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDramaCharacter;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDramaEpisode;
import cn.gaifan.douyinOperations.module.shortvideo.service.DramaService;
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
 * DramaController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("DramaController 集成测试")
class DramaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DramaService dramaService;

    @Test
    @DisplayName("短剧列表 - 应返回 200")
    void list_shouldReturn200() throws Exception {
        SvDrama drama = new SvDrama();
        drama.setId(1L);
        drama.setTitle("测试短剧");

        when(dramaService.listDramas(eq(1L))).thenReturn(List.of(drama));

        mockMvc.perform(post("/api/v1/short-video/drama/list")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @DisplayName("短剧详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        SvDrama drama = new SvDrama();
        drama.setId(1L);
        drama.setTitle("测试短剧");

        when(dramaService.getDrama(eq(1L), eq(1L))).thenReturn(drama);

        mockMvc.perform(post("/api/v1/short-video/drama/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("创建短剧 - 应返回 200")
    void create_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "新短剧");
        body.put("description", "描述");
        body.put("genre", "romance");
        body.put("totalEpisodes", 10);

        SvDrama drama = new SvDrama();
        drama.setId(1L);
        drama.setTitle("新短剧");

        when(dramaService.createDrama(eq(1L), eq("新短剧"), eq("描述"), eq("romance"), eq(10)))
                .thenReturn(drama);

        mockMvc.perform(post("/api/v1/short-video/drama/create")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("删除短剧 - 应返回 204")
    void delete_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        doNothing().when(dramaService).deleteDrama(eq(1L), eq(1L));

        mockMvc.perform(post("/api/v1/short-video/drama/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("剧集列表 - 应返回 200")
    void episodes_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("dramaId", 1L);

        SvDramaEpisode episode = new SvDramaEpisode();
        episode.setId(1L);
        episode.setEpisodeNumber(1);

        when(dramaService.listEpisodes(eq(1L), eq(1L))).thenReturn(List.of(episode));

        mockMvc.perform(post("/api/v1/short-video/drama/episodes")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("添加剧集 - 应返回 200")
    void addEpisode_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("dramaId", 1L);
        body.put("episodeNumber", 1);
        body.put("title", "第一集");

        SvDramaEpisode episode = new SvDramaEpisode();
        episode.setId(1L);
        episode.setEpisodeNumber(1);

        when(dramaService.addEpisode(eq(1L), eq(1L), eq(1), eq("第一集"), isNull(), isNull()))
                .thenReturn(episode);

        mockMvc.perform(post("/api/v1/short-video/drama/add-episode")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("角色列表 - 应返回 200")
    void characters_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("dramaId", 1L);

        SvDramaCharacter character = new SvDramaCharacter();
        character.setId(1L);
        character.setCharacterName("主角");

        when(dramaService.listCharacters(eq(1L), eq(1L))).thenReturn(List.of(character));

        mockMvc.perform(post("/api/v1/short-video/drama/characters")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("添加角色 - 应返回 200")
    void addCharacter_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("dramaId", 1L);
        body.put("name", "主角");
        body.put("description", "主角描述");

        SvDramaCharacter character = new SvDramaCharacter();
        character.setId(1L);
        character.setCharacterName("主角");

        when(dramaService.addCharacter(eq(1L), eq(1L), eq("主角"), eq("主角描述"), isNull(), isNull()))
                .thenReturn(character);

        mockMvc.perform(post("/api/v1/short-video/drama/add-character")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("短剧列表（未登录）- 应返回 2001")
    void list_unauthorized_shouldReturn2001() throws Exception {
        mockMvc.perform(post("/api/v1/short-video/drama/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }

    @Test
    @DisplayName("短剧详情（缺少 id）- 应返回 1001")
    void get_missingId_shouldReturn1001() throws Exception {
        Map<String, Object> body = new HashMap<>();

        mockMvc.perform(post("/api/v1/short-video/drama/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1001));
    }
}
