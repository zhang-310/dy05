package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.live.entity.LiveApprovalLog;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveApprovalLogRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveApprovalService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 直播话术审批服务实现
 */
@Service
public class LiveApprovalServiceImpl implements LiveApprovalService {

    private static final Logger log = LoggerFactory.getLogger(LiveApprovalServiceImpl.class);

    @Resource
    private LiveApprovalLogRepository approvalLogRepository;

    @Resource
    private LiveScriptRepository liveScriptRepository;

    @Resource
    private LiveSessionRepository liveSessionRepository;

    @Override
    @Transactional
    public void submitForApproval(Long sessionId, Long operatorId) {
        LiveSession session = liveSessionRepository.findByIdAndDeleted(sessionId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "场次不存在"));

        // 所有权校验：只有场次创建者可提交审批
        if (!session.getUserId().equals(operatorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此场次");
        }

        // 获取场次下所有话术，设为待审核
        List<LiveScript> scripts = liveScriptRepository.findBySessionIdAndDeleted(sessionId, 0);
        if (scripts.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "场次下无话术，无法提交审批");
        }

        for (LiveScript script : scripts) {
            script.setApprovalStatus(1); // 待审核
        }
        liveScriptRepository.saveAll(scripts);

        // 记录审批日志
        LiveApprovalLog logEntry = new LiveApprovalLog();
        logEntry.setSessionId(sessionId);
        logEntry.setAction("submit");
        logEntry.setOperatorId(operatorId);
        logEntry.setComment("提交场次话术审批");
        approvalLogRepository.save(logEntry);

        log.info("场次 {} 的话术已提交审批，操作人 {}", sessionId, operatorId);
    }

    @Override
    @Transactional
    public void approve(Long sessionId, Long scriptId, Long operatorId, String comment) {
        // 校验场次存在
        liveSessionRepository.findByIdAndDeleted(sessionId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "场次不存在"));

        LiveScript script = liveScriptRepository.findById(scriptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "话术不存在"));

        if (!script.getSessionId().equals(sessionId)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "话术不属于该场次");
        }

        if (script.getApprovalStatus() == null || script.getApprovalStatus() != 1) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "话术当前状态不可审批");
        }

        script.setApprovalStatus(2); // 已通过
        liveScriptRepository.save(script);

        LiveApprovalLog logEntry = new LiveApprovalLog();
        logEntry.setSessionId(sessionId);
        logEntry.setScriptId(scriptId);
        logEntry.setAction("approve");
        logEntry.setOperatorId(operatorId);
        logEntry.setComment(comment);
        approvalLogRepository.save(logEntry);

        log.info("话术 {} 审批通过，操作人 {}", scriptId, operatorId);
    }

    @Override
    @Transactional
    public void reject(Long sessionId, Long scriptId, Long operatorId, String comment) {
        // 校验场次存在
        liveSessionRepository.findByIdAndDeleted(sessionId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "场次不存在"));

        LiveScript script = liveScriptRepository.findById(scriptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "话术不存在"));

        if (!script.getSessionId().equals(sessionId)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "话术不属于该场次");
        }

        if (script.getApprovalStatus() == null || script.getApprovalStatus() != 1) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "话术当前状态不可审批");
        }

        script.setApprovalStatus(3); // 已拒绝
        liveScriptRepository.save(script);

        LiveApprovalLog logEntry = new LiveApprovalLog();
        logEntry.setSessionId(sessionId);
        logEntry.setScriptId(scriptId);
        logEntry.setAction("reject");
        logEntry.setOperatorId(operatorId);
        logEntry.setComment(comment);
        approvalLogRepository.save(logEntry);

        log.info("话术 {} 审批拒绝，操作人 {}，原因：{}", scriptId, operatorId, comment);
    }

    @Override
    public List<LiveApprovalLog> getApprovalHistory(Long sessionId) {
        return approvalLogRepository.findBySessionIdAndDeletedOrderByCreateTimeDesc(sessionId, 0);
    }

    @Override
    public List<LiveSession> getPendingApprovals(Long operatorId) {
        // 查找包含 approvalStatus=1（待审核）话术的场次 ID
        List<Long> pendingSessionIds = liveScriptRepository.findSessionIdsWithPendingApproval();
        if (pendingSessionIds.isEmpty()) {
            return List.of();
        }
        return liveSessionRepository.findAllById(pendingSessionIds).stream()
                .filter(s -> s.getDeleted() == 0)
                .toList();
    }
}
