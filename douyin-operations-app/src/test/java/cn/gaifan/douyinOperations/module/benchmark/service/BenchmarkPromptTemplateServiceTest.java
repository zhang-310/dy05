package cn.gaifan.douyinOperations.module.benchmark.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.benchmark.entity.BenchmarkPromptTemplate;
import cn.gaifan.douyinOperations.module.benchmark.repository.BenchmarkPromptTemplateRepository;
import cn.gaifan.douyinOperations.module.benchmark.service.impl.BenchmarkPromptTemplateServiceImpl;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkPromptTemplateSearchVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkPromptTemplateSaveVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkPromptTemplateVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BenchmarkPromptTemplateService 单元测试
 */
@DisplayName("BenchmarkPromptTemplateService 单元测试")
class BenchmarkPromptTemplateServiceTest {

    @Mock
    private BenchmarkPromptTemplateRepository repository;

    @InjectMocks
    private BenchmarkPromptTemplateServiceImpl service;

    private static final Long TEST_USER_ID = 1L;
    private BenchmarkPromptTemplate testTemplate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testTemplate = new BenchmarkPromptTemplate();
        testTemplate.setId(1L);
        testTemplate.setOwnerId(TEST_USER_ID);
        testTemplate.setTemplateName("测试模板");
        testTemplate.setTemplateCode("TEST_TEMPLATE_001");
        testTemplate.setTemplateContent("这是一个测试 Prompt 模板，包含变量 {{variable1}} 和 {{variable2}}");
        testTemplate.setTemplateVariables("{\"variable1\": \"string\", \"variable2\": \"number\"}");
        testTemplate.setSceneType("creative_analysis");
        testTemplate.setIndustry("beauty");
        testTemplate.setIsActive(true);
        testTemplate.setVersion(1);
        testTemplate.setUsageCount(10);
        testTemplate.setAvgScore(BigDecimal.valueOf(4.5));
        testTemplate.setDeleted(0);
    }

    @Test
    @DisplayName("保存模板 - 新增 - 应成功保存")
    void save_newTemplate_shouldSaveSuccessfully() {
        BenchmarkPromptTemplateSaveVO saveVO = new BenchmarkPromptTemplateSaveVO();
        saveVO.setTemplateName("新模板");
        saveVO.setTemplateCode("NEW_TEMPLATE_001");
        saveVO.setTemplateContent("新的 Prompt 内容");
        saveVO.setSceneType("creative_analysis");
        saveVO.setIsActive(true);

        when(repository.findByTemplateCodeAndDeleted("NEW_TEMPLATE_001", 0)).thenReturn(Optional.empty());
        when(repository.save(any(BenchmarkPromptTemplate.class))).thenReturn(testTemplate);

        BenchmarkPromptTemplateVO result = service.save(saveVO, TEST_USER_ID);

        assertThat(result).isNotNull();
        verify(repository, times(1)).save(any(BenchmarkPromptTemplate.class));
    }

    @Test
    @DisplayName("保存模板 - 更新 - 应成功更新")
    void save_existingTemplate_shouldUpdateSuccessfully() {
        BenchmarkPromptTemplateSaveVO saveVO = new BenchmarkPromptTemplateSaveVO();
        saveVO.setId(1L);
        saveVO.setTemplateName("更新后的模板");
        saveVO.setTemplateCode("TEST_TEMPLATE_001");
        saveVO.setTemplateContent("更新后的内容");

        when(repository.findById(1L)).thenReturn(Optional.of(testTemplate));
        when(repository.findByTemplateCodeAndDeleted("TEST_TEMPLATE_001", 0)).thenReturn(Optional.of(testTemplate));
        when(repository.save(any(BenchmarkPromptTemplate.class))).thenReturn(testTemplate);

        BenchmarkPromptTemplateVO result = service.save(saveVO, TEST_USER_ID);

        assertThat(result).isNotNull();
        verify(repository, times(1)).findById(1L);
        verify(repository, times(1)).save(any(BenchmarkPromptTemplate.class));
        assertThat(testTemplate.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("保存模板 - 模板编码重复 - 应抛出异常")
    void save_duplicateTemplateCode_shouldThrowException() {
        BenchmarkPromptTemplateSaveVO saveVO = new BenchmarkPromptTemplateSaveVO();
        saveVO.setTemplateName("重复编码模板");
        saveVO.setTemplateCode("TEST_TEMPLATE_001");
        saveVO.setTemplateContent("内容");

        BenchmarkPromptTemplate existingTemplate = new BenchmarkPromptTemplate();
        existingTemplate.setId(999L);
        existingTemplate.setTemplateCode("TEST_TEMPLATE_001");

        when(repository.findByTemplateCodeAndDeleted("TEST_TEMPLATE_001", 0)).thenReturn(Optional.of(existingTemplate));

        assertThatThrownBy(() -> service.save(saveVO, TEST_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("模板编码已存在");
    }

    @Test
    @DisplayName("查询列表 - 应返回分页结果")
    void search_shouldReturnPagedResult() {
        BenchmarkPromptTemplateSearchVO searchVO = new BenchmarkPromptTemplateSearchVO();
        searchVO.setPage(0);
        searchVO.setRows(10);
        searchVO.setSceneType("creative_analysis");

        Page<BenchmarkPromptTemplate> page = new PageImpl<>(List.of(testTemplate), PageRequest.of(0, 10), 1);
        when(repository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);

        PageResultVO<BenchmarkPromptTemplateVO> result = service.search(searchVO, TEST_USER_ID);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getTemplateName()).isEqualTo("测试模板");
    }

    @Test
    @DisplayName("按 ID 查询 - 应返回模板")
    void getById_shouldReturnTemplate() {
        when(repository.findById(1L)).thenReturn(Optional.of(testTemplate));

        BenchmarkPromptTemplateVO result = service.getById(1L, TEST_USER_ID);

        assertThat(result).isNotNull();
        assertThat(result.getTemplateName()).isEqualTo("测试模板");
        assertThat(result.getTemplateCode()).isEqualTo("TEST_TEMPLATE_001");
    }

    @Test
    @DisplayName("按 ID 查询 - 不存在 - 应抛出异常")
    void getById_nonExistent_shouldThrowException() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999L, TEST_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Prompt 模板不存在");
    }

    @Test
    @DisplayName("删除模板 - 应逻辑删除")
    void delete_shouldLogicallyDelete() {
        when(repository.findById(1L)).thenReturn(Optional.of(testTemplate));

        service.delete(1L, TEST_USER_ID);

        verify(repository, times(1)).save(testTemplate);
        assertThat(testTemplate.getDeleted()).isEqualTo(1);
    }

    @Test
    @DisplayName("切换激活状态 - 应更新状态")
    void toggleActive_shouldUpdateStatus() {
        when(repository.findById(1L)).thenReturn(Optional.of(testTemplate));

        service.toggleActive(1L, false, TEST_USER_ID);

        verify(repository, times(1)).save(testTemplate);
        assertThat(testTemplate.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("按场景类型查询激活模板 - 应返回列表")
    void getActiveTemplatesByScene_shouldReturnList() {
        when(repository.findByOwnerIdAndSceneTypeAndIsActiveAndDeleted(TEST_USER_ID, "creative_analysis", true, 0))
                .thenReturn(List.of(testTemplate));

        List<BenchmarkPromptTemplateVO> result = service.getActiveTemplatesByScene("creative_analysis", TEST_USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSceneType()).isEqualTo("creative_analysis");
    }

    @Test
    @DisplayName("按模板编码查询 - 应返回模板")
    void getByTemplateCode_shouldReturnTemplate() {
        when(repository.findByTemplateCodeAndDeleted("TEST_TEMPLATE_001", 0)).thenReturn(Optional.of(testTemplate));

        BenchmarkPromptTemplateVO result = service.getByTemplateCode("TEST_TEMPLATE_001", TEST_USER_ID);

        assertThat(result).isNotNull();
        assertThat(result.getTemplateCode()).isEqualTo("TEST_TEMPLATE_001");
    }

    @Test
    @DisplayName("更新使用统计 - 应更新使用次数和评分")
    void updateUsageStats_shouldUpdateStats() {
        when(repository.findById(1L)).thenReturn(Optional.of(testTemplate));

        service.updateUsageStats(1L, 5.0);

        verify(repository, times(1)).save(testTemplate);
        assertThat(testTemplate.getUsageCount()).isEqualTo(11);
        // 平均分计算：(4.5 * 10 + 5.0) / 11 = 4.55
        assertThat(testTemplate.getAvgScore()).isGreaterThan(BigDecimal.valueOf(4.5));
    }
}
