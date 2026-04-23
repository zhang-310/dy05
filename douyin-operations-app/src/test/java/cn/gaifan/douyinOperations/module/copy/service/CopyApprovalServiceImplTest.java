package cn.gaifan.douyinOperations.module.copy.service;

import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.copy.entity.CopyApproval;
import cn.gaifan.douyinOperations.module.copy.entity.CopyLibrary;
import cn.gaifan.douyinOperations.module.copy.repository.CopyApprovalRepository;
import cn.gaifan.douyinOperations.module.copy.repository.CopyLibraryRepository;
import cn.gaifan.douyinOperations.module.copy.service.impl.CopyApprovalServiceImpl;
import cn.gaifan.douyinOperations.module.copy.vo.CopyApprovalSearchVO;
import cn.gaifan.douyinOperations.module.copy.vo.CopyApprovalSaveVO;
import cn.gaifan.douyinOperations.module.copy.vo.CopyApprovalVO;
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
@DisplayName("CopyApprovalService 单元测试")
class CopyApprovalServiceImplTest {

    @Mock
    private CopyApprovalRepository copyApprovalRepository;

    @Mock
    private CopyLibraryRepository copyLibraryRepository;

    @InjectMocks
    private CopyApprovalServiceImpl copyApprovalService;

    private Long userId = 1L;
    private Long copyId = 100L;
    private CopyApproval mockApproval;
    private CopyLibrary mockCopy;

    @BeforeEach
    void setUp() {
        mockApproval = new CopyApproval();
        mockApproval.setId(1L);
        mockApproval.setCopyId(copyId);
        mockApproval.setUserId(userId);
        mockApproval.setApprovalStatus(2); // 待审核
        mockApproval.setComments("待审核");
        mockApproval.setDeleted(0);
        mockApproval.setCreateTime(new Timestamp(System.currentTimeMillis()));
        mockApproval.setUpdateTime(new Timestamp(System.currentTimeMillis()));

        mockCopy = new CopyLibrary();
        mockCopy.setId(copyId);
        mockCopy.setUserId(2L);
        mockCopy.setTitle("测试文案");
        mockCopy.setContent("测试内容");
        mockCopy.setStatus(0);
        mockCopy.setDeleted(0);
    }

