package cn.gaifan.douyinOperations.module.drama.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.drama.entity.DramaProject;
import cn.gaifan.douyinOperations.module.drama.repository.DramaProjectRepository;
import cn.gaifan.douyinOperations.module.drama.vo.*;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DramaServiceImplTest {

    @Mock
    private DramaProjectRepository repo;

    @InjectMocks
    private DramaServiceImpl service;

    private DramaProject draftProject;
    private DramaProject planningProject;
    private DramaProject doneProject;
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        draftProject = new DramaProject();
        draftProject.setId(1L);
        draftProject.setUserId(userId);
        draftProject.setTitle("测试短剧");
        draftProject.setDescription("A test drama");
        draftProject.setGenre("romance");
        draftProject.setStatus("draft");
        draftProject.setEpisodeCount(10);
        draftProject.setVisibility("private");
        draftProject.setCostCredits(100L);
        draftProject.setCreateTime(new Timestamp(System.currentTimeMillis()));

        planningProject = new DramaProject();
        planningProject.setId(2L);
        planningProject.setUserId(userId);
        planningProject.setTitle("规划中短剧");
        planningProject.setGenre("fantasy");
        planningProject.setStatus("planning");
        planningProject.setEpisodeCount(20);
        planningProject.setVisibility("private");
        planningProject.setCostCredits(200L);
        planningProject.setCreateTime(new Timestamp(System.currentTimeMillis()));

        doneProject = new DramaProject();
        doneProject.setId(3L);
        doneProject.setUserId(userId);
        doneProject.setTitle("已完成短剧");
        doneProject.setGenre("comedy");
        doneProject.setStatus("done");
        doneProject.setEpisodeCount(12);
        doneProject.setVisibility("public");
        doneProject.setCostCredits(150L);
        doneProject.setCreateTime(new Timestamp(System.currentTimeMillis()));
    }

    @Test
    void overview_shouldReturnProjectCountByStatus() {
        when(repo.findByUserIdAndDeletedOrderByCreateTimeDesc(userId, 0))
                .thenReturn(List.of(draftProject, planningProject, doneProject, doneProject));

        Map<String, Object> result = service.overview(userId);

        assertThat(result.get("productCode")).isEqualTo("drama-ai");
        assertThat(result.get("projectCount")).isEqualTo(4);
        @SuppressWarnings("unchecked")
        Map<String, Long> byStatus = (Map<String, Long>) result.get("byStatus");
        assertThat(byStatus).containsEntry("draft", 1L);
        assertThat(byStatus).containsEntry("planning", 1L);
        assertThat(byStatus).containsEntry("done", 2L);
    }

    @Test
    void search_shouldReturnPaginatedResults() {
        DramaSearchVO searchVO = new DramaSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);
        searchVO.setSortName("createTime");
        searchVO.setSortOrder("desc");
        searchVO.validateParams();

        List<DramaProject> projects = List.of(doneProject, draftProject);
        Page<DramaProject> page = new PageImpl<>(projects);
        when(repo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResultVO<?> result = service.search(searchVO, userId);

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getList()).hasSize(2);
    }

    @Test
    void createProject_shouldCreateAndReturnId() {
        DramaSaveVO saveVO = new DramaSaveVO();
        saveVO.setTitle("新短剧");
        saveVO.setDescription("A new drama");
        saveVO.setGenre("thriller");
        saveVO.setEpisodeCount(8);

        when(repo.save(any(DramaProject.class))).thenAnswer(inv -> {
            DramaProject p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });

        Long id = service.createProject(saveVO, userId);

        assertThat(id).isEqualTo(100L);
        verify(repo).save(argThat(p ->
                "新短剧".equals(p.getTitle()) &&
                "draft".equals(p.getStatus()) &&
                "thriller".equals(p.getGenre())));
    }

    @Test
    void getDetail_shouldReturnProjectForOwner() {
        when(repo.findById(1L)).thenReturn(Optional.of(draftProject));

        DramaProjectVO result = service.getDetail(1L, userId);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("测试短剧");
        assertThat(result.getStatus()).isEqualTo("draft");
        assertThat(result.getGenre()).isEqualTo("romance");
    }

    @Test
    void getDetail_shouldThrowForNonExistentProject() {
        when(repo.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(999L, userId))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.DATA_NOT_FOUND);
    }

    @Test
    void update_shouldModifyFields() {
        when(repo.findById(1L)).thenReturn(Optional.of(draftProject));
        when(repo.save(any(DramaProject.class))).thenAnswer(inv -> inv.getArgument(0));

        DramaUpdateVO updateVO = new DramaUpdateVO();
        updateVO.setId(1L);
        updateVO.setTitle("更新的标题");
        updateVO.setDescription("Updated description");
        updateVO.setGenre("sci-fi");

        Long id = service.update(updateVO, userId);

        assertThat(id).isEqualTo(1L);
        verify(repo).save(argThat(p ->
                "更新的标题".equals(p.getTitle()) &&
                "sci-fi".equals(p.getGenre())));
    }

    @Test
    void delete_shouldSoftDelete() {
        when(repo.findById(1L)).thenReturn(Optional.of(draftProject));

        service.delete(1L, userId);

        verify(repo).save(argThat(p -> p.getDeleted() == 1));
    }

    @Test
    void changeStatus_shouldAdvanceToNextState() {
        when(repo.findById(1L)).thenReturn(Optional.of(draftProject));
        when(repo.save(any(DramaProject.class))).thenAnswer(inv -> inv.getArgument(0));

        service.changeStatus(1L, "planning", userId);

        verify(repo).save(argThat(p -> "planning".equals(p.getStatus())));
    }

    @Test
    void changeStatus_shouldRejectBackwardTransition() {
        when(repo.findById(2L)).thenReturn(Optional.of(planningProject));

        assertThatThrownBy(() -> service.changeStatus(2L, "draft", userId))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.OPERATION_NOT_ALLOWED);
    }

    @Test
    void changeStatus_shouldRejectInvalidStatus() {
        // Validation happens before any repo call, so no stubbing needed
        assertThatThrownBy(() -> service.changeStatus(1L, "invalid_status", userId))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.OPERATION_NOT_ALLOWED);
    }

    @Test
    void changeStatus_shouldAllowSameStatus() {
        when(repo.findById(1L)).thenReturn(Optional.of(draftProject));
        when(repo.save(any(DramaProject.class))).thenAnswer(inv -> inv.getArgument(0));

        service.changeStatus(1L, "draft", userId);

        verify(repo).save(argThat(p -> "draft".equals(p.getStatus())));
    }
}
