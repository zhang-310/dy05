package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.live.vo.LivePersonaSnapshotVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveProductSnapshotVO;

import java.util.Optional;

/**
 * 跨模块数据适配器：为直播模块提供商品/人设数据快照，避免直接引用 product/douyin Entity。
 * 实现在 live 模块内部，通过 Repository 查询后映射为 VO。
 */
public interface LiveCrossModuleAdapter {

    /**
     * 获取商品快照（从 dy_product 表读取并映射）
     */
    Optional<LiveProductSnapshotVO> getProductSnapshot(Long productId);

    /**
     * 获取人设快照（从 dy_persona 表读取并映射）
     */
    Optional<LivePersonaSnapshotVO> getPersonaSnapshot(Long personaId);
}