    @Test
    @DisplayName("搜索审批记录 - 应返回分页结果")
    void search_shouldReturnPageResult() {
        // Given
        CopyApprovalSearchVO searchVO = new CopyApprovalSearchVO();
        searchVO.setUserId(userId);
        Page<CopyApproval> page = new PageImpl<>(List.of(mockApproval));
        when(copyApprovalRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(copyLibraryRepository.findAllById(anyList()))
                .thenReturn(List.of(mockCopy));

        // When
        PageResultVO<CopyApprovalVO> result = copyApprovalService.search(searchVO);

        // Then
        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getCopyId()).isEqualTo(copyId);
        verify(copyApprovalRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("搜索审批记录 - 按审批状态过滤")
    void search_withApprovalStatus_shouldFilter() {
        // Given
        CopyApprovalSearchVO searchVO = new CopyApprovalSearchVO();
        searchVO.setApprovalStatus(2);
        Page<CopyApproval> page = new PageImpl<>(List.of(mockApproval));
        when(copyApprovalRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(copyLibraryRepository.findAllById(anyList()))
                .thenReturn(List.of(mockCopy));

        // When
        PageResultVO<CopyApprovalVO> result = copyApprovalService.search(searchVO);

        // Then
        assertThat(result.getTotal()).isEqualTo(1L);
        verify(copyApprovalRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("搜索审批记录 - 按关键词过滤")
    void search_withKeyword_shouldFilter() {
        // Given
        CopyApprovalSearchVO searchVO = new CopyApprovalSearchVO();
        searchVO.setKeyword("测试");
        Page<CopyApproval> page = new PageImpl<>(List.of(mockApproval));
        when(copyApprovalRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(copyLibraryRepository.findAllById(anyList()))
                .thenReturn(List.of(mockCopy));

        // When
        PageResultVO<CopyApprovalVO> result = copyApprovalService.search(searchVO);

        // Then
        assertThat(result.getTotal()).isEqualTo(1L);
        verify(copyApprovalRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("搜索审批记录 - 关键词无匹配应返回空")
    void search_withKeywordNoMatch_shouldReturnEmpty() {
        // Given
        CopyApprovalSearchVO searchVO = new CopyApprovalSearchVO();
        searchVO.setKeyword("不存在");
        Page<CopyApproval> page = new PageImpl<>(List.of());
        when(copyApprovalRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        // When
        PageResultVO<CopyApprovalVO> result = copyApprovalService.search(searchVO);

        // Then
        assertThat(result.getTotal()).isEqualTo(0L);
        verify(copyApprovalRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("获取审批详情 - 正常情况")
    void getById_normal_shouldReturnVO() {
        // Given
        when(copyApprovalRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockApproval));
        when(copyLibraryRepository.findByIdAndDeleted(copyId, 0))
                .thenReturn(Optional.of(mockCopy));

        // When
        CopyApprovalVO result = copyApprovalService.getById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCopyId()).isEqualTo(copyId);
        assertThat(result.getCopyTitle()).isEqualTo("测试文案");
        verify(copyApprovalRepository).findByIdAndDeleted(1L, 0);
    }

    @Test
    @DisplayName("获取审批详情 - ID 无效应抛出异常")
    void getById_invalidId_shouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> copyApprovalService.getById(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("审批 ID 无效");

        assertThatThrownBy(() -> copyApprovalService.getById(0L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("审批 ID 无效");
    }

    @Test
    @DisplayName("获取审批详情 - 不存在应抛出异常")
    void getById_notFound_shouldThrowException() {
        // Given
        when(copyApprovalRepository.findByIdAndDeleted(999L, 0))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> copyApprovalService.getById(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("审批记录不存在");
    }

    @Test
    @DisplayName("保存审批 - 新建应返回ID")
    void save_create_shouldReturnId() {
        // Given
        CopyApprovalSaveVO saveVO = new CopyApprovalSaveVO();
        saveVO.setCopyId(copyId);
        saveVO.setUserId(userId);
        saveVO.setApprovalStatus(2);
        when(copyLibraryRepository.findByIdAndDeleted(copyId, 0))
                .thenReturn(Optional.of(mockCopy));
        when(copyApprovalRepository.save(any(CopyApproval.class)))
                .thenReturn(mockApproval);

        // When
        long result = copyApprovalService.save(saveVO);

        // Then
        assertThat(result).isEqualTo(1L);
        verify(copyLibraryRepository).findByIdAndDeleted(copyId, 0);
        verify(copyApprovalRepository).save(argThat(approval ->
                approval.getCopyId().equals(copyId) &&
                approval.getApprovalStatus() == 2
        ));
    }

    @Test
    @DisplayName("保存审批 - 新建时文案不存在应抛出异常")
    void save_createWithInvalidCopy_shouldThrowException() {
        // Given
        CopyApprovalSaveVO saveVO = new CopyApprovalSaveVO();
        saveVO.setCopyId(999L);
        saveVO.setUserId(userId);
        when(copyLibraryRepository.findByIdAndDeleted(999L, 0))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> copyApprovalService.save(saveVO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文案不存在");
    }

    @Test
    @DisplayName("保存审批 - 更新应修改现有记录")
    void save_update_shouldModifyExisting() {
        // Given
        CopyApprovalSaveVO saveVO = new CopyApprovalSaveVO();
        saveVO.setId(1L);
        saveVO.setCopyId(copyId);
        saveVO.setUserId(userId);
        saveVO.setApprovalStatus(1); // 通过
        saveVO.setComments("审核通过");
        when(copyApprovalRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockApproval));
        when(copyApprovalRepository.save(any(CopyApproval.class)))
                .thenReturn(mockApproval);

        // When
        long result = copyApprovalService.save(saveVO);

        // Then
        assertThat(result).isEqualTo(1L);
        verify(copyApprovalRepository).findByIdAndDeleted(1L, 0);
        verify(copyLibraryRepository).updateStatus(copyId, 1);
        verify(copyApprovalRepository).save(argThat(approval ->
                approval.getApprovalStatus() == 1 &&
                approval.getApprovalTime() != null
        ));
    }

    @Test
    @DisplayName("保存审批 - 拒绝应同步更新文案状态为待审核")
    void save_reject_shouldUpdateCopyStatusToPending() {
        // Given
        CopyApprovalSaveVO saveVO = new CopyApprovalSaveVO();
        saveVO.setId(1L);
        saveVO.setCopyId(copyId);
        saveVO.setUserId(userId);
        saveVO.setApprovalStatus(0); // 拒绝
        saveVO.setComments("不符合规范");
        when(copyApprovalRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockApproval));
        when(copyApprovalRepository.save(any(CopyApproval.class)))
                .thenReturn(mockApproval);

        // When
        long result = copyApprovalService.save(saveVO);

        // Then
        assertThat(result).isEqualTo(1L);
        verify(copyLibraryRepository).updateStatus(copyId, 0);
        verify(copyApprovalRepository).save(argThat(approval ->
                approval.getApprovalStatus() == 0 &&
                approval.getApprovalTime() != null
        ));
    }

    @Test
    @DisplayName("保存审批 - 更新不存在的记录应抛出异常")
    void save_updateNotFound_shouldThrowException() {
        // Given
        CopyApprovalSaveVO saveVO = new CopyApprovalSaveVO();
        saveVO.setId(999L);
        saveVO.setCopyId(copyId);
        saveVO.setUserId(userId);
        when(copyApprovalRepository.findByIdAndDeleted(999L, 0))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> copyApprovalService.save(saveVO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("审批记录不存在");
    }

    @Test
    @DisplayName("删除审批 - 正常情况应逻辑删除")
    void delete_normal_shouldLogicalDelete() {
        // Given
        when(copyApprovalRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockApproval));
        when(copyApprovalRepository.save(any(CopyApproval.class)))
                .thenReturn(mockApproval);

        // When
        copyApprovalService.delete(1L);

        // Then
        verify(copyApprovalRepository).findByIdAndDeleted(1L, 0);
        verify(copyApprovalRepository).save(argThat(approval -> approval.getDeleted() == 1));
    }

    @Test
    @DisplayName("删除审批 - ID 无效应抛出异常")
    void delete_invalidId_shouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> copyApprovalService.delete(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("审批 ID 无效");
    }

    @Test
    @DisplayName("删除审批 - 不存在应抛出异常")
    void delete_notFound_shouldThrowException() {
        // Given
        when(copyApprovalRepository.findByIdAndDeleted(999L, 0))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> copyApprovalService.delete(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("审批记录不存在");
    }
}
