package cn.gaifan.douyinOperations.module.shortvideo.service;

import java.util.List;
import java.util.Map;

/**
 * 发布时间推荐 Service
 */
public interface PublishTimeRecommendationService {

    /**
     * 获取账号的推荐发布时间段
     *
     * @param accountId        抖音账号 ID
     * @param visibleOwnerIds  可见用户 ID 列表（用于校验账号归属），null 表示管理员不限制
     * @return 推荐时段列表，每项含 dayOfWeek、hourOfDay、avgViewCount、videoCount
     */
    List<Map<String, Object>> getRecommendedTimes(Long accountId, List<Long> visibleOwnerIds);
}
