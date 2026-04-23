package cn.gaifan.douyinOperations.module.copy.controller;

import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.copy.service.CopyApprovalService;
import cn.gaifan.douyinOperations.module.copy.vo.CopyApprovalSaveVO;
import cn.gaifan.douyinOperations.module.copy.vo.CopyApprovalSearchVO;
import cn.gaifan.douyinOperations.module.copy.vo.CopyApprovalVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
 * CopyApprovalController 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("CopyApprovalController 集成测试")
class CopyApprovalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CopyApprovalService copyApprovalService;

    @MockBean
    private DataScopeResolver dataScopeService;

    @BeforeEach
    void setUp() {
        when(dataScopeService.getVisibleUserIds(anyLong(), anyString())).thenReturn(List.of(1L));
    }

    @Test
    @DisplayName("分页搜索审批记录 - 应返回 200")
    void search_shouldReturn200() throws Exception {
        CopyApprovalSearchVO searchVO = new CopyApprovalSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);
        searchVO.setApprovalStatus(1);

        CopyApprovalVO approvalVO = new CopyApprovalVO();
        approvalVO.setId(1L);
        approvalVO.setCopyId(10L);
        approvalVO.setUserId(1L);
        approvalVO.setApprovalStatus(1);
        approvalVO.setComments("审批通过");
        approvalVO.setCopyTitle("测试文案");
        approvalVO.setCopyContent("文案内容");
        approvalVO.setSubmitterId(2L);
        approvalVO.setApprovalTime(new Timestamp(System.currentTimeMillis()));
        approvalVO.setCreateTime(new Timestamp(System.currentTimeMillis()));
        approvalVO.setUpdateTime(new Timestamp(System.currentTimeMillis()));

        PageResultVO<CopyApprovalVO> pageResult = new PageResultVO<>();
        pageResult.setTotal(1L);
        pageResult.setList(List.of(approvalVO));

        when(copyApprovalService.search(any(CopyApprovalSearchVO.class))).thenReturn(pageResult);

        mockMvc.perform(post("/api/v1/copy/approval/search")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].approvalStatus").value(1));
    }

    @Test
    @DisplayName("获取审批详情 - 应返回 200")
    void get_shouldReturn200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        CopyApprovalVO approvalVO = new CopyApprovalVO();
        approvalVO.setId(1L);
        approvalVO.setCopyId(10L);
        approvalVO.setApprovalStatus(1);
        approvalVO.setComments("审批通过");

        when(copyApprovalService.getById(1L)).thenReturn(approvalVO);

        mockMvc.perform(post("/api/v1/copy/approval/get")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.approvalStatus").value(1));
    }

    @Test
    @DisplayName("提交审批 - 应返回 200")
    void save_shouldReturn200() throws Exception {
        CopyApprovalSaveVO saveVO = new CopyApprovalSaveVO();
        saveVO.setCopyId(10L);
        saveVO.setUserId(1L);
        saveVO.setApprovalStatus(2);
        saveVO.setComments("待审批");

        when(copyApprovalService.save(any(CopyApprovalSaveVO.class))).thenReturn(1L);

        mockMvc.perform(post("/api/v1/copy/approval/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("管理员审批通过 - 应返回 200")
    void approveByAdmin_shouldReturn200() throws Exception {
        CopyApprovalSaveVO saveVO = new CopyApprovalSaveVO();
        saveVO.setCopyId(10L);
        saveVO.setUserId(1L);
        saveVO.setApprovalStatus(1);
        saveVO.setComments("审批通过");

        when(copyApprovalService.save(any(CopyApprovalSaveVO.class))).thenReturn(1L);

        mockMvc.perform(post("/api/v1/copy/approval/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    @DisplayName("非管理员审批通过 - 应返回 2002")
    void approveByNonAdmin_shouldReturn2002() throws Exception {
        CopyApprovalSaveVO saveVO = new CopyApprovalSaveVO();
        saveVO.setCopyId(10L);
        saveVO.setUserId(1L);
        saveVO.setApprovalStatus(1);
        saveVO.setComments("审批通过");

        mockMvc.perform(post("/api/v1/copy/approval/save")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2002))
                .andExpect(jsonPath("$.message").value("仅管理员可执行审批通过/拒绝"));
    }

    @Test
    @DisplayName("删除审批记录 - 应返回 204")
    void delete_shouldReturn204() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("id", 1L);

        doNothing().when(copyApprovalService).delete(1L);

        mockMvc.perform(post("/api/v1/copy/approval/delete")
                        .requestAttr("userId", 1L)
                        .requestAttr("roleCode", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("未认证访问 - 应返回 2001")
    void withoutAuth_shouldReturn2001() throws Exception {
        CopyApprovalSearchVO searchVO = new CopyApprovalSearchVO();

        mockMvc.perform(post("/api/v1/copy/approval/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchVO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2001));
    }
}
