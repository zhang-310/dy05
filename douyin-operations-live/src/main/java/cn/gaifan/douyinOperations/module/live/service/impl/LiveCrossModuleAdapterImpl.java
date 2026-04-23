package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.douyin.entity.DyPersona;
import cn.gaifan.douyinOperations.module.douyin.repository.DyPersonaRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveCrossModuleAdapter;
import cn.gaifan.douyinOperations.module.live.vo.LivePersonaSnapshotVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveProductSnapshotVO;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;
import cn.gaifan.douyinOperations.module.product.repository.DyProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 跨模块适配器实现：集中封装所有 live → product/douyin Entity 引用。
 * 新代码应通过此适配器获取快照 VO，而非直接 import Entity。
 */
@Component
public class LiveCrossModuleAdapterImpl implements LiveCrossModuleAdapter {

    @Autowired(required = false)
    private DyProductRepository productRepository;

    @Autowired(required = false)
    private DyPersonaRepository personaRepository;

    @Override
    public Optional<LiveProductSnapshotVO> getProductSnapshot(Long productId) {
        if (productId == null || productRepository == null) return Optional.empty();
        return productRepository.findById(productId).map(this::toProductSnapshot);
    }

    @Override
    public Optional<LivePersonaSnapshotVO> getPersonaSnapshot(Long personaId) {
        if (personaId == null || personaRepository == null) return Optional.empty();
        return personaRepository.findById(personaId).map(this::toPersonaSnapshot);
    }

    private LiveProductSnapshotVO toProductSnapshot(DyProduct p) {
        return LiveProductSnapshotVO.builder()
                .id(p.getId())
                .productName(p.getProductName())
                .description(p.getDescription())
                .price(p.getPrice())
                .costPrice(p.getCostPrice())
                .productCategory(p.getProductCategory())
                .imageUrl(p.getImageUrl())
                .aiSellingPoints(p.getAiSellingPoints())
                .profitMarginPct(p.getProfitMarginPct())
                .lossPerUnit(p.getLossPerUnit())
                .controlStrategy(p.getControlStrategy())
                .build();
    }

    private LivePersonaSnapshotVO toPersonaSnapshot(DyPersona p) {
        return LivePersonaSnapshotVO.builder()
                .id(p.getId())
                .personaName(p.getPersonaName())
                .tone(p.getTone())
                .localFlavor(p.getLocalFlavor())
                .personaTraits(p.getPersonaTraits())
                .ipType(p.getIpType())
                .ageRange(p.getAgeRange())
                .positioningTags(p.getPositioningTags())
                .liveStyle(p.getLiveStyle())
                .contentRatio(p.getContentRatio())
                .build();
    }
}
