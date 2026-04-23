package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveScriptApprovalSearchVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveScriptApprovalVO;

import java.util.List;

public interface LiveScriptApprovalService {

    /** 提交话术审核 */
    LiveScriptApprovalVO submit(Long scriptId, String comments, Long userId);

    /** 批量提交场次下所有话术审核（整场提交，统一替代 LiveApprovalController.submit） */
    List<LiveScriptApprovalVO> submitBySession(Long sessionId, String comments, Long userId);

    /** 审批（通过/拒绝） */
    LiveScriptApprovalVO review(Long approvalId, String action, String comments, Long reviewerId);

    /** 撤回审核 */
    void revoke(Long scriptId, Long userId);

    /** 分页查询审核记录 */
    PageResultVO<LiveScriptApprovalVO> search(LiveScriptApprovalSearchVO vo);

    /** 查询话术的审核历史 */
    List<LiveScriptApprovalVO> history(Long scriptId);
}
