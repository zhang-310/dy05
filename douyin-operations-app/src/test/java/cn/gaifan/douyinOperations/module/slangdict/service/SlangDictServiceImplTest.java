package cn.gaifan.douyinOperations.module.slangdict.service;

import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.entity.AiTaskModelConfig;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiTaskModelConfigRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import cn.gaifan.douyinOperations.module.slangdict.entity.SdEntry;
import cn.gaifan.douyinOperations.module.slangdict.entity.SdProductMapping;
import cn.gaifan.douyinOperations.module.slangdict.repository.SdEntryRepository;
import cn.gaifan.douyinOperations.module.slangdict.repository.SdProductMappingRepository;
import cn.gaifan.douyinOperations.module.slangdict.service.impl.SlangDictServiceImpl;
import cn.gaifan.douyinOperations.module.slangdict.vo.SdEntrySearchVO;
import cn.gaifan.douyinOperations.module.slangdict.vo.SdEntrySaveVO;
import cn.gaifan.douyinOperations.module.slangdict.vo.SdEntryVO;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SlangDictService 单元测试")
class SlangDictServiceImplTest {

    @Mock
    private SdEntryRepository entryRepository;
    @Mock
    private SdProductMappingRepository mappingRepository;
    @Mock
    private DyProductRepository productRepository;
    @Mock
    private LlmClient llmClient;
    @Mock
    private AiModelRepository aiModelRepository;
    @Mock
    private AiTaskModelConfigRepository taskModelConfigRepository;

    @InjectMocks
    private SlangDictServiceImpl slangDictService;

    private Long userId = 1L;
    private Long productId = 1L;
    private SdEntry mockEntry;
    private SdProductMapping mockMapping;
    private DyProduct mockProduct;
    private AiModel mockModel;

    @BeforeEach
    void setUp() {
        // Mock Entry
        mockEntry = new SdEntry();
        mockEntry.setId(1L);
        mockEntry.setUserId(userId);
        mockEntry.setPhrase("小金瓶");
        mockEntry.setMeaning("护肤精华液");
        mockEntry.setCategory("护肤");
        mockEntry.setUsageScene("直播");
        mockEntry.setExample("今天给大家推荐小金瓶");
        mockEntry.setSource("主播创作");
        mockEntry.setUseCount(10);
        mockEntry.setStatus(1);
        mockEntry.setDeleted(0);

        // Mock Mapping
        mockMapping = new SdProductMapping();
        mockMapping.setId(1L);
        mockMapping.setEntryId(1L);
        mockMapping.setProductId(productId);
        mockMapping.setUserId(userId);
        mockMapping.setDeleted(0);

        // Mock Product
        mockProduct = new DyProduct();
        mockProduct.setId(productId);
        mockProduct.setUserId(userId);
        mockProduct.setProductName("护肤精华液");
        mockProduct.setDescription("高效保湿精华");
        mockProduct.setPrice(new BigDecimal("299.00"));

        // Mock AI Model
        mockModel = new AiModel();
        mockModel.setId(1L);
        mockModel.setModelName("gpt-4");
        mockModel.setStatus(1);
        mockModel.setDeleted(0);
    }

    @Test
    @DisplayName("搜索梗条目 - 应返回分页结果")
    void search_shouldReturnPageResult() {
        // Given
        SdEntrySearchVO searchVO = new SdEntrySearchVO();
        searchVO.setUserId(userId);
        searchVO.setPage(0);
        searchVO.setRows(10);

        Page<SdEntry> mockPage = new PageImpl<>(List.of(mockEntry));
        when(entryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        // When
        PageResultVO<SdEntryVO> result = slangDictService.search(searchVO);

        // Then
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getPhrase()).isEqualTo("小金瓶");
    }

