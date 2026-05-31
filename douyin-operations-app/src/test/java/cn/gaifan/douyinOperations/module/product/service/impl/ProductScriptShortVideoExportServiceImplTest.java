package cn.gaifan.douyinOperations.module.product.service.impl;

import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.entity.DyProductScript;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import cn.gaifan.douyinOperations.module.product.repository.DyProductScriptRepository;
import cn.gaifan.douyinOperations.module.product.repository.ProductScriptVersionRepository;
import cn.gaifan.douyinOperations.module.product.vo.ProductScriptExportToShortVideoResultVO;
import cn.gaifan.douyinOperations.module.product.vo.ProductScriptExportToShortVideoVO;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvProjectService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvScriptService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptSaveVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductScriptShortVideoExportServiceImpl 单元测试")
class ProductScriptShortVideoExportServiceImplTest {

    @Mock
    private DyProductRepository productRepository;
    @Mock
    private DyProductScriptRepository productScriptRepository;
    @Mock
    private ProductScriptVersionRepository productScriptVersionRepository;
    @Mock
    private SvScriptService svScriptService;
    @Mock
    private SvProjectService svProjectService;

    @InjectMocks
    private ProductScriptShortVideoExportServiceImpl service;

    @Test
    @DisplayName("导出商品激活话术应创建短视频脚本和项目")
    void exportToShortVideoProject_activeProductScript_shouldCreateScriptAndProject() {
        DyProduct product = new DyProduct();
        product.setId(7L);
        product.setUserId(1L);
        product.setProductName("修护面霜");
        product.setProductCategory("护肤");
        product.setPrice(new BigDecimal("99.00"));
        product.setAiSellingPoints("修护屏障\n敏感肌可用");
        product.setImageUrl("https://cdn.example.com/cream.jpg");

        DyProductScript script = new DyProductScript();
        script.setId(8L);
        script.setProductId(7L);
        script.setScriptContent("先讲敏感肌痛点，再给出修护方案。");
        script.setStyle("professional");
        script.setDuration(45);
        script.setPersonaId(9L);

        ProductScriptExportToShortVideoVO vo = new ProductScriptExportToShortVideoVO();
        vo.setProductId(7L);
        vo.setStyle("种草");
        vo.setDuration(60);

        when(productRepository.findByIdAndDeleted(7L, 0)).thenReturn(Optional.of(product));
        when(productScriptRepository.findByProductIdAndIsActiveAndDeleted(7L, true, 0)).thenReturn(List.of(script));
        when(svScriptService.save(any(SvScriptSaveVO.class), eq(1L))).thenReturn(100L);
        when(svProjectService.save(any(SvProjectSaveVO.class), eq(1L))).thenReturn(200L);

        ProductScriptExportToShortVideoResultVO result = service.exportToShortVideoProject(vo, 1L);

        assertThat(result.getScriptId()).isEqualTo(100L);
        assertThat(result.getProjectId()).isEqualTo(200L);
        assertThat(result.getProjectName()).isEqualTo("商品转短视频·修护面霜");

        ArgumentCaptor<SvScriptSaveVO> scriptCaptor = ArgumentCaptor.forClass(SvScriptSaveVO.class);
        verify(svScriptService).save(scriptCaptor.capture(), eq(1L));
        assertThat(scriptCaptor.getValue().getScriptType()).isEqualTo("soft_ad");
        assertThat(scriptCaptor.getValue().getGenerationType()).isEqualTo("from_product_script");
        assertThat(scriptCaptor.getValue().getTheme()).isEqualTo("product:7");
        assertThat(scriptCaptor.getValue().getStyle()).isEqualTo("种草");
        assertThat(scriptCaptor.getValue().getDuration()).isEqualTo(60);
        assertThat(scriptCaptor.getValue().getContent()).contains("修护屏障", "先讲敏感肌痛点");

        ArgumentCaptor<SvProjectSaveVO> projectCaptor = ArgumentCaptor.forClass(SvProjectSaveVO.class);
        verify(svProjectService).save(projectCaptor.capture(), eq(1L));
        assertThat(projectCaptor.getValue().getProjectType()).isEqualTo("soft_ad");
        assertThat(projectCaptor.getValue().getScriptId()).isEqualTo(100L);
        assertThat(projectCaptor.getValue().getDuration()).isEqualTo(60);
        assertThat(projectCaptor.getValue().getThumbnailUrl()).isEqualTo("https://cdn.example.com/cream.jpg");
        assertThat(projectCaptor.getValue().getRelatedProductIds()).containsExactly(7L);
    }

    @Test
    @DisplayName("无有效商品话术时应明确失败")
    void exportToShortVideoProject_noScript_shouldThrow() {
        DyProduct product = new DyProduct();
        product.setId(7L);
        product.setUserId(1L);
        product.setProductName("修护面霜");

        ProductScriptExportToShortVideoVO vo = new ProductScriptExportToShortVideoVO();
        vo.setProductId(7L);

        when(productRepository.findByIdAndDeleted(7L, 0)).thenReturn(Optional.of(product));
        when(productScriptRepository.findByProductIdAndIsActiveAndDeleted(7L, true, 0)).thenReturn(List.of());
        when(productScriptRepository.findByProductIdAndDeletedOrderByCreateTimeDesc(7L, 0)).thenReturn(List.of());

        assertThrows(BusinessException.class, () -> service.exportToShortVideoProject(vo, 1L));
    }
}
