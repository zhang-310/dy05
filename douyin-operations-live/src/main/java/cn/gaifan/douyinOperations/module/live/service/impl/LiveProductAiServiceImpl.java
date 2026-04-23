package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.live.service.LiveAiService;
import cn.gaifan.douyinOperations.module.live.vo.ProductScriptGenerateVO;
import cn.gaifan.douyinOperations.module.live.vo.ProductScriptResultVO;
import cn.gaifan.douyinOperations.module.product.service.ProductAiService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 产品话术 AI 生成服务实现，委托 LiveAiService。
 * 解耦 Product 模块对 Live 模块的直接依赖。
 */
@Service
@Primary
public class LiveProductAiServiceImpl implements ProductAiService {

    @Resource
    private LiveAiService liveAiService;

    @Override
    public ProductScriptAiResult generateScript(Long productId, String scriptType, String style,
            Long personaId, int duration, Long userId, Boolean useKbRef, String scene, List<String> kbCategories) {
        ProductScriptGenerateVO vo = new ProductScriptGenerateVO();
        vo.setProductId(productId);
        vo.setScriptType(scriptType);
        vo.setStyle(style);
        vo.setPersonaId(personaId);
        vo.setDuration(duration);
        vo.setUseKbRef(useKbRef);
        vo.setScene(scene);
        vo.setKbCategories(kbCategories);

        ProductScriptResultVO result = liveAiService.generateProductScript(vo, userId);
        return new ProductScriptAiResult(
                result.getScriptContent(),
                result.getTokenUsage() != null ? result.getTokenUsage() : 0);
    }
}