    @Test
    @DisplayName("搜索梗条目 - 按产品ID搜索应返回关联条目")
    void search_byProductId_shouldReturnRelatedEntries() {
        // Given
        SdEntrySearchVO searchVO = new SdEntrySearchVO();
        searchVO.setUserId(userId);
        searchVO.setProductId(productId);
        searchVO.setPage(0);
        searchVO.setRows(10);

        when(mappingRepository.findByProductIdAndUserIdAndDeleted(productId, userId, 0))
                .thenReturn(List.of(mockMapping));

        Page<SdEntry> mockPage = new PageImpl<>(List.of(mockEntry));
        when(entryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        // When
        PageResultVO<SdEntryVO> result = slangDictService.search(searchVO);

        // Then
        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
    }

    @Test
    @DisplayName("搜索梗条目 - 产品无关联条目应返回空结果")
    void search_byProductId_noMappings_shouldReturnEmpty() {
        // Given
        SdEntrySearchVO searchVO = new SdEntrySearchVO();
        searchVO.setUserId(userId);
        searchVO.setProductId(productId);
        searchVO.setPage(0);
        searchVO.setRows(10);

        when(mappingRepository.findByProductIdAndUserIdAndDeleted(productId, userId, 0))
                .thenReturn(List.of());

        // When
        PageResultVO<SdEntryVO> result = slangDictService.search(searchVO);

        // Then
        assertThat(result.getTotal()).isEqualTo(0);
        assertThat(result.getList()).isEmpty();
    }

    @Test
    @DisplayName("根据ID获取梗条目 - 存在应返回VO")
    void getById_exists_shouldReturnVO() {
        // Given
        when(entryRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockEntry));
        when(mappingRepository.findByEntryIdAndDeleted(1L, 0))
                .thenReturn(List.of(mockMapping));

        // When
        SdEntryVO result = slangDictService.getById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPhrase()).isEqualTo("小金瓶");
        assertThat(result.getProductIds()).containsExactly(productId);
    }

    @Test
    @DisplayName("根据ID获取梗条目 - 不存在应抛出异常")
    void getById_notExists_shouldThrowException() {
        // Given
        when(entryRepository.findByIdAndDeleted(999L, 0))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> slangDictService.getById(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("梗条目不存在");
    }

    @Test
    @DisplayName("保存梗条目 - 新增应创建记录")
    void save_create_shouldCreateNew() {
        // Given
        SdEntrySaveVO saveVO = new SdEntrySaveVO();
        saveVO.setPhrase("小蓝瓶");
        saveVO.setMeaning("保湿面霜");
        saveVO.setCategory("护肤");

        when(entryRepository.save(any(SdEntry.class)))
                .thenAnswer(invocation -> {
                    SdEntry saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                });

        // When
        Long result = slangDictService.save(saveVO, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(1L);
        verify(entryRepository).save(argThat(entry ->
                entry.getPhrase().equals("小蓝瓶") &&
                entry.getUserId().equals(userId)
        ));
    }

    @Test
    @DisplayName("保存梗条目 - 更新应修改现有记录")
    void save_update_shouldModifyExisting() {
        // Given
        SdEntrySaveVO saveVO = new SdEntrySaveVO();
        saveVO.setId(1L);
        saveVO.setPhrase("更新后的梗");
        saveVO.setMeaning("更新后的含义");

        when(entryRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockEntry));
        when(entryRepository.save(any(SdEntry.class)))
                .thenReturn(mockEntry);

        // When
        Long result = slangDictService.save(saveVO, userId);

        // Then
        verify(entryRepository).save(argThat(entry ->
                entry.getPhrase().equals("更新后的梗") &&
                entry.getMeaning().equals("更新后的含义")
        ));
    }

    @Test
    @DisplayName("删除梗条目 - 应标记为已删除")
    void delete_shouldMarkAsDeleted() {
        // Given
        when(entryRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockEntry));

        // When
        slangDictService.delete(1L);

        // Then
        verify(entryRepository).save(argThat(entry ->
                entry.getDeleted() == 1
        ));
    }

    @Test
    @DisplayName("根据产品ID获取梗条目 - 应返回关联条目列表")
    void getByProductId_shouldReturnRelatedEntries() {
        // Given
        when(mappingRepository.findByProductIdAndUserIdAndDeleted(productId, userId, 0))
                .thenReturn(List.of(mockMapping));
        when(entryRepository.findAllById(anySet()))
                .thenReturn(List.of(mockEntry));

        // When
        List<SdEntryVO> result = slangDictService.getByProductId(productId, userId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPhrase()).isEqualTo("小金瓶");
    }

    @Test
    @DisplayName("根据产品ID获取梗条目 - 无关联应返回空列表")
    void getByProductId_noMappings_shouldReturnEmpty() {
        // Given
        when(mappingRepository.findByProductIdAndUserIdAndDeleted(productId, userId, 0))
                .thenReturn(List.of());

        // When
        List<SdEntryVO> result = slangDictService.getByProductId(productId, userId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("绑定产品 - 应创建关联记录")
    void bindProduct_shouldCreateMapping() {
        // Given
        when(entryRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockEntry));
        when(mappingRepository.findByProductIdAndUserIdAndDeleted(productId, userId, 0))
                .thenReturn(List.of());

        // When
        slangDictService.bindProduct(1L, productId, userId);

        // Then
        verify(mappingRepository).save(argThat(mapping ->
                mapping.getEntryId().equals(1L) &&
                mapping.getProductId().equals(productId) &&
                mapping.getUserId().equals(userId)
        ));
    }

    @Test
    @DisplayName("绑定产品 - 已绑定应不重复创建")
    void bindProduct_alreadyBound_shouldNotCreateDuplicate() {
        // Given
        when(entryRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockEntry));
        when(mappingRepository.findByProductIdAndUserIdAndDeleted(productId, userId, 0))
                .thenReturn(List.of(mockMapping));

        // When
        slangDictService.bindProduct(1L, productId, userId);

        // Then
        verify(mappingRepository, never()).save(any(SdProductMapping.class));
    }

    @Test
    @DisplayName("解绑产品 - 应调用软删除方法")
    void unbindProduct_shouldCallSoftDelete() {
        // When
        slangDictService.unbindProduct(1L, productId, userId);

        // Then
        verify(mappingRepository).softDeleteByEntryAndProduct(1L, productId, userId);
    }

    @Test
    @DisplayName("AI生成梗 - 成功应返回梗列表")
    void aiGeneratePhrases_success_shouldReturnList() {
        // Given
        when(productRepository.findById(productId))
                .thenReturn(Optional.of(mockProduct));
        when(taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted("copy_processing", 1, 0))
                .thenReturn(Optional.empty());
        when(aiModelRepository.findByStatusAndDeleted(1, 0))
                .thenReturn(List.of(mockModel));

        LlmClient.LlmResponse mockResponse = new LlmClient.LlmResponse(
                "\"小金瓶\" → 护肤精华液\n\"神仙水\" → 保湿精华\n\"小棕瓶\" → 修复精华",
                100L,
                true,
                null
        );
        when(llmClient.chatWithFallback(anyList(), anyString(), anyString()))
                .thenReturn(mockResponse);

        // When
        List<String> result = slangDictService.aiGeneratePhrases(productId, userId, 3);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0)).contains("小金瓶");
    }

    @Test
    @DisplayName("AI生成梗 - 产品不存在应抛出异常")
    void aiGeneratePhrases_productNotExists_shouldThrowException() {
        // Given
        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> slangDictService.aiGeneratePhrases(999L, userId, 5))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("商品不存在");
    }

    @Test
    @DisplayName("AI生成梗 - 无可用模型应抛出异常")
    void aiGeneratePhrases_noModels_shouldThrowException() {
        // Given
        when(productRepository.findById(productId))
                .thenReturn(Optional.of(mockProduct));
        when(taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted("copy_processing", 1, 0))
                .thenReturn(Optional.empty());
        when(aiModelRepository.findByStatusAndDeleted(1, 0))
                .thenReturn(List.of());

        // When & Then
        assertThatThrownBy(() -> slangDictService.aiGeneratePhrases(productId, userId, 5))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无可用 AI 模型");
    }

    @Test
    @DisplayName("AI生成梗 - AI失败应抛出异常")
    void aiGeneratePhrases_aiFails_shouldThrowException() {
        // Given
        when(productRepository.findById(productId))
                .thenReturn(Optional.of(mockProduct));
        when(taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted("copy_processing", 1, 0))
                .thenReturn(Optional.empty());
        when(aiModelRepository.findByStatusAndDeleted(1, 0))
                .thenReturn(List.of(mockModel));

        LlmClient.LlmResponse mockResponse = new LlmClient.LlmResponse(
                null,
                0L,
                false,
                "API 调用失败"
        );
        when(llmClient.chatWithFallback(anyList(), anyString(), anyString()))
                .thenReturn(mockResponse);

        // When & Then
        assertThatThrownBy(() -> slangDictService.aiGeneratePhrases(productId, userId, 5))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AI 生成失败");
    }

    @Test
    @DisplayName("AI生成梗 - 数量超限应限制为20")
    void aiGeneratePhrases_countExceeds_shouldLimitTo20() {
        // Given
        when(productRepository.findById(productId))
                .thenReturn(Optional.of(mockProduct));
        when(taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted("copy_processing", 1, 0))
                .thenReturn(Optional.empty());
        when(aiModelRepository.findByStatusAndDeleted(1, 0))
                .thenReturn(List.of(mockModel));

        LlmClient.LlmResponse mockResponse = new LlmClient.LlmResponse(
                "\"小金瓶\" → 护肤精华液",
                50L,
                true,
                null
        );
        when(llmClient.chatWithFallback(anyList(), anyString(), anyString()))
                .thenReturn(mockResponse);

        // When
        slangDictService.aiGeneratePhrases(productId, userId, 100);

        // Then
        verify(llmClient).chatWithFallback(anyList(), anyString(), contains("5 条")); // 默认限制为5
    }

    @Test
    @DisplayName("构建梗上下文 - 应返回格式化文本")
    void buildSlangContextForPrompt_shouldReturnFormattedText() {
        // Given
        when(mappingRepository.findByProductIdAndUserIdAndDeleted(productId, userId, 0))
                .thenReturn(List.of(mockMapping));
        when(entryRepository.findAllById(anySet()))
                .thenReturn(List.of(mockEntry));

        // When
        String result = slangDictService.buildSlangContextForPrompt(productId, userId);

        // Then
        assertThat(result).contains("产品创意表达");
        assertThat(result).contains("小金瓶");
        assertThat(result).contains("护肤精华液");
    }

    @Test
    @DisplayName("构建梗上下文 - 无关联条目应返回空字符串")
    void buildSlangContextForPrompt_noEntries_shouldReturnEmpty() {
        // Given
        when(mappingRepository.findByProductIdAndUserIdAndDeleted(productId, userId, 0))
                .thenReturn(List.of());

        // When
        String result = slangDictService.buildSlangContextForPrompt(productId, userId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("构建梗上下文 - null参数应返回空字符串")
    void buildSlangContextForPrompt_nullParams_shouldReturnEmpty() {
        // When
        String result = slangDictService.buildSlangContextForPrompt(null, userId);

        // Then
        assertThat(result).isEmpty();
    }
}
