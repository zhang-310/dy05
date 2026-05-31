package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.util.RequestRoleResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.AiCallLog;
import cn.gaifan.douyinOperations.module.ai.entity.AiKbDocument;
import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase;
import cn.gaifan.douyinOperations.module.ai.repository.AiCallLogRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiKnowledgeBaseRepository;
import cn.gaifan.douyinOperations.module.ai.util.ContentSecurityScanner;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.entity.LiveScriptApproval;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptApprovalRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptApprovalService;
import cn.gaifan.douyinOperations.module.live.vo.LiveScriptApprovalSearchVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveScriptApprovalVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LiveScriptApprovalServiceImpl implements LiveScriptApprovalService {

    private static final Logger log = LoggerFactory.getLogger(LiveScriptApprovalServiceImpl.class);

    @Resource
    private LiveScriptApprovalRepository approvalRepository;
    @Resource
    private LiveScriptRepository scriptRepository;
    @Resource
    private LiveSessionRepository sessionRepository;
    @Autowired(required = false)
    private AiCallLogRepository aiCallLogRepository;
    @Autowired(required = false)
    private AiKbDocumentRepository aiKbDocumentRepository;
    @Autowired(required = false)
    private AiKnowledgeBaseRepository aiKnowledgeBaseRepository;

    @Override
    @Transactional
    public LiveScriptApprovalVO submit(Long scriptId, String comments, Long userId) {
        LiveScript script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIVE_SCRIPT_NOT_FOUND, "话术不存在"));

        // 校验所有权（管理员跳过）
        if (!RequestRoleResolver.isAdmin()) {
            sessionRepository.findByIdAndUserIdAndDeleted(script.getSessionId(), userId, 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "无权操作该话术"));
        }

        // 检查当前状态：只有草稿(0)或已拒绝(3)可以提交
        if (script.getApprovalStatus() != null && script.getApprovalStatus() == 1) {
            throw new BusinessException(ErrorCode.LIVE_APPROVAL_ALREADY_PENDING, "该话术已在审核中");
        }
        if (script.getApprovalStatus() != null && script.getApprovalStatus() == 2) {
            throw new BusinessException(ErrorCode.LIVE_APPROVAL_ALREADY_APPROVED, "该话术已审核通过");
        }
        assertOfficialApprovalGate(script, "直播话术提交审核");

        // P0-10 多级审批：根据内容安全风险等级设置 max_level
        String content = script.getScriptContent() != null ? script.getScriptContent() : "";
        int riskLevel = ContentSecurityScanner.getRiskLevel(content, "approval-check", false);
        int maxLevel;
        int approvalLevel = 1;
        int status = 1;
        String autoApproveRule = null;

        if (riskLevel == 0) {
            maxLevel = 0;
            status = 2; // 无风险 → 自动通过
            autoApproveRule = "low_risk_auto";
            script.setApprovalStatus(2);
        } else if (riskLevel == 1) {
            maxLevel = 1; // 低风险 → 单级审批
            script.setApprovalStatus(1);
        } else {
            maxLevel = 2; // 高风险 → 双级审批
            script.setApprovalStatus(1);
        }
        scriptRepository.save(script);

        // 创建审核记录
        LiveScriptApproval approval = new LiveScriptApproval();
        approval.setScriptId(scriptId);
        approval.setSessionId(script.getSessionId());
        approval.setSubmitterId(userId);
        approval.setAction("submit");
        approval.setStatus(status);
        approval.setComments(comments);
        approval.setApprovalLevel(approvalLevel);
        approval.setMaxLevel(maxLevel);
        if (autoApproveRule != null) approval.setAutoApproveRule(autoApproveRule);
        approvalRepository.save(approval);

        return toVO(approval, script, null);
    }

    @Override
    @Transactional
    public List<LiveScriptApprovalVO> submitBySession(Long sessionId, String comments, Long userId) {
        LiveSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
        if (!RequestRoleResolver.isAdmin() && !session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该场次");
        }
        List<LiveScript> scripts = scriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(sessionId, 0);
        List<LiveScriptApprovalVO> results = new ArrayList<>();
        for (LiveScript script : scripts) {
            // 跳过已在审核中或已通过的话术
            if (script.getApprovalStatus() != null
                    && (script.getApprovalStatus() == 1 || script.getApprovalStatus() == 2)) {
                continue;
            }
            if (script.getScriptContent() == null || script.getScriptContent().isBlank()) {
                continue;
            }
            try {
                results.add(submit(script.getId(), comments, userId));
            } catch (Exception e) {
                log.warn("场次批量提交审批跳过 scriptId={}: {}", script.getId(), e.getMessage());
            }
        }
        return results;
    }

    @Override
    @Transactional
    public LiveScriptApprovalVO review(Long approvalId, String action, String comments, Long reviewerId) {
        if (!"approve".equals(action) && !"reject".equals(action)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "操作类型无效，仅支持 approve/reject");
        }

        LiveScriptApproval approval = approvalRepository.findByIdAndDeleted(approvalId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIVE_APPROVAL_NOT_FOUND, "审核记录不存在"));

        if (approval.getStatus() != 1) {
            throw new BusinessException(ErrorCode.LIVE_APPROVAL_ALREADY_REVIEWED, "该审核已处理");
        }

        int newStatus;
        if ("reject".equals(action)) {
            newStatus = 3;
        } else {
            scriptRepository.findById(approval.getScriptId())
                    .ifPresent(script -> assertOfficialApprovalGate(script, "直播话术审批通过"));
            // approve：P0-10 分级审批
            int maxLevel = approval.getMaxLevel() != null ? approval.getMaxLevel() : 1;
            int currentLevel = approval.getApprovalLevel() != null ? approval.getApprovalLevel() : 1;
            if (currentLevel < maxLevel) {
                // 进入下一级，仍为待审核
                approval.setApprovalLevel(currentLevel + 1);
                approval.setCurrentApproverId(reviewerId);
                newStatus = 1;
            } else {
                newStatus = 2; // 最终通过
            }
        }

        approval.setReviewerId(reviewerId);
        approval.setAction(action);
        approval.setStatus(newStatus);
        approval.setComments(comments);
        approval.setReviewTime(new Timestamp(System.currentTimeMillis()));
        approvalRepository.save(approval);

        // 同步更新话术审核状态（仅最终通过/拒绝时更新）
        if (newStatus != 1) {
            scriptRepository.findById(approval.getScriptId()).ifPresent(script -> {
                script.setApprovalStatus(newStatus);
                scriptRepository.save(script);
            });
        }

        LiveScript script = scriptRepository.findById(approval.getScriptId()).orElse(null);
        return toVO(approval, script, null);
    }

    @Override
    @Transactional
    public void revoke(Long scriptId, Long userId) {
        LiveScript script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIVE_SCRIPT_NOT_FOUND, "话术不存在"));

        if (!RequestRoleResolver.isAdmin()) {
            sessionRepository.findByIdAndUserIdAndDeleted(script.getSessionId(), userId, 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "无权操作该话术"));
        }

        if (script.getApprovalStatus() == null || script.getApprovalStatus() != 1) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "仅待审核状态可撤回");
        }

        // 恢复为草稿
        script.setApprovalStatus(0);
        scriptRepository.save(script);

        // 标记最新审核记录为已撤回
        approvalRepository.findTopByScriptIdAndDeletedOrderByCreateTimeDesc(scriptId, 0)
                .ifPresent(a -> {
                    a.setAction("revoke");
                    a.setStatus(0);
                    approvalRepository.save(a);
                });
    }

    @Override
    public PageResultVO<LiveScriptApprovalVO> search(LiveScriptApprovalSearchVO vo) {
        Specification<LiveScriptApproval> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (vo.getScriptId() != null) predicates.add(cb.equal(root.get("scriptId"), vo.getScriptId()));
            if (vo.getSessionId() != null) predicates.add(cb.equal(root.get("sessionId"), vo.getSessionId()));
            if (vo.getStatus() != null) predicates.add(cb.equal(root.get("status"), vo.getStatus()));
            if (vo.getSubmitterId() != null) predicates.add(cb.equal(root.get("submitterId"), vo.getSubmitterId()));
            if (vo.getReviewerId() != null) predicates.add(cb.equal(root.get("reviewerId"), vo.getReviewerId()));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        PageRequest pageable = PageRequest.of(vo.getPage(), vo.getRows(), sort);
        Page<LiveScriptApproval> page = approvalRepository.findAll(spec, pageable);

        // Batch fetch related scripts and sessions
        Set<Long> scriptIds = page.getContent().stream().map(LiveScriptApproval::getScriptId).collect(Collectors.toSet());
        Set<Long> sessionIds = page.getContent().stream().map(LiveScriptApproval::getSessionId).collect(Collectors.toSet());
        Map<Long, LiveScript> scriptMap = scriptIds.isEmpty() ? Map.of() :
                scriptRepository.findAllById(scriptIds).stream().collect(Collectors.toMap(LiveScript::getId, s -> s, (a, b) -> a));
        Map<Long, LiveSession> sessionMap = sessionIds.isEmpty() ? Map.of() :
                sessionRepository.findAllById(sessionIds).stream().collect(Collectors.toMap(LiveSession::getId, s -> s, (a, b) -> a));

        List<LiveScriptApprovalVO> list = page.getContent().stream()
                .map(a -> toVO(a, scriptMap.get(a.getScriptId()), sessionMap.get(a.getSessionId())))
                .collect(Collectors.toList());

        PageResultVO<LiveScriptApprovalVO> result = new PageResultVO<>();
        result.setTotal(page.getTotalElements());
        result.setList(list);
        result.setPageNum(vo.getPage());
        result.setPageSize(vo.getRows());
        return result;
    }

    @Override
    public List<LiveScriptApprovalVO> history(Long scriptId) {
        List<LiveScriptApproval> records = approvalRepository.findByScriptIdAndDeleted(scriptId, 0);
        LiveScript script = scriptRepository.findById(scriptId).orElse(null);
        return records.stream()
                .sorted(Comparator.comparing(LiveScriptApproval::getCreateTime).reversed())
                .map(a -> toVO(a, script, null))
                .collect(Collectors.toList());
    }

    private void assertOfficialApprovalGate(LiveScript script, String scene) {
        if (hasRequiredOfficialReferences(script)) {
            return;
        }
        throw new BusinessException(ErrorCode.COMPLIANCE_VIOLATION,
                "官方规则引用门禁未通过：" + scene
                        + " 必须能追溯到 douyin 官方学习资料和 douyin_weigui 违规规则引用，禁止放行审批结果");
    }

    private boolean hasRequiredOfficialReferences(LiveScript script) {
        if (script == null || script.getAiCallLogId() == null
                || aiCallLogRepository == null || aiKbDocumentRepository == null || aiKnowledgeBaseRepository == null) {
            return false;
        }
        return aiCallLogRepository.findById(script.getAiCallLogId())
                .map(AiCallLog::getReferencedChunkIds)
                .map(this::resolveReferenceGate)
                .orElse(false);
    }

    private boolean resolveReferenceGate(String referencedChunkIds) {
        if (!StringUtils.hasText(referencedChunkIds)) {
            return false;
        }
        Set<Long> docIds = parseReferencedDocIds(referencedChunkIds);
        if (docIds.isEmpty()) {
            return false;
        }
        List<AiKbDocument> docs = aiKbDocumentRepository.findByIdIn(new ArrayList<>(docIds));
        if (docs.isEmpty()) {
            return false;
        }
        Set<Long> kbIds = docs.stream()
                .map(AiKbDocument::getKbId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (kbIds.isEmpty()) {
            return false;
        }
        Map<Long, String> kbNames = aiKnowledgeBaseRepository.findAllById(kbIds).stream()
                .collect(Collectors.toMap(AiKnowledgeBase::getId, AiKnowledgeBase::getKbName, (a, b) -> a));
        boolean hasOfficialLearning = false;
        boolean hasViolationRule = false;
        for (AiKbDocument doc : docs) {
            String kbName = kbNames.get(doc.getKbId());
            if ("douyin".equals(kbName)) {
                hasOfficialLearning = true;
            }
            if ("douyin_weigui".equals(kbName)) {
                hasViolationRule = true;
            }
        }
        return hasOfficialLearning && hasViolationRule;
    }

    private Set<Long> parseReferencedDocIds(String referencedChunkIds) {
        Set<Long> docIds = new LinkedHashSet<>();
        try {
            List<Number> nums = com.alibaba.fastjson2.JSON.parseArray(referencedChunkIds, Number.class);
            if (nums == null) {
                return docIds;
            }
            for (Number n : nums) {
                if (n == null) {
                    continue;
                }
                long raw = n.longValue();
                if (raw <= 0) {
                    continue;
                }
                docIds.add(raw >= 10000L ? raw / 10000L : raw);
            }
        } catch (Exception e) {
            log.debug("解析直播话术官方引用失败: {}", e.getMessage());
        }
        return docIds;
    }

    // ── Mapping ──

    private LiveScriptApprovalVO toVO(LiveScriptApproval a, LiveScript script, LiveSession session) {
        LiveScriptApprovalVO vo = new LiveScriptApprovalVO();
        vo.setId(a.getId());
        vo.setScriptId(a.getScriptId());
        vo.setSessionId(a.getSessionId());
        vo.setSubmitterId(a.getSubmitterId());
        vo.setReviewerId(a.getReviewerId());
        vo.setAction(a.getAction());
        vo.setStatus(a.getStatus());
        vo.setComments(a.getComments());
        vo.setReviewTime(a.getReviewTime());
        vo.setCreateTime(a.getCreateTime());
        vo.setUpdateTime(a.getUpdateTime());
        if (script != null) {
            vo.setScriptContent(script.getScriptContent());
            vo.setScriptType(script.getScriptType());
        }
        if (session != null) {
            vo.setSessionTitle(session.getLiveTitle());
        }
        return vo;
    }
}
