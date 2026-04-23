package cn.gaifan.douyinOperations.module.product.service.impl;

import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import cn.gaifan.douyinOperations.module.product.vo.ProductSaveVO;
import cn.gaifan.douyinOperations.module.product.vo.ProductSearchVO;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl 产品服务测试")
class ProductServiceImplTest {

    @InjectMocks
    private ProductServiceImpl productService;

    @Mock
    private DyProductRepository dyProductRepository;

    private DyProduct buildProduct(Long id, String name) {
        DyProduct p = new DyProduct();
        p.setId(id);
        p.setProductName(name);
        p.setUserId(1L);
        p.setPrice(java.math.BigDecimal.TEN);
        p.setDeleted(0);
        return p;
    }

    @Nested
    @DisplayName("search 分页搜索")
    class SearchTests {

        @Test
        void search_shouldReturnPage() {
            ProductSearchVO vo = new ProductSearchVO();
            vo.setPage(0);
            vo.setRows(10);
            when(dyProductRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(buildProduct(1L, "测试商品")), org.springframework.data.domain.PageRequest.of(0, 10), 1));

            var result = productService.search(vo);

            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getList()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getById")
    class GetByIdTests {

        @Test
        void getById_nullId_shouldThrow() {
            assertThatThrownBy(() -> productService.getById(null))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void getById_notFound_shouldThrow() {
            when(dyProductRepository.findByIdAndDeleted(999L, 0)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getById(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("不存在");
        }

        @Test
        void getById_found_shouldReturnVO() {
            DyProduct p = buildProduct(1L, "商品A");
            when(dyProductRepository.findByIdAndDeleted(1L, 0)).thenReturn(Optional.of(p));

            var result = productService.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getProductName()).isEqualTo("商品A");
        }
    }
}
