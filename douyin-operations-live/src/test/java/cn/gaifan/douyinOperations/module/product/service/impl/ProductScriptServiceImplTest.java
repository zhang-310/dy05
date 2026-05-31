package cn.gaifan.douyinOperations.module.product.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.event.ProductScriptUpdatedEvent;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.entity.DyProductScript;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import cn.gaifan.douyinOperations.module.product.repository.DyProductScriptRepository;
import cn.gaifan.douyinOperations.module.product.service.ComplianceService;
import cn.gaifan.douyinOperations.module.product.service.ProductAiService;
import cn.gaifan.douyinOperations.module.product.service.ProductScriptRateLimitService;
import cn.gaifan.douyinOperations.module.product.service.ScriptVersionHistoryService;
import cn.gaifan.douyinOperations.module.product.vo.ProductScriptSaveVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("商品话术服务测试")
class ProductScriptServiceImplTest {

    @Mock
    private DyProductScriptRepository scriptRepository;
    @Mock
    private DyProductRepository productRepository;
    @Mock
    private ProductAiService productAiService;
    @Mock
    private ComplianceService complianceService;
    @Mock
    private ProductScriptRateLimitService rateLimitService;
    @Mock
    private ScriptVersionHistoryService scriptVersionHistoryService;
    @Mock
    private ObjectProvider<ProductScriptServiceImpl> selfProvider;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProductScriptServiceImpl service;

    @Test
    @DisplayName("列表 - 校验商品归属后只返回未删除话术")
    void listScriptsChecksProductOwnership() {
        DyProduct product = product(9L, 1L);
        DyProductScript script = script(8L, 9L, "professional", false);
        when(productRepository.findById(9L)).thenReturn(Optional.of(product));
        when(scriptRepository.findByProductIdAndDeletedOrderByCreateTimeDesc(9L, 0)).thenReturn(List.of(script));

        List<DyProductScript> result = service.listScripts(9L, 1L);

        assertThat(result).containsExactly(script);
        verify(scriptRepository).findByProductIdAndDeletedOrderByCreateTimeDesc(9L, 0);
    }

    @Test
    @DisplayName("列表 - 非 owner 拒绝访问")
    void listScriptsRejectsForeignProduct() {
        when(productRepository.findById(9L)).thenReturn(Optional.of(product(9L, 2L)));

        assertThatThrownBy(() -> service.listScripts(9L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN)
                .hasMessageContaining("无权限访问此产品");

        verify(scriptRepository, never()).findByProductIdAndDeletedOrderByCreateTimeDesc(any(), any());
    }

    @Test
    @DisplayName("激活 - 同商品同类型同风格唯一激活")
    void activateScriptDeactivatesSameStyleAndSavesCurrent() {
        DyProductScript script = script(8L, 9L, "professional", false);
        when(scriptRepository.findById(8L)).thenReturn(Optional.of(script));
        when(productRepository.findById(9L)).thenReturn(Optional.of(product(9L, 1L)));

        service.activateScript(8L, 1L);

        verify(scriptRepository).deactivateByProductIdAndScriptTypeAndStyle(9L, "formal", "professional");
        verify(scriptRepository).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.getId().equals(8L) && Boolean.TRUE.equals(saved.getIsActive())
        ));
    }

    @Test
    @DisplayName("删除 - 软删除且不物理删除")
    void deleteScriptSoftDeletes() {
        DyProductScript script = script(8L, 9L, "professional", true);
        when(scriptRepository.findById(8L)).thenReturn(Optional.of(script));
        when(productRepository.findById(9L)).thenReturn(Optional.of(product(9L, 1L)));

        service.deleteScript(8L, 1L);

        verify(scriptRepository).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.getId().equals(8L) && Integer.valueOf(1).equals(saved.getDeleted())
        ));
        verify(scriptRepository, never()).delete(any(DyProductScript.class));
    }

    @Test
    @DisplayName("保存 - 手工话术生成版本并发布更新事件")
    void saveScriptCreatesVersionAndPublishesEvent() {
        ProductScriptSaveVO vo = new ProductScriptSaveVO();
        vo.setProductId(9L);
        vo.setScriptType("formal");
        vo.setScriptContent("手工话术");
        vo.setStyle(" professional ");
        vo.setDuration(60);
        vo.setIsActive(true);
        vo.setSource("manual");

        when(productRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(product(9L, 1L)));
        when(scriptRepository.findMaxVersionByProductIdAndScriptTypeAndStyle(9L, "formal", "professional"))
                .thenReturn(2);
        when(scriptRepository.save(any(DyProductScript.class))).thenAnswer(invocation -> {
            DyProductScript saved = invocation.getArgument(0);
            saved.setId(18L);
            return saved;
        });

        DyProductScript result = service.saveScript(vo, 1L);

        assertThat(result.getId()).isEqualTo(18L);
        verify(scriptRepository).deactivateByProductIdAndScriptTypeAndStyle(9L, "formal", "professional");
        verify(scriptRepository).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.getProductId().equals(9L)
                        && "formal".equals(saved.getScriptType())
                        && "手工话术".equals(saved.getScriptContent())
                        && "professional".equals(saved.getStyle())
                        && Integer.valueOf(3).equals(saved.getVersion())
                        && Boolean.TRUE.equals(saved.getIsActive())
                        && "manual".equals(saved.getSource())
        ));
        verify(scriptVersionHistoryService).saveHistory(any(DyProductScript.class), org.mockito.ArgumentMatchers.eq(1L));
        verify(eventPublisher).publishEvent(any(ProductScriptUpdatedEvent.class));
    }

    private static DyProduct product(Long id, Long userId) {
        DyProduct product = new DyProduct();
        product.setId(id);
        product.setUserId(userId);
        product.setProductName("修护精华");
        return product;
    }

    private static DyProductScript script(Long id, Long productId, String style, boolean active) {
        DyProductScript script = new DyProductScript();
        script.setId(id);
        script.setProductId(productId);
        script.setScriptType("formal");
        script.setStyle(style);
        script.setScriptContent("修护精华话术");
        script.setVersion(1);
        script.setIsActive(active);
        script.setDeleted(0);
        return script;
    }
}
