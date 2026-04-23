package cn.gaifan.douyinOperations.module.copy.service;

import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.copy.entity.CopyTemplate;
import cn.gaifan.douyinOperations.module.copy.repository.CopyTemplateRepository;
import cn.gaifan.douyinOperations.module.copy.service.impl.CopyTemplateServiceImpl;
import cn.gaifan.douyinOperations.module.copy.vo.CopyTemplateSearchVO;
import cn.gaifan.douyinOperations.module.copy.vo.CopyTemplateSaveVO;
import cn.gaifan.douyinOperations.module.copy.vo.CopyTemplateVO;
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
@DisplayName("CopyTemplateService 单元测试")
class CopyTemplateServiceImplTest {

    @Mock
    private CopyTemplateRepository copyTemplateRepository;

    @InjectMocks
    private CopyTemplateServiceImpl copyTemplateService;

    private Long userId = 1L;
    private CopyTemplate mockTemplate;

    @BeforeEach
    void setUp() {
        mockTemplate = new CopyTemplate();
        mockTemplate.setId(1L);
        mockTemplate.setUserId(userId);
        mockTemplate.setTemplateName("测试模板");
        mockTemplate.setTemplateContent("欢迎{name}，今天是{date}");
        mockTemplate.setCategory("直播话术");
        mockTemplate.setDescription("测试模板描述");
        mockTemplate.setStatus(1);
        mockTemplate.setDeleted(0);
        mockTemplate.setCreateTime(new Timestamp(System.currentTimeMillis()));
        mockTemplate.setUpdateTime(new Timestamp(System.currentTimeMillis()));
    }

