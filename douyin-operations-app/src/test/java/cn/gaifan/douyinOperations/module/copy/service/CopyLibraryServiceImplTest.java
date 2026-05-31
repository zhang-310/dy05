package cn.gaifan.douyinOperations.module.copy.service;

import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.copy.entity.CopyLibrary;
import cn.gaifan.douyinOperations.module.copy.repository.CopyLibraryRepository;
import cn.gaifan.douyinOperations.module.copy.service.impl.CopyLibraryServiceImpl;
import cn.gaifan.douyinOperations.module.copy.vo.CopyLibrarySearchVO;
import cn.gaifan.douyinOperations.module.copy.vo.CopyLibrarySaveVO;
import cn.gaifan.douyinOperations.module.copy.vo.CopyLibraryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CopyLibraryService 单元测试")
class CopyLibraryServiceImplTest {

    @Mock
    private CopyLibraryRepository copyLibraryRepository;

    @InjectMocks
    private CopyLibraryServiceImpl copyLibraryService;

    private Long userId = 1L;
    private CopyLibrary mockCopy;

    @BeforeEach
    void setUp() {
        mockCopy = new CopyLibrary();
        mockCopy.setId(1L);
        mockCopy.setUserId(userId);
        mockCopy.setTitle("测试文案");
        mockCopy.setContent("这是一段测试文案内容");
        mockCopy.setCategory("直播话术");
        mockCopy.setTags("护肤,美妆");
        mockCopy.setWordCount(10);
        mockCopy.setUseCount(5);
        mockCopy.setRating(5);
        mockCopy.setStatus(1);
        mockCopy.setDeleted(0);
        mockCopy.setCreateTime(new Timestamp(System.currentTimeMillis()));
        mockCopy.setUpdateTime(new Timestamp(System.currentTimeMillis()));
    }

