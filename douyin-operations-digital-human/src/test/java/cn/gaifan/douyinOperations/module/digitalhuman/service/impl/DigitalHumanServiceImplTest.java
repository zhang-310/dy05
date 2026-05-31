package cn.gaifan.douyinOperations.module.digitalhuman.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.service.DigitalHumanProvider;
import cn.gaifan.douyinOperations.module.digitalhuman.entity.DigitalHumanTask;
import cn.gaifan.douyinOperations.module.digitalhuman.repository.DigitalHumanTaskRepository;
import cn.gaifan.douyinOperations.module.digitalhuman.vo.DigitalHumanSaveVO;
import cn.gaifan.douyinOperations.module.digitalhuman.vo.DigitalHumanSearchVO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DigitalHumanServiceImplTest {

    @Mock
    private DigitalHumanTaskRepository repo;

    @Mock
    private DigitalHumanProvider digitalHumanProvider;

    @InjectMocks
    private DigitalHumanServiceImpl service;

    private DigitalHumanTask pendingTask;
    private DigitalHumanTask completedTask;
    private DigitalHumanTask failedTask;
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        pendingTask = new DigitalHumanTask();
        pendingTask.setId(1L);
        pendingTask.setUserId(userId);
        pendingTask.setScriptContent("Hello world");
        pendingTask.setVoiceType("default");
        pendingTask.setStatus("pending");
        pendingTask.setProgress(0);
        pendingTask.setCostCredits(50L);
        pendingTask.setCreateTime(new Timestamp(System.currentTimeMillis()));

        completedTask = new DigitalHumanTask();
        completedTask.setId(2L);
        completedTask.setUserId(userId);
        completedTask.setScriptContent("Test script");
        completedTask.setVoiceType("zh-CN-XiaoxiaoNeural");
        completedTask.setStatus("completed");
        completedTask.setOutputUrl("https://output.example.com/video.mp4");
        completedTask.setProgress(100);
        completedTask.setCostCredits(50L);
        completedTask.setCreateTime(new Timestamp(System.currentTimeMillis()));

        failedTask = new DigitalHumanTask();
        failedTask.setId(3L);
        failedTask.setUserId(userId);
        failedTask.setScriptContent("Failed script");
        failedTask.setVoiceType("default");
        failedTask.setStatus("failed");
        failedTask.setErrorMessage("Provider error");
        failedTask.setProgress(0);
        failedTask.setCostCredits(50L);
        failedTask.setCreateTime(new Timestamp(System.currentTimeMillis()));
    }

    @Test
    void overview_shouldReturnTaskCountByStatus() {
        when(repo.findByUserIdAndDeletedOrderByCreateTimeDesc(userId, 0))
                .thenReturn(List.of(pendingTask, completedTask, completedTask, failedTask));

        Map<String, Object> result = service.overview(userId);

        assertThat(result.get("productCode")).isEqualTo("digital-human");
        assertThat(result.get("taskCount")).isEqualTo(4);
        @SuppressWarnings("unchecked")
        Map<String, Long> byStatus = (Map<String, Long>) result.get("byStatus");
        assertThat(byStatus).containsEntry("pending", 1L);
        assertThat(byStatus).containsEntry("completed", 2L);
        assertThat(byStatus).containsEntry("failed", 1L);
    }

    @Test
    void search_shouldReturnPaginatedResults() {
        DigitalHumanSearchVO searchVO = new DigitalHumanSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);
        searchVO.setSortName("createTime");
        searchVO.setSortOrder("desc");
        searchVO.validateParams();

        List<DigitalHumanTask> tasks = List.of(completedTask, pendingTask);
        Page<DigitalHumanTask> page = new PageImpl<>(tasks);
        when(repo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResultVO<?> result = service.search(searchVO, userId);

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getList()).hasSize(2);
    }

    @Test
    void createTask_shouldCreateTaskAndCallProvider() {
        DigitalHumanSaveVO saveVO = new DigitalHumanSaveVO();
        saveVO.setScriptContent("Test script content");
        saveVO.setVoiceType("zh-CN-XiaoxiaoNeural");
        saveVO.setAvatarId("avatar-001");

        List<String> stateSequence = new ArrayList<>();
        when(repo.save(any(DigitalHumanTask.class))).thenAnswer(inv -> {
            DigitalHumanTask t = inv.getArgument(0);
            stateSequence.add(t.getStatus());
            if (t.getId() == null) t.setId(100L);
            return t;
        });

        when(digitalHumanProvider.isConfigured()).thenReturn(true);
        when(digitalHumanProvider.generateTalkingHead(
                eq("avatar-001"), eq("Test script content"), eq("zh-CN-XiaoxiaoNeural")))
                .thenReturn("https://output.example.com/dh.mp4");

        Long taskId = service.createTask(saveVO, userId);

        assertThat(taskId).isEqualTo(100L);
        assertThat(stateSequence).containsExactly("pending", "processing", "completed");
    }

    @Test
    void createTask_shouldHandleProviderFailure() {
        DigitalHumanSaveVO saveVO = new DigitalHumanSaveVO();
        saveVO.setScriptContent("Test script");

        when(repo.save(any(DigitalHumanTask.class))).thenAnswer(inv -> {
            DigitalHumanTask t = inv.getArgument(0);
            if (t.getId() == null) t.setId(200L);
            return t;
        });

        when(digitalHumanProvider.isConfigured()).thenReturn(true);
        when(digitalHumanProvider.generateTalkingHead(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("API error"));

        Long taskId = service.createTask(saveVO, userId);

        assertThat(taskId).isEqualTo(200L);
        verify(repo, atLeastOnce()).save(argThat(t ->
                "failed".equals(t.getStatus()) && t.getErrorMessage() != null));
    }

    @Test
    void createTask_shouldSaveAsPendingWhenProviderNotConfigured() {
        DigitalHumanSaveVO saveVO = new DigitalHumanSaveVO();
        saveVO.setScriptContent("Test");

        when(repo.save(any(DigitalHumanTask.class))).thenAnswer(inv -> inv.getArgument(0));

        Long taskId = service.createTask(saveVO, userId);

        assertThat(taskId).isNull();
    }

    @Test
    void getStatus_shouldReturnTaskForOwner() {
        when(repo.findById(1L)).thenReturn(Optional.of(pendingTask));

        var result = service.getStatus(1L, userId);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("pending");
        assertThat(result.getScriptContent()).isEqualTo("Hello world");
    }

    @Test
    void getStatus_shouldThrowForNonExistentTask() {
        when(repo.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStatus(999L, userId))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.DATA_NOT_FOUND);
    }

    @Test
    void delete_shouldSoftDeleteTask() {
        when(repo.findById(1L)).thenReturn(Optional.of(pendingTask));

        service.delete(1L, userId);

        verify(repo).save(argThat(t -> t.getDeleted() == 1));
    }

    @Test
    void delete_shouldThrowForNonExistentTask() {
        when(repo.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(999L, userId))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.DATA_NOT_FOUND);
    }
}
