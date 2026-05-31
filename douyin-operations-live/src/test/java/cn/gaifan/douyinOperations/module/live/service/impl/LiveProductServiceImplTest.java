package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.vo.LiveProductBatchAddItemVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveProductSaveVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveProductSearchVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveProductVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("直播商品服务测试")
class LiveProductServiceImplTest {

    @Mock
    private LiveProductRepository liveProductRepository;

    @InjectMocks
    private LiveProductServiceImpl liveProductService;

    @Test
    @DisplayName("分页查询 - 数据范围为空直接返回空页")
    void searchEmptyVisibleSessionIdsReturnsEmptyPage() {
        LiveProductSearchVO vo = new LiveProductSearchVO();
        vo.setSessionIds(Collections.emptyList());
        vo.setPage(0);
        vo.setRows(20);

        PageResultVO<LiveProductVO> result = liveProductService.search(vo);

        assertThat(result.getTotal()).isZero();
        assertThat(result.getList()).isEmpty();
        verifyNoInteractions(liveProductRepository);
    }

    @Test
    @DisplayName("分页查询 - 按场次返回新增字段")
    void searchBySessionReturnsProductContractFields() {
        LiveProduct product = product(31L, 18L, 9001L);
        product.setProductType("hot");
        product.setScriptSource("session");
        product.setProductScriptId(77L);
        product.setRevenue(new BigDecimal("4999.50"));

        when(liveProductRepository.findBySessionId(eq(18L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));

        LiveProductSearchVO vo = new LiveProductSearchVO();
        vo.setSessionId(18L);
        vo.setPage(0);
        vo.setRows(20);

        PageResultVO<LiveProductVO> result = liveProductService.search(vo);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList().get(0).getProductType()).isEqualTo("hot");
        assertThat(result.getList().get(0).getScriptSource()).isEqualTo("session");
        assertThat(result.getList().get(0).getProductScriptId()).isEqualTo(77L);
        assertThat(result.getList().get(0).getRevenue()).isEqualByComparingTo("4999.50");
        verify(liveProductRepository).findBySessionId(eq(18L), any(Pageable.class));
    }

    @Test
    @DisplayName("保存 - 新建商品关系写入真实契约字段")
    void saveCreatePersistsContractFields() {
        LiveProductSaveVO vo = new LiveProductSaveVO();
        vo.setSessionId(18L);
        vo.setProductId(9001L);
        vo.setProductName("修护精华");
        vo.setSaleQuantity(12);
        vo.setPosition(2);
        vo.setProductType(" hot ");
        vo.setScriptSource(" session ");
        vo.setProductScriptId(77L);

        when(liveProductRepository.save(any(LiveProduct.class))).thenAnswer(invocation -> {
            LiveProduct saved = invocation.getArgument(0);
            saved.setId(31L);
            return saved;
        });

        long id = liveProductService.save(vo);

        assertThat(id).isEqualTo(31L);
        verify(liveProductRepository).save(org.mockito.ArgumentMatchers.argThat(product ->
                product.getSessionId().equals(18L)
                        && product.getProductId().equals(9001L)
                        && "修护精华".equals(product.getProductName())
                        && Integer.valueOf(12).equals(product.getSaleQuantity())
                        && Integer.valueOf(2).equals(product.getPosition())
                        && "hot".equals(product.getProductType())
                        && "session".equals(product.getScriptSource())
                        && Long.valueOf(77L).equals(product.getProductScriptId())
        ));
    }

    @Test
    @DisplayName("保存 - 更新商品关系保留不可写字段并更新契约字段")
    void saveUpdatePersistsContractFields() {
        LiveProduct existing = product(31L, 18L, 9001L);
        existing.setRevenue(new BigDecimal("88.80"));
        LiveProductSaveVO vo = new LiveProductSaveVO();
        vo.setId(31L);
        vo.setSessionId(18L);
        vo.setProductId(9001L);
        vo.setProductName("修护精华升级版");
        vo.setSaleQuantity(13);
        vo.setPosition(3);
        vo.setProductType("profit");
        vo.setProductScriptId(88L);

        when(liveProductRepository.findById(31L)).thenReturn(Optional.of(existing));
        when(liveProductRepository.save(any(LiveProduct.class))).thenAnswer(invocation -> invocation.getArgument(0));

        long id = liveProductService.save(vo);

        assertThat(id).isEqualTo(31L);
        verify(liveProductRepository).save(org.mockito.ArgumentMatchers.argThat(product ->
                product.getId().equals(31L)
                        && "修护精华升级版".equals(product.getProductName())
                        && Integer.valueOf(13).equals(product.getSaleQuantity())
                        && Integer.valueOf(3).equals(product.getPosition())
                        && "profit".equals(product.getProductType())
                        && "session".equals(product.getScriptSource())
                        && Long.valueOf(88L).equals(product.getProductScriptId())
                        && product.getRevenue().compareTo(new BigDecimal("88.80")) == 0
        ));
    }

    @Test
    @DisplayName("保存 - 无效场次或商品 ID 拒绝保存")
    void saveRejectsInvalidIds() {
        LiveProductSaveVO missingSession = new LiveProductSaveVO();
        missingSession.setProductId(9001L);

        assertThatThrownBy(() -> liveProductService.save(missingSession))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAIL)
                .hasMessageContaining("直播场次 ID 无效");

        LiveProductSaveVO missingProduct = new LiveProductSaveVO();
        missingProduct.setSessionId(18L);

        assertThatThrownBy(() -> liveProductService.save(missingProduct))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAIL)
                .hasMessageContaining("产品 ID 无效");

        verify(liveProductRepository, never()).save(any());
    }

    @Test
    @DisplayName("批量添加 - 使用最大位次并保留商品类型和话术 ID")
    void batchAddPersistsContractFieldsAndPosition() {
        LiveProduct existing = product(11L, 18L, 8001L);
        existing.setPosition(4);
        when(liveProductRepository.findBySessionIdOrderByPositionAscIdAsc(18L)).thenReturn(List.of(existing));
        when(liveProductRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        LiveProductBatchAddItemVO item = new LiveProductBatchAddItemVO();
        item.setProductId(9001L);
        item.setProductName("修护精华");
        item.setProductType("hot");
        item.setProductScriptId(77L);

        int count = liveProductService.batchAdd(18L, List.of(item), 1L);

        assertThat(count).isEqualTo(1);
        ArgumentCaptor<List<LiveProduct>> captor = ArgumentCaptor.forClass(List.class);
        verify(liveProductRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        LiveProduct saved = captor.getValue().get(0);
        assertThat(saved.getSessionId()).isEqualTo(18L);
        assertThat(saved.getProductId()).isEqualTo(9001L);
        assertThat(saved.getProductName()).isEqualTo("修护精华");
        assertThat(saved.getProductType()).isEqualTo("hot");
        assertThat(saved.getProductScriptId()).isEqualTo(77L);
        assertThat(saved.getPosition()).isEqualTo(5);
    }

    private static LiveProduct product(Long id, Long sessionId, Long productId) {
        LiveProduct product = new LiveProduct();
        product.setId(id);
        product.setSessionId(sessionId);
        product.setProductId(productId);
        product.setProductName("修护精华");
        product.setSaleQuantity(12);
        product.setRevenue(BigDecimal.ZERO);
        product.setPosition(1);
        product.setScriptSource("session");
        product.setDeleted(0);
        product.setCreateTime(new Timestamp(System.currentTimeMillis()));
        return product;
    }
}
