package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.vo.LiveProductSearchVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveProductVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LiveProductServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LiveProductServiceImpl 单元测试")
class LiveProductServiceImplTest {

    @Mock
    private LiveProductRepository liveProductRepository;
    @Mock
    private LiveScriptRepository liveScriptRepository;
    @Mock
    private LiveSessionRepository liveSessionRepository;

    @InjectMocks
    private LiveProductServiceImpl liveProductService;

    private LiveProduct sampleProduct;
    private static final Long TEST_SESSION_ID = 100L;
    private static final Long TEST_PRODUCT_ID = 10L;

    @BeforeEach
    void setUp() {
        sampleProduct = new LiveProduct();
        sampleProduct.setId(1L);
        sampleProduct.setSessionId(TEST_SESSION_ID);
        sampleProduct.setProductId(TEST_PRODUCT_ID);
        sampleProduct.setProductName("测试商品");
        sampleProduct.setPosition(0);
        sampleProduct.setDeleted(0);
        sampleProduct.setCreateTime(new Timestamp(System.currentTimeMillis()));
    }

    @Nested
    @DisplayName("search 搜索")
    class SearchTests {

        @Test
        @DisplayName("search_bySessionId_shouldReturnPage")
        void search_bySessionId_shouldReturnPage() {
            LiveProductSearchVO vo = new LiveProductSearchVO();
            vo.setPage(0);
            vo.setRows(10);
            vo.setSessionId(TEST_SESSION_ID);

            when(liveProductRepository.findBySessionId(eq(TEST_SESSION_ID), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(sampleProduct), org.springframework.data.domain.PageRequest.of(0, 10), 1));

            PageResultVO<LiveProductVO> result = liveProductService.search(vo);

            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getList()).hasSize(1);
            assertThat(result.getList().get(0).getProductName()).isEqualTo("测试商品");
        }

        @Test
        @DisplayName("search_noSessionId_shouldThrow")
        void search_noSessionId_shouldThrow() {
            LiveProductSearchVO vo = new LiveProductSearchVO();
            vo.setPage(0);
            vo.setRows(10);

            BusinessException ex = assertThrows(BusinessException.class, () -> liveProductService.search(vo));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.VALIDATION_FAIL);
        }
    }

    @Nested
    @DisplayName("getById")
    class GetByIdTests {

        @Test
        @DisplayName("getById_valid_shouldReturn")
        void getById_valid_shouldReturn() {
            when(liveProductRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

            LiveProductVO result = liveProductService.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getProductName()).isEqualTo("测试商品");
        }

        @Test
        @DisplayName("getById_notFound_shouldThrow")
        void getById_notFound_shouldThrow() {
            when(liveProductRepository.findById(999L)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class, () -> liveProductService.getById(999L));
            assertThat(ex.getCode()).isEqualTo(ErrorCode.DATA_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("delete_valid_shouldCallRepository")
        void delete_valid_shouldCallRepository() {
            liveProductService.delete(1L);

            verify(liveProductRepository).deleteById(1L);
        }
    }
}
