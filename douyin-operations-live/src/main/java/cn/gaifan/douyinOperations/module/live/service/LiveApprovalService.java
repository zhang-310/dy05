package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.live.entity.LiveApprovalLog;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;

import java.util.List;

/**
 * 直播话术审批服务
 */
public interface LiveApprovalService {

    /**
     * 提交场次话术审批（将场次下所有话术 approvalStatus 设为 1=待审核）
     */
    void submitForApproval(Long sessionId, Long operatorId);

    /**
     * 审批通过（设 approvalStatus=2）
     */
    void approve(Long sessionId, Long scriptId, Long operatorId, String comment);

    /**
     * 审批拒绝（设 approvalStatus=3）
     */
    void reject(Long sessionId, Long scriptId, Long operatorId, String comment);

    /**
     * 获取场次的审批历史
     */
    List<LiveApprovalLog> getApprovalHistory(Long sessionId);

    /**
     * 获取待审批场次列表
     */
    List<LiveSession> getPendingApprovals(Long operatorId);
}
