package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.douyin.entity.DyPersona;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.vo.LivePersonaSnapshotVO;

/**
 * 直播话术 Prompt 上下文构建器：人设描述、主播策略、产品类型提示。
 * 从 LivePromptBuilder 拆分。
 */
public interface LivePromptContextBuilder {

    String buildPersonaDesc(DyPersona persona);

    String buildPersonaDesc(DyPersona persona, LiveSession session);

    /** 基于人设快照 VO 构建描述（新代码优先使用此方法） */
    default String buildPersonaDesc(LivePersonaSnapshotVO snapshot) {
        return snapshot != null ? snapshot.getPersonaName() + " — " + snapshot.getTone() : "";
    }

    String buildHostPersonaStrategy(String hostCode);

    String buildHostPersonaStrategy(String hostCode, String sessionType);

    /** 根据 productId + sessionId 从 live_product 解析 productType，返回话术侧重提示 */
    String getProductTypeHintForPrompt(Long productId, Long sessionId);
}
