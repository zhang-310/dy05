package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptVersionService;
import cn.gaifan.douyinOperations.module.live.vo.*;
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

import java.sql.Timestamp;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LiveScriptVersionController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("LiveScriptVersionController 集成测试")
class LiveScriptVersionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LiveScriptVersionService liveScriptVersionService;

    @Test
    @DisplayName("分页查询版本 - 应返回 200")
    void search_shouldReturn200() throws Exception {
        LiveScriptVersionSearchVO searchVO = new LiveScriptVersionSearchVO();
        searchVO.setScriptId(100L);

        LiveScriptVersionVO versionVO = new LiveScriptVersionVO();
        versionVO.setId(1L);
        versionVO.setScriptId(100L);
        versionVO.setVersionNo(1);
        versionVO.setVersionLabel("v1.0");
        versionVO.setScriptContent("测试话术内容");
        versionVO.setVersionStatus("ACTIVE");

        PageResultVO<LiveScriptVersionVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(versionVO));

        when(liveScriptVersionService.search(any(LiveScriptVersionSearchVO.class)))
                .thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/live/script/version/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].versionLabel").value("v1.0"));
    }

    @Test
    @DisplayName("获取版本详情 - 应返回 200")
    void getById_shouldReturn200() throws Exception {
        LiveScriptVersionVO versionVO = new LiveScriptVersionVO();
        versionVO.setId(1L);
        versionVO.setScriptId(100L);
        versionVO.setVersionNo(1);
        versionVO.setScriptContent("测试话术内容");

        when(liveScriptVersionService.getById(eq(1L))).thenReturn(versionVO);

        mockMvc.perform(post("/api/v1/live/script/version/get")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("保存版本 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        LiveScriptVersionSaveVO saveVO = new LiveScriptVersionSaveVO();
        saveVO.setScriptId(100L);
        saveVO.setVersionLabel("v1.0");
        saveVO.setScriptContent("新版本话术");

        when(liveScriptVersionService.save(any(LiveScriptVersionSaveVO.class)))
                .thenReturn(1L);

        mockMvc.perform(post("/api/v1/live/script/version/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("删除版本 - 应返回 200")
    void delete_shouldReturn200() throws Exception {
        doNothing().when(liveScriptVersionService).delete(eq(1L));

        mockMvc.perform(post("/api/v1/live/script/version/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("按脚本ID获取版本列表 - 应返回 200")
    void getByScriptId_shouldReturn200() throws Exception {
        LiveScriptVersionVO version1 = new LiveScriptVersionVO();
        version1.setId(1L);
        version1.setScriptId(100L);
        version1.setVersionNo(1);

        LiveScriptVersionVO version2 = new LiveScriptVersionVO();
        version2.setId(2L);
        version2.setScriptId(100L);
        version2.setVersionNo(2);

        when(liveScriptVersionService.getVersionsByScriptId(eq(100L)))
                .thenReturn(List.of(version1, version2));

        mockMvc.perform(post("/api/v1/live/script/version/getByScriptId")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].versionNo").value(1))
                .andExpect(jsonPath("$.data[1].versionNo").value(2));
    }

    @Test
    @DisplayName("获取最新版本 - 应返回 200")
    void getLatestVersion_shouldReturn200() throws Exception {
        LiveScriptVersionVO versionVO = new LiveScriptVersionVO();
        versionVO.setId(2L);
        versionVO.setScriptId(100L);
        versionVO.setVersionNo(2);
        versionVO.setVersionLabel("v2.0");

        when(liveScriptVersionService.getLatestVersion(eq(100L))).thenReturn(versionVO);

        mockMvc.perform(post("/api/v1/live/script/version/getLatestVersion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.versionNo").value(2));
    }

    @Test
    @DisplayName("版本对比 - 应返回 200")
    void diffVersions_shouldReturn200() throws Exception {
        VersionDiffRequestVO requestVO = new VersionDiffRequestVO();
        requestVO.setOldVersionId(1L);
        requestVO.setNewVersionId(2L);

        VersionDiffVO diffVO = new VersionDiffVO();
        diffVO.setOldVersionId(1L);
        diffVO.setNewVersionId(2L);
        diffVO.setDiffHtml("<div>差异内容</div>");

        when(liveScriptVersionService.diffVersions(eq(1L), eq(2L))).thenReturn(diffVO);

        mockMvc.perform(post("/api/v1/live/script/version/diff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.oldVersionId").value(1))
                .andExpect(jsonPath("$.data.newVersionId").value(2));
    }

    @Test
    @DisplayName("设置推荐版本 - 应返回 200")
    void setRecommended_shouldReturn200() throws Exception {
        SetRecommendedVO vo = new SetRecommendedVO();
        vo.setVersionId(1L);
        vo.setRecommendReason("效果优秀");

        doNothing().when(liveScriptVersionService).setRecommended(eq(1L), eq("效果优秀"));

        mockMvc.perform(post("/api/v1/live/script/version/setRecommended")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("取消推荐版本 - 应返回 200")
    void cancelRecommended_shouldReturn200() throws Exception {
        doNothing().when(liveScriptVersionService).cancelRecommended(eq(1L));

        mockMvc.perform(post("/api/v1/live/script/version/cancelRecommended")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("获取推荐版本列表 - 应返回 200")
    void getRecommendedVersions_shouldReturn200() throws Exception {
        LiveScriptVersionVO versionVO = new LiveScriptVersionVO();
        versionVO.setId(1L);
        versionVO.setScriptId(100L);
        versionVO.setIsRecommended(1);
        versionVO.setRecommendReason("效果优秀");

        when(liveScriptVersionService.getRecommendedVersions(eq(100L)))
                .thenReturn(List.of(versionVO));

        mockMvc.perform(post("/api/v1/live/script/version/getRecommendedVersions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].isRecommended").value(1));
    }

    @Test
    @DisplayName("智能推荐版本 - 应返回 200")
    void recommendVersions_shouldReturn200() throws Exception {
        LiveScriptVersionVO versionVO = new LiveScriptVersionVO();
        versionVO.setId(1L);
        versionVO.setScriptId(100L);
        versionVO.setRecommendScore(0.95);

        when(liveScriptVersionService.recommendVersions(eq(100L)))
                .thenReturn(List.of(versionVO));

        mockMvc.perform(post("/api/v1/live/script/version/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].recommendScore").value(0.95));
    }

    @Test
    @DisplayName("更新版本状态 - 应返回 200")
    void updateVersionStatus_shouldReturn200() throws Exception {
        UpdateVersionStatusVO vo = new UpdateVersionStatusVO();
        vo.setVersionId(1L);
        vo.setVersionStatus("ARCHIVED");

        doNothing().when(liveScriptVersionService).updateVersionStatus(eq(1L), eq("ARCHIVED"));

        mockMvc.perform(post("/api/v1/live/script/version/updateStatus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("增加使用次数 - 应返回 200")
    void incrementUsageCount_shouldReturn200() throws Exception {
        doNothing().when(liveScriptVersionService).incrementUsageCount(eq(1L));

        mockMvc.perform(post("/api/v1/live/script/version/incrementUsageCount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("点赞版本 - 应返回 200")
    void incrementLikedCount_shouldReturn200() throws Exception {
        doNothing().when(liveScriptVersionService).incrementLikedCount(eq(1L));

        mockMvc.perform(post("/api/v1/live/script/version/like")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("批量获取最新版本 - 应返回 200")
    void getLatestByScriptIds_shouldReturn200() throws Exception {
        LiveScriptVersionVO version1 = new LiveScriptVersionVO();
        version1.setId(1L);
        version1.setScriptId(100L);

        LiveScriptVersionVO version2 = new LiveScriptVersionVO();
        version2.setId(2L);
        version2.setScriptId(101L);

        when(liveScriptVersionService.getLatestVersionsByScriptIds(anyList()))
                .thenReturn(List.of(version1, version2));

        mockMvc.perform(post("/api/v1/live/script/version/getLatestByScriptIds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[100, 101]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data[0].scriptId").value(100))
                .andExpect(jsonPath("$.data[1].scriptId").value(101));
    }
}
