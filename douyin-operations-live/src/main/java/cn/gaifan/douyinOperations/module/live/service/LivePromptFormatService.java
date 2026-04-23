package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.douyin.entity.DyPersona;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiGenerateVO;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;

import java.util.List;

/**
 * 直播话术 Prompt 格式化服务：风格展开、模板、产品类型、时段等描述构建。
 * 从 LivePromptBuilder 拆分。
 */
public interface LivePromptFormatService {

    String expandStyleForPrompt(String style);

    String formatProductTypeLabel(String productType);

    String getProductTypePromptHint(String productType);

    String buildDurationHintForProductType(String productType);

    String buildIpTypeDesc(String ipType);

    String buildIpGrowthStageDesc(String ipType, long followerCount, int operatingMonths);

    String buildTimeSlotDesc(String timeSlot);

    String buildScriptModuleDesc(String module);

    String buildMaterialTypeDesc(String materialType);

    String buildRetentionStrategyDesc(String strategy);

    String buildInteractionLevelDesc(String level);

    String buildHotKeywordsDesc(List<String> hotKeywords);

    String buildOpeningTemplate(LiveSession session, String personaName, String extra);

    String buildProductTemplate(Long productId, String extra);

    String buildTransitionTemplate(String extra, LiveAiGenerateVO vo);

    String buildClosingTemplate(LiveSession session, String personaName, String extra);

    String buildChatTemplate(LiveSession session, String personaName, String requirement, String extra);

    /** chat_2h 场次 product 槽位：时段注入 */
    String buildChat2hTimeSlotHint(String timeSlot);
}
