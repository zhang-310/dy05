package cn.gaifan.douyinOperations.module.abtest.service;

import cn.gaifan.douyinOperations.module.abtest.vo.SessionTemplateAssignVO;

/**
 * 直播场次模板结构 A/B：按账号维度分配模板变体（实验 target：live_account + accountId）
 */
public interface SessionTemplateAbService {

    /**
     * 新建场次且未显式指定 templateId 时调用：若存在运行中实验则随机模板变体
     *
     * @param ownerId         租户用户
     * @param accountId       抖音账号 ID（与实验 target_entity_id 对齐）
     * @param userFingerprint 与 script_style 一致，用于 view 去重
     */
    SessionTemplateAssignVO assignTemplateForNewSession(Long ownerId, Long accountId, String userFingerprint);
}