    @Test
    @DisplayName("搜索文案 - 应返回分页结果")
    void search_shouldReturnPageResult() {
        // Given
        CopyLibrarySearchVO searchVO = new CopyLibrarySearchVO();
        searchVO.setUserId(userId);
        Page<CopyLibrary> page = new PageImpl<>(List.of(mockCopy));
        when(copyLibraryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        // When
        PageResultVO<CopyLibraryVO> result = copyLibraryService.search(searchVO);

        // Then
        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getTitle()).isEqualTo("测试文案");
        verify(copyLibraryRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("搜索文案 - 按关键词过滤")
    void search_withKeyword_shouldFilter() {
        // Given
        CopyLibrarySearchVO searchVO = new CopyLibrarySearchVO();
        searchVO.setKeyword("测试");
        Page<CopyLibrary> page = new PageImpl<>(List.of(mockCopy));
        when(copyLibraryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        // When
        PageResultVO<CopyLibraryVO> result = copyLibraryService.search(searchVO);

        // Then
        assertThat(result.getTotal()).isEqualTo(1L);
        verify(copyLibraryRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("搜索文案 - 按分类过滤")
    void search_withCategory_shouldFilter() {
        // Given
        CopyLibrarySearchVO searchVO = new CopyLibrarySearchVO();
        searchVO.setCategory("直播话术");
        Page<CopyLibrary> page = new PageImpl<>(List.of(mockCopy));
        when(copyLibraryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        // When
        PageResultVO<CopyLibraryVO> result = copyLibraryService.search(searchVO);

        // Then
        assertThat(result.getTotal()).isEqualTo(1L);
        verify(copyLibraryRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("获取文案详情 - 正常情况")
    void getById_normal_shouldReturnVO() {
        // Given
        when(copyLibraryRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockCopy));

        // When
        CopyLibraryVO result = copyLibraryService.getById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("测试文案");
        verify(copyLibraryRepository).findByIdAndDeleted(1L, 0);
    }

    @Test
    @DisplayName("获取文案详情 - ID 无效应抛出异常")
    void getById_invalidId_shouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> copyLibraryService.getById(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文案 ID 无效");

        assertThatThrownBy(() -> copyLibraryService.getById(0L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文案 ID 无效");
    }

    @Test
    @DisplayName("获取文案详情 - 不存在应抛出异常")
    void getById_notFound_shouldThrowException() {
        // Given
        when(copyLibraryRepository.findByIdAndDeleted(999L, 0))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> copyLibraryService.getById(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文案不存在");
    }

    @Test
    @DisplayName("保存文案 - 新建应返回ID")
    void save_create_shouldReturnId() {
        // Given
        CopyLibrarySaveVO saveVO = new CopyLibrarySaveVO();
        saveVO.setUserId(userId);
        saveVO.setTitle("新文案");
        saveVO.setContent("新文案内容");
        saveVO.setCategory("短视频");
        when(copyLibraryRepository.save(any(CopyLibrary.class)))
                .thenReturn(mockCopy);

        // When
        long result = copyLibraryService.save(saveVO);

        // Then
        assertThat(result).isEqualTo(1L);
        verify(copyLibraryRepository).save(argThat(copy ->
                copy.getUserId().equals(userId) &&
                copy.getTitle().equals("新文案") &&
                copy.getWordCount() == 5
        ));
    }

    @Test
    @DisplayName("保存文案 - 更新应修改现有记录")
    void save_update_shouldModifyExisting() {
        // Given
        CopyLibrarySaveVO saveVO = new CopyLibrarySaveVO();
        saveVO.setId(1L);
        saveVO.setUserId(userId);
        saveVO.setTitle("更新后的文案");
        saveVO.setContent("更新后的内容");
        when(copyLibraryRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockCopy));
        when(copyLibraryRepository.save(any(CopyLibrary.class)))
                .thenReturn(mockCopy);

        // When
        long result = copyLibraryService.save(saveVO);

        // Then
        assertThat(result).isEqualTo(1L);
        verify(copyLibraryRepository).findByIdAndDeleted(1L, 0);
        verify(copyLibraryRepository).save(argThat(copy ->
                copy.getTitle().equals("更新后的文案") &&
                copy.getWordCount() == 6
        ));
    }

    @Test
    @DisplayName("保存文案 - 更新不存在的记录应抛出异常")
    void save_updateNotFound_shouldThrowException() {
        // Given
        CopyLibrarySaveVO saveVO = new CopyLibrarySaveVO();
        saveVO.setId(999L);
        saveVO.setUserId(userId);
        saveVO.setTitle("文案");
        saveVO.setContent("内容");
        when(copyLibraryRepository.findByIdAndDeleted(999L, 0))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> copyLibraryService.save(saveVO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文案不存在");
    }

    @Test
    @DisplayName("删除文案 - 正常情况应逻辑删除")
    void delete_normal_shouldLogicalDelete() {
        // Given
        when(copyLibraryRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockCopy));
        when(copyLibraryRepository.save(any(CopyLibrary.class)))
                .thenReturn(mockCopy);

        // When
        copyLibraryService.delete(1L, 1L);

        // Then
        verify(copyLibraryRepository).findByIdAndDeleted(1L, 0);
        verify(copyLibraryRepository).save(argThat(copy -> copy.getDeleted() == 1));
    }

    @Test
    @DisplayName("删除文案 - ID 无效应抛出异常")
    void delete_invalidId_shouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> copyLibraryService.delete(null, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文案 ID 无效");
    }

    @Test
    @DisplayName("删除文案 - 不存在应抛出异常")
    void delete_notFound_shouldThrowException() {
        // Given
        when(copyLibraryRepository.findByIdAndDeleted(999L, 0))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> copyLibraryService.delete(999L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文案不存在");
    }

    @Test
    @DisplayName("更新状态 - 正常情况")
    void updateStatus_normal_shouldUpdate() {
        // Given
        when(copyLibraryRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockCopy));

        // When
        copyLibraryService.updateStatus(1L, 1, 1L);

        // Then
        verify(copyLibraryRepository).findByIdAndDeleted(1L, 0);
        verify(copyLibraryRepository).updateStatus(1L, 1);
    }

    @Test
    @DisplayName("更新状态 - 不存在应抛出异常")
    void updateStatus_notFound_shouldThrowException() {
        // Given
        when(copyLibraryRepository.findByIdAndDeleted(999L, 0))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> copyLibraryService.updateStatus(999L, 1, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文案不存在");
    }

    @Test
    @DisplayName("增加使用次数 - 正常情况")
    void incrementUseCount_normal_shouldIncrement() {
        // Given
        when(copyLibraryRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockCopy));

        // When
        copyLibraryService.incrementUseCount(1L);

        // Then
        verify(copyLibraryRepository).findByIdAndDeleted(1L, 0);
        verify(copyLibraryRepository).incrementUseCount(1L);
    }

    @Test
    @DisplayName("增加使用次数 - 不存在应抛出异常")
    void incrementUseCount_notFound_shouldThrowException() {
        // Given
        when(copyLibraryRepository.findByIdAndDeleted(999L, 0))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> copyLibraryService.incrementUseCount(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文案不存在");
    }
}