    @Test
    @DisplayName("搜索模板 - 应返回分页结果")
    void search_shouldReturnPageResult() {
        // Given
        CopyTemplateSearchVO searchVO = new CopyTemplateSearchVO();
        searchVO.setUserId(userId);
        Page<CopyTemplate> page = new PageImpl<>(List.of(mockTemplate));
        when(copyTemplateRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        // When
        PageResultVO<CopyTemplateVO> result = copyTemplateService.search(searchVO);

        // Then
        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getTemplateName()).isEqualTo("测试模板");
        verify(copyTemplateRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("搜索模板 - 按关键词过滤")
    void search_withKeyword_shouldFilter() {
        // Given
        CopyTemplateSearchVO searchVO = new CopyTemplateSearchVO();
        searchVO.setKeyword("测试");
        Page<CopyTemplate> page = new PageImpl<>(List.of(mockTemplate));
        when(copyTemplateRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        // When
        PageResultVO<CopyTemplateVO> result = copyTemplateService.search(searchVO);

        // Then
        assertThat(result.getTotal()).isEqualTo(1L);
        verify(copyTemplateRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("获取模板详情 - 正常情况")
    void getById_normal_shouldReturnVO() {
        // Given
        when(copyTemplateRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockTemplate));

        // When
        CopyTemplateVO result = copyTemplateService.getById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTemplateName()).isEqualTo("测试模板");
        verify(copyTemplateRepository).findByIdAndDeleted(1L, 0);
    }

    @Test
    @DisplayName("获取模板详情 - ID 无效应抛出异常")
    void getById_invalidId_shouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> copyTemplateService.getById(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("模板 ID 无效");

        assertThatThrownBy(() -> copyTemplateService.getById(0L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("模板 ID 无效");
    }

    @Test
    @DisplayName("获取模板详情 - 不存在应抛出异常")
    void getById_notFound_shouldThrowException() {
        // Given
        when(copyTemplateRepository.findByIdAndDeleted(999L, 0))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> copyTemplateService.getById(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("模板不存在");
    }

    @Test
    @DisplayName("保存模板 - 新建应返回ID")
    void save_create_shouldReturnId() {
        // Given
        CopyTemplateSaveVO saveVO = new CopyTemplateSaveVO();
        saveVO.setUserId(userId);
        saveVO.setTemplateName("新模板");
        saveVO.setTemplateContent("你好{user_name}");
        saveVO.setCategory("短视频");
        when(copyTemplateRepository.save(any(CopyTemplate.class)))
                .thenReturn(mockTemplate);

        // When
        long result = copyTemplateService.save(saveVO);

        // Then
        assertThat(result).isEqualTo(1L);
        verify(copyTemplateRepository).save(argThat(template ->
                template.getUserId().equals(userId) &&
                template.getTemplateName().equals("新模板")
        ));
    }

    @Test
    @DisplayName("保存模板 - 更新应修改现有记录")
    void save_update_shouldModifyExisting() {
        // Given
        CopyTemplateSaveVO saveVO = new CopyTemplateSaveVO();
        saveVO.setId(1L);
        saveVO.setUserId(userId);
        saveVO.setTemplateName("更新后的模板");
        saveVO.setTemplateContent("更新{content}");
        when(copyTemplateRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockTemplate));
        when(copyTemplateRepository.save(any(CopyTemplate.class)))
                .thenReturn(mockTemplate);

        // When
        long result = copyTemplateService.save(saveVO);

        // Then
        assertThat(result).isEqualTo(1L);
        verify(copyTemplateRepository).findByIdAndDeleted(1L, 0);
        verify(copyTemplateRepository).save(argThat(template ->
                template.getTemplateName().equals("更新后的模板")
        ));
    }

    @Test
    @DisplayName("保存模板 - 更新不存在的记录应抛出异常")
    void save_updateNotFound_shouldThrowException() {
        // Given
        CopyTemplateSaveVO saveVO = new CopyTemplateSaveVO();
        saveVO.setId(999L);
        saveVO.setUserId(userId);
        saveVO.setTemplateName("模板");
        saveVO.setTemplateContent("内容");
        when(copyTemplateRepository.findByIdAndDeleted(999L, 0))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> copyTemplateService.save(saveVO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("模板不存在");
    }

    @Test
    @DisplayName("保存模板 - 变量格式合法应通过")
    void save_validVariables_shouldPass() {
        // Given
        CopyTemplateSaveVO saveVO = new CopyTemplateSaveVO();
        saveVO.setUserId(userId);
        saveVO.setTemplateName("变量模板");
        saveVO.setTemplateContent("你好{user_name}，今天是{date_2024}，欢迎{_welcome}");
        when(copyTemplateRepository.save(any(CopyTemplate.class)))
                .thenReturn(mockTemplate);

        // When & Then
        assertThatCode(() -> copyTemplateService.save(saveVO))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("保存模板 - 变量格式非法应抛出异常")
    void save_invalidVariables_shouldThrowException() {
        // Given
        CopyTemplateSaveVO saveVO = new CopyTemplateSaveVO();
        saveVO.setUserId(userId);
        saveVO.setTemplateName("非法变量模板");
        saveVO.setTemplateContent("你好{123invalid}");

        // When & Then
        assertThatThrownBy(() -> copyTemplateService.save(saveVO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("模板变量格式非法");
    }

    @Test
    @DisplayName("保存模板 - 变量名含特殊字符应抛出异常")
    void save_variablesWithSpecialChars_shouldThrowException() {
        // Given
        CopyTemplateSaveVO saveVO = new CopyTemplateSaveVO();
        saveVO.setUserId(userId);
        saveVO.setTemplateName("特殊字符变量模板");
        saveVO.setTemplateContent("你好{user-name}");

        // When & Then
        assertThatThrownBy(() -> copyTemplateService.save(saveVO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("模板变量格式非法");
    }

    @Test
    @DisplayName("删除模板 - 正常情况应逻辑删除")
    void delete_normal_shouldLogicalDelete() {
        // Given
        when(copyTemplateRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockTemplate));
        when(copyTemplateRepository.save(any(CopyTemplate.class)))
                .thenReturn(mockTemplate);

        // When
        copyTemplateService.delete(1L);

        // Then
        verify(copyTemplateRepository).findByIdAndDeleted(1L, 0);
        verify(copyTemplateRepository).save(argThat(template -> template.getDeleted() == 1));
    }

    @Test
    @DisplayName("删除模板 - ID 无效应抛出异常")
    void delete_invalidId_shouldThrowException() {
        // When & Then
        assertThatThrownBy(() -> copyTemplateService.delete(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("模板 ID 无效");
    }

    @Test
    @DisplayName("删除模板 - 不存在应抛出异常")
    void delete_notFound_shouldThrowException() {
        // Given
        when(copyTemplateRepository.findByIdAndDeleted(999L, 0))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> copyTemplateService.delete(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("模板不存在");
    }

    @Test
    @DisplayName("更新状态 - 正常情况")
    void updateStatus_normal_shouldUpdate() {
        // Given
        when(copyTemplateRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockTemplate));

        // When
        copyTemplateService.updateStatus(1L, 0);

        // Then
        verify(copyTemplateRepository).findByIdAndDeleted(1L, 0);
        verify(copyTemplateRepository).updateStatus(1L, 0);
    }

    @Test
    @DisplayName("更新状态 - 不存在应抛出异常")
    void updateStatus_notFound_shouldThrowException() {
        // Given
        when(copyTemplateRepository.findByIdAndDeleted(999L, 0))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> copyTemplateService.updateStatus(999L, 0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("模板不存在");
    }
}
