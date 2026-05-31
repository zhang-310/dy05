package cn.gaifan.douyinOperations.module.product.service.impl;

import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.entity.DyProductScript;
import cn.gaifan.douyinOperations.module.product.entity.ProductScriptVersion;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import cn.gaifan.douyinOperations.module.product.repository.DyProductScriptRepository;
import cn.gaifan.douyinOperations.module.product.repository.ProductScriptSnapshotRepository;
import cn.gaifan.douyinOperations.module.product.repository.ProductScriptVersionRepository;
import cn.gaifan.douyinOperations.module.product.vo.EnsureOptimizationVersionResultVO;
import cn.gaifan.douyinOperations.module.product.vo.EnsureOptimizationVersionVO;
import cn.gaifan.douyinOperations.module.product.vo.ProductScriptVersionVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductScriptVersionServiceImplTest {

    @Mock
    private ProductScriptVersionRepository versionRepository;

    @Mock
    private DyProductScriptRepository scriptRepository;

    @Mock
    private DyProductRepository productRepository;

    @Mock
    private ProductScriptSnapshotRepository snapshotRepository;

    @InjectMocks
    private ProductScriptVersionServiceImpl service;

    @Test
    @DisplayName("listByProductId 按 owner/product/deleted 查询并保持版本倒序")
    void listByProductId_usesOwnerScopedOrderedRepositoryQuery() {
        ProductScriptVersion version5 = ProductScriptVersion.builder()
                .id(31L)
                .productId(9L)
                .scriptId(8L)
                .versionNumber(5)
                .content("第五版话术")
                .style("warm")
                .ownerId(1L)
                .isActive(true)
                .isRecommended(false)
                .usageCount(7)
                .deleted(0)
                .build();
        ProductScriptVersion version3 = ProductScriptVersion.builder()
                .id(21L)
                .productId(9L)
                .scriptId(8L)
                .versionNumber(3)
                .content("第三版话术")
                .style("professional")
                .ownerId(1L)
                .isActive(false)
                .isRecommended(true)
                .usageCount(2)
                .deleted(0)
                .build();
        when(versionRepository.findByOwnerIdAndProductIdAndDeletedOrderByVersionNumberDesc(1L, 9L, 0))
                .thenReturn(List.of(version5, version3));

        List<ProductScriptVersionVO> result = service.listByProductId(9L, 1L);

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting("versionNumber")
                .containsExactly(5, 3);
        verify(versionRepository).findByOwnerIdAndProductIdAndDeletedOrderByVersionNumberDesc(1L, 9L, 0);
    }

    @Test
    @DisplayName("ensureOptimizationVersion 复用已有商品话术镜像")
    void ensureOptimizationVersion_reusesExistingMirror() {
        EnsureOptimizationVersionVO vo = new EnsureOptimizationVersionVO();
        vo.setScriptId(8L);

        DyProductScript script = createScript();
        DyProduct product = createProduct();
        ProductScriptVersion existing = ProductScriptVersion.builder()
                .id(21L)
                .productId(9L)
                .scriptId(8L)
                .versionNumber(3)
                .ownerId(1L)
                .deleted(0)
                .build();

        when(scriptRepository.findById(8L)).thenReturn(Optional.of(script));
        when(productRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(product));
        when(versionRepository.findFirstByScriptIdAndOwnerIdAndDeletedOrderByVersionNumberDesc(8L, 1L, 0))
                .thenReturn(Optional.of(existing));

        EnsureOptimizationVersionResultVO result = service.ensureOptimizationVersion(vo, 1L);

        assertThat(result.getScriptId()).isEqualTo(8L);
        assertThat(result.getScriptVersionId()).isEqualTo(21L);
        assertThat(result.isCreated()).isFalse();
        verifyNoMoreInteractions(snapshotRepository);
    }

    @Test
    @DisplayName("ensureOptimizationVersion 新建商品话术优化镜像")
    void ensureOptimizationVersion_createsMirrorWhenMissing() {
        EnsureOptimizationVersionVO vo = new EnsureOptimizationVersionVO();
        vo.setScriptId(8L);

        ProductScriptVersion saved = ProductScriptVersion.builder()
                .id(22L)
                .productId(9L)
                .scriptId(8L)
                .versionNumber(4)
                .ownerId(1L)
                .deleted(0)
                .build();

        when(scriptRepository.findById(8L)).thenReturn(Optional.of(createScript()));
        when(productRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(createProduct()));
        when(versionRepository.findFirstByScriptIdAndOwnerIdAndDeletedOrderByVersionNumberDesc(8L, 1L, 0))
                .thenReturn(Optional.empty());
        when(versionRepository.findMaxVersionNumberByProductId(9L)).thenReturn(3);
        when(versionRepository.save(any(ProductScriptVersion.class))).thenReturn(saved);

        EnsureOptimizationVersionResultVO result = service.ensureOptimizationVersion(vo, 1L);

        assertThat(result.getScriptVersionId()).isEqualTo(22L);
        assertThat(result.isCreated()).isTrue();
        verify(versionRepository).save(any(ProductScriptVersion.class));
    }

    private DyProductScript createScript() {
        DyProductScript script = new DyProductScript();
        script.setId(8L);
        script.setProductId(9L);
        script.setScriptContent("修护精华话术");
        script.setStyle("professional");
        script.setIsActive(true);
        return script;
    }

    private DyProduct createProduct() {
        DyProduct product = new DyProduct();
        product.setId(9L);
        product.setUserId(1L);
        return product;
    }
}
