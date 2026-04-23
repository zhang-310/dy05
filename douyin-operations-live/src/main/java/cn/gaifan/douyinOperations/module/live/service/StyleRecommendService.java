package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.live.vo.StyleRecommendationVO;

import java.util.List;

/**
 * 智能风格推荐 #29：基于历史效果（live_script.effectiveness_score）按风格聚合，推荐最优风格
 */
public interface StyleRecommendService {

    /**
     * 按历史话术效果推荐风格（仅统计有 effectiveness_score 的话术）
     *
     * @param ownerId  所属用户，用于数据隔离
     * @param productId 可选，限定某产品下的使用记录
     * @param limit    返回条数，默认 10
     * @return 按平均效果分降序的风格列表
     */
    List<StyleRecommendationVO> recommendStyles(Long ownerId, Long productId, int limit);
}
