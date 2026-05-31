package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.AiPromptTemplate;
import cn.gaifan.douyinOperations.module.ai.vo.AiPromptTemplateSaveVO;
import cn.gaifan.douyinOperations.module.ai.vo.AiPromptTemplateSearchVO;

public interface PromptTemplateService {

    /**
     * 分页搜索模板
     */
    PageResultVO<AiPromptTemplate> search(AiPromptTemplateSearchVO searchVO);

    /**
     * 根据 ID 获取模板
     */
    AiPromptTemplate getById(Long id);

    /**
     * 保存模板（新增或更新）
     */
    AiPromptTemplate save(AiPromptTemplateSaveVO saveVO);

    /**
     * 逻辑删除模板
     */
    void delete(Long id);

    /**
     * 获取激活模板，按回退链：用户模板 → 系统模板 → null
     *
     * @param templateCode 模板编码
     * @param variantName  变体名称
     * @param ownerId      用户 ID
     * @return 匹配的模板或 null
     */
    AiPromptTemplate getActiveTemplate(String templateCode, String variantName, Long ownerId);

    /**
     * 递增使用次数
     */
    void incrementUsage(Long templateId);

    /**
     * 递增使用次数并更新最后使用时间
     */
    void incrementUsageCount(Long templateId);

    /**
     * 更新效果评分（滑动平均）
     */
    void updateEffectivenessScore(Long templateId, float score);
}
