package cn.gaifan.douyinOperations.module.photoavatar.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.service.ImageGenerationService;
import cn.gaifan.douyinOperations.module.ai.service.ImageGenerationService.ImageToImageRequest;
import cn.gaifan.douyinOperations.module.ai.service.ImageGenerationService.ImageResult;
import cn.gaifan.douyinOperations.module.photoavatar.entity.PhotoAvatarTask;
import cn.gaifan.douyinOperations.module.photoavatar.repository.PhotoAvatarTaskRepository;
import cn.gaifan.douyinOperations.module.photoavatar.vo.PhotoAvatarSaveVO;
import cn.gaifan.douyinOperations.module.photoavatar.vo.PhotoAvatarSearchVO;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhotoAvatarServiceImplTest {

    @Mock
    private PhotoAvatarTaskRepository repo;

    @Mock
    private ImageGenerationService imageGenerationService;

    @InjectMocks
    private PhotoAvatarServiceImpl service;

    private PhotoAvatarTask pendingTask;
    private PhotoAvatarTask completedTask;
    private PhotoAvatarTask failedTask;
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        pendingTask = new PhotoAvatarTask();
        pendingTask.setId(1L);
        pendingTask.setUserId(userId);
        pendingTask.setPhotoUrl("https://example.com/photo.jpg");
        pendingTask.setOutfitStyle("casual");
        pendingTask.setBackground("studio");
        pendingTask.setStatus("pending");
        pendingTask.setProgress(0);
        pendingTask.setCostCredits(80L);
        pendingTask.setCreateTime(new Timestamp(System.currentTimeMillis()));

        completedTask = new PhotoAvatarTask();
        completedTask.setId(2L);
        completedTask.setUserId(userId);
        completedTask.setPhotoUrl("https://example.com/photo2.jpg");
        completedTask.setOutfitStyle("formal");
        completedTask.setBackground("outdoor");
        completedTask.setStatus("completed");
        completedTask.setOutputUrl("https://output.example.com/video.mp4");
        completedTask.setProgress(100);
        completedTask.setCostCredits(80L);
        completedTask.setCreateTime(new Timestamp(System.currentTimeMillis()));

        failedTask = new PhotoAvatarTask();
        failedTask.setId(3L);
        failedTask.setUserId(userId);
        failedTask.setPhotoUrl("https://example.com/photo3.jpg");
        failedTask.setOutfitStyle("fantasy");
        failedTask.setBackground("solid");
        failedTask.setStatus("failed");
        failedTask.setErrorMessage("Generation failed");
        failedTask.setProgress(0);
        failedTask.setCostCredits(80L);
        failedTask.setCreateTime(new Timestamp(System.currentTimeMillis()));
    }

    @Test
    void overview_shouldReturnTaskCountByStatus() {
        when(repo.findByUserIdAndDeletedOrderByCreateTimeDesc(userId, 0))
                .thenReturn(List.of(pendingTask, completedTask, completedTask, failedTask));

        Map<String, Object> result = service.overview(userId);

        assertThat(result.get("productCode")).isEqualTo("photo-avatar-video");
        assertThat(result.get("taskCount")).isEqualTo(4);
        @SuppressWarnings("unchecked")
        Map<String, Long> byStatus = (Map<String, Long>) result.get("byStatus");
        assertThat(byStatus).containsEntry("pending", 1L);
        assertThat(byStatus).containsEntry("completed", 2L);
        assertThat(byStatus).containsEntry("failed", 1L);
    }

    @Test
    void overview_shouldReturnZeroForNoTasks() {
        when(repo.findByUserIdAndDeletedOrderByCreateTimeDesc(userId, 0))
                .thenReturn(List.of());

        Map<String, Object> result = service.overview(userId);

        assertThat(result.get("taskCount")).isEqualTo(0);
        @SuppressWarnings("unchecked")
        Map<String, Long> byStatus = (Map<String, Long>) result.get("byStatus");
        assertThat(byStatus).isEmpty();
    }

    @Test
    void search_shouldReturnPaginatedResults() {
        PhotoAvatarSearchVO searchVO = new PhotoAvatarSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);
        searchVO.setSortName("createTime");
        searchVO.setSortOrder("desc");
        searchVO.validateParams();

        List<PhotoAvatarTask> tasks = List.of(completedTask, pendingTask);
        Page<PhotoAvatarTask> page = new PageImpl<>(tasks);
        when(repo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResultVO<?> result = service.search(searchVO, userId);

        assertThat(result.getTotal()).isEqualTo(2);
        assertThat(result.getList()).hasSize(2);
    }

    @Test
    void search_shouldFilterByStatus() {
        PhotoAvatarSearchVO searchVO = new PhotoAvatarSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);
        searchVO.setStatus("failed");
        searchVO.validateParams();

        List<PhotoAvatarTask> tasks = List.of(failedTask);
        Page<PhotoAvatarTask> page = new PageImpl<>(tasks);
        when(repo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResultVO<?> result = service.search(searchVO, userId);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
    }

    @Test
    void createVideo_shouldCreateTaskAndCallImageGenerationService() {
        PhotoAvatarSaveVO saveVO = new PhotoAvatarSaveVO();
        saveVO.setPhotoUrl("https://example.com/new.jpg");
        saveVO.setOutfitStyle("business");
        saveVO.setBackground("studio");

        List<String> stateSequence = new ArrayList<>();
        when(repo.save(any(PhotoAvatarTask.class))).thenAnswer(inv -> {
            PhotoAvatarTask t = inv.getArgument(0);
            stateSequence.add(t.getStatus());
            if (t.getId() == null) t.setId(100L);
            return t;
        });

        ImageResult mockResult = new ImageResult(
                "https://output.example.com/result.jpg", null, null, null);
        when(imageGenerationService.imageToImage(any(ImageToImageRequest.class), eq(userId)))
                .thenReturn(mockResult);

        Long taskId = service.createVideo(saveVO, userId);

        assertThat(taskId).isEqualTo(100L);
        assertThat(stateSequence).containsExactly("pending", "processing", "completed");
    }

    @Test
    void createVideo_shouldHandleImageGenerationFailure() {
        PhotoAvatarSaveVO saveVO = new PhotoAvatarSaveVO();
        saveVO.setPhotoUrl("https://example.com/bad.jpg");

        when(repo.save(any(PhotoAvatarTask.class))).thenAnswer(inv -> {
            PhotoAvatarTask t = inv.getArgument(0);
            if (t.getId() == null) t.setId(200L);
            return t;
        });

        when(imageGenerationService.imageToImage(any(ImageToImageRequest.class), eq(userId)))
                .thenThrow(new RuntimeException("API error"));

        Long taskId = service.createVideo(saveVO, userId);

        assertThat(taskId).isEqualTo(200L);
        verify(repo, atLeastOnce()).save(argThat(t ->
                "failed".equals(t.getStatus()) && t.getErrorMessage() != null));
    }

    @Test
    void getStatus_shouldReturnTaskForOwner() {
        when(repo.findById(1L)).thenReturn(Optional.of(pendingTask));

        var result = service.getStatus(1L, userId);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("pending");
        assertThat(result.getPhotoUrl()).isEqualTo("https://example.com/photo.jpg");
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
    void getStatus_shouldThrowForWrongUser() {
        PhotoAvatarTask otherUserTask = new PhotoAvatarTask();
        otherUserTask.setId(1L);
        otherUserTask.setUserId(999L);
        otherUserTask.setDeleted(0);
        when(repo.findById(1L)).thenReturn(Optional.of(otherUserTask));

        assertThatThrownBy(() -> service.getStatus(1L, userId))
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

    @Test
    void retry_shouldResetAndRegenerateFailedTask() {
        when(repo.findById(3L)).thenReturn(Optional.of(failedTask));
        when(repo.save(any(PhotoAvatarTask.class))).thenAnswer(inv -> inv.getArgument(0));

        ImageResult mockResult = new ImageResult(
                "https://output.example.com/retry.jpg", null, null, null);
        when(imageGenerationService.imageToImage(any(ImageToImageRequest.class), eq(userId)))
                .thenReturn(mockResult);

        Long taskId = service.retry(3L, userId);

        assertThat(taskId).isEqualTo(3L);
        verify(repo, atLeastOnce()).save(argThat(t ->
                "completed".equals(t.getStatus()) && t.getErrorMessage() == null));
    }

    @Test
    void retry_shouldThrowForNonFailedTask() {
        when(repo.findById(1L)).thenReturn(Optional.of(pendingTask));

        assertThatThrownBy(() -> service.retry(1L, userId))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.OPERATION_NOT_ALLOWED);
    }

    @Test
    void retry_shouldHandleFailure() {
        when(repo.findById(3L)).thenReturn(Optional.of(failedTask));
        when(repo.save(any(PhotoAvatarTask.class))).thenAnswer(inv -> inv.getArgument(0));
        when(imageGenerationService.imageToImage(any(ImageToImageRequest.class), eq(userId)))
                .thenThrow(new RuntimeException("Still failing"));

        Long taskId = service.retry(3L, userId);

        assertThat(taskId).isEqualTo(3L);
        verify(repo, atLeastOnce()).save(argThat(t ->
                "failed".equals(t.getStatus()) && t.getErrorMessage() != null));
    }
}
