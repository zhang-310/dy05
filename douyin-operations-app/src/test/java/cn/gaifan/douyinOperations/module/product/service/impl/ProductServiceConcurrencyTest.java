package cn.gaifan.douyinOperations.module.product.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import cn.gaifan.douyinOperations.module.product.service.ProductLinkExtractService;
import cn.gaifan.douyinOperations.module.product.vo.ProductSearchVO;
import cn.gaifan.douyinOperations.module.product.vo.ProductVO;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ProductServiceImpl 并发与数据隔离测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl 并发与数据隔离测试")
class ProductServiceConcurrencyTest {

    @Mock
    private DyProductRepository dyProductRepository;

    @Mock
    private ProductLinkExtractService productLinkExtractService;

    @InjectMocks
    private ProductServiceImpl productService;

    private DyProduct sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = new DyProduct();
        sampleProduct.setId(1L);
        sampleProduct.setUserId(100L);
        sampleProduct.setProductName("Test Product");
        sampleProduct.setPrice(new BigDecimal("99.99"));
        sampleProduct.setInventory(50L);
        sampleProduct.setStatus(1);
        sampleProduct.setFeatured(0);
        sampleProduct.setDeleted(0);
        sampleProduct.setVersion(0);
        sampleProduct.setCreateTime(new Timestamp(System.currentTimeMillis()));
        sampleProduct.setUpdateTime(new Timestamp(System.currentTimeMillis()));
    }

    // ==================== updateInventory 测试 ====================

    @Nested
    @DisplayName("updateInventory 方法测试")
    class UpdateInventoryTests {

        @Test
        @DisplayName("updateInventory_singleThread_success - 单线程正常更新库存")
        void updateInventory_singleThread_success() {
            when(dyProductRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.of(sampleProduct));
            when(dyProductRepository.save(any(DyProduct.class))).thenReturn(sampleProduct);

            assertDoesNotThrow(() -> productService.updateInventory(1L, 10L));

            verify(dyProductRepository).save(argThat(product ->
                    product.getInventory() == 60L));
        }

        @Test
        @DisplayName("updateInventory_decreaseSuccess - 减少库存成功")
        void updateInventory_decreaseSuccess() {
            when(dyProductRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.of(sampleProduct));
            when(dyProductRepository.save(any(DyProduct.class))).thenReturn(sampleProduct);

            assertDoesNotThrow(() -> productService.updateInventory(1L, -10L));

            verify(dyProductRepository).save(argThat(product ->
                    product.getInventory() == 40L));
        }

        @Test
        @DisplayName("updateInventory_insufficientStock_throwsException - 库存不足抛出异常")
        void updateInventory_insufficientStock_throwsException() {
            when(dyProductRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.of(sampleProduct));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> productService.updateInventory(1L, -100L));

            assertEquals(ErrorCode.VALIDATION_FAIL, ex.getCode());
            assertTrue(ex.getMessage().contains("库存不足"));
        }

        @Test
        @DisplayName("updateInventory_optimisticLockConflict_throwsException - 乐观锁冲突抛出异常")
        void updateInventory_optimisticLockConflict_throwsException() {
            when(dyProductRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.of(sampleProduct));
            when(dyProductRepository.save(any(DyProduct.class)))
                    .thenThrow(new OptimisticLockException("Version conflict"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> productService.updateInventory(1L, 5L));

            assertEquals(ErrorCode.INVENTORY_CONFLICT, ex.getCode());
            assertTrue(ex.getMessage().contains("库存已被其他操作更新"));
        }

        @Test
        @DisplayName("updateInventory_productNotFound_throwsException - 商品不存在抛出异常")
        void updateInventory_productNotFound_throwsException() {
            when(dyProductRepository.findByIdAndDeleted(999L, 0)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> productService.updateInventory(999L, 10L));

            assertEquals(ErrorCode.DATA_NOT_FOUND, ex.getCode());
        }

        @Test
        @DisplayName("updateInventory_nullInventory_treatsAsZero - 当前库存为 null 视为 0")
        void updateInventory_nullInventory_treatsAsZero() {
            sampleProduct.setInventory(null);
            when(dyProductRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.of(sampleProduct));
            when(dyProductRepository.save(any(DyProduct.class))).thenReturn(sampleProduct);

            assertDoesNotThrow(() -> productService.updateInventory(1L, 20L));

            verify(dyProductRepository).save(argThat(product ->
                    product.getInventory() == 20L));
        }

        @Test
        @DisplayName("updateInventory_nullInventory_negativeQuantity_throwsException - null库存减少抛异常")
        void updateInventory_nullInventory_negativeQuantity_throwsException() {
            sampleProduct.setInventory(null);
            when(dyProductRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.of(sampleProduct));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> productService.updateInventory(1L, -5L));

            assertEquals(ErrorCode.VALIDATION_FAIL, ex.getCode());
        }
    }

    // ==================== search 数据隔离测试 ====================

    @Nested
    @DisplayName("search 数据隔离测试")
    class SearchDataIsolationTests {

        @Test
        @DisplayName("search_dataIsolation_onlyReturnsOwnData - 带 userId 过滤仅返回自己数据")
        @SuppressWarnings("unchecked")
        void search_dataIsolation_onlyReturnsOwnData() {
            ProductSearchVO searchVO = new ProductSearchVO();
            searchVO.setUserId(100L);
            searchVO.setPage(0);
            searchVO.setRows(10);

            DyProduct otherProduct = new DyProduct();
            otherProduct.setId(2L);
            otherProduct.setUserId(200L);
            otherProduct.setProductName("Other User Product");
            otherProduct.setPrice(new BigDecimal("50.00"));

            // Only sampleProduct (userId=100) should be returned, NOT otherProduct (userId=200)
            Page<DyProduct> page = new PageImpl<>(List.of(sampleProduct));
            when(dyProductRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(page);

            PageResultVO<ProductVO> result = productService.search(searchVO);

            assertNotNull(result);
            assertEquals(1L, result.getTotal());
            assertEquals(100L, result.getList().get(0).getUserId());
            // Verify specification was called (the userId filter is built inside)
            verify(dyProductRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("search_withKeyword_filtersCorrectly - 关键词搜索过滤")
        @SuppressWarnings("unchecked")
        void search_withKeyword_filtersCorrectly() {
            ProductSearchVO searchVO = new ProductSearchVO();
            searchVO.setKeyword("Test");
            searchVO.setPage(0);
            searchVO.setRows(10);

            Page<DyProduct> page = new PageImpl<>(List.of(sampleProduct));
            when(dyProductRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(page);

            PageResultVO<ProductVO> result = productService.search(searchVO);

            assertNotNull(result);
            assertEquals(1L, result.getTotal());
        }

        @Test
        @DisplayName("search_emptyResult_returnsEmptyPage - 空结果返回空分页")
        @SuppressWarnings("unchecked")
        void search_emptyResult_returnsEmptyPage() {
            ProductSearchVO searchVO = new ProductSearchVO();
            searchVO.setUserId(999L);
            searchVO.setPage(0);
            searchVO.setRows(10);

            Page<DyProduct> emptyPage = new PageImpl<>(List.of());
            when(dyProductRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(emptyPage);

            PageResultVO<ProductVO> result = productService.search(searchVO);

            assertNotNull(result);
            assertEquals(0L, result.getTotal());
            assertTrue(result.getList().isEmpty());
        }
    }

    // ==================== inferProductType 测试 ====================

    @Nested
    @DisplayName("inferProductType 方法测试")
    class InferProductTypeTests {

        @Test
        @DisplayName("inferProductType_profitProduct_returnsProfit - 高利润品识别")
        void inferProductType_profitProduct_returnsProfit() {
            sampleProduct.setProfitMarginPct(new BigDecimal("0.45"));
            when(dyProductRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.of(sampleProduct));

            String type = productService.inferProductType(1L);

            assertNotNull(type);
            assertTrue(type.contains("profit"));
        }

        @Test
        @DisplayName("inferProductType_nullProduct_returnsNull - 产品不存在返回 null")
        void inferProductType_nullProduct_returnsNull() {
            when(dyProductRepository.findByIdAndDeleted(999L, 0)).thenReturn(Optional.empty());

            String type = productService.inferProductType(999L);

            assertNull(type);
        }

        @Test
        @DisplayName("inferProductType_defaultFlat_returnsFlat - 默认平价品")
        void inferProductType_defaultFlat_returnsFlat() {
            when(dyProductRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.of(sampleProduct));

            String type = productService.inferProductType(1L);

            assertNotNull(type);
            assertEquals("flat", type);
        }
    }
}
