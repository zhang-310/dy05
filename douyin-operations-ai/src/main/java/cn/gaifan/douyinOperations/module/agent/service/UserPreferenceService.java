package cn.gaifan.douyinOperations.module.agent.service;

import java.util.List;

/**
 * 用户偏好与长期记忆服务
 * 话术迭代时记录 instruction_used、script_type；REFINE_SUGGESTIONS 优先展示用户常用
 */
public interface UserPreferenceService {

    /**
     * 记录偏好使用
     */
    void record(Long userId, String key, String value);

    /**
     * 获取用户某类偏好 top N（按 usage_count、last_used_at 排序）
     */
    List<String> getTopPreferences(Long userId, String key, int limit);
}
