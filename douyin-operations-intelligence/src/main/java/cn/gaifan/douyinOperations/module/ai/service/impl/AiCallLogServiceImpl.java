package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.util.SensitiveDataMasker;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.AiCallLog;
import cn.gaifan.douyinOperations.module.ai.repository.AiCallLogRepository;
import cn.gaifan.douyinOperations.module.ai.service.AiCallLogService;
import cn.gaifan.douyinOperations.module.ai.vo.CallLogSearchVO;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;


@Service
public class AiCallLogServiceImpl implements AiCallLogService {

    @Resource
    private AiCallLogRepository aiCallLogRepository;

    @Override
    public PageResultVO<AiCallLog> search(CallLogSearchVO vo) {
        int page = vo.getPage() != null ? vo.getPage() : 0;
        int rows = vo.getRows() != null ? Math.min(vo.getRows(), 100) : 20;
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        PageRequest pr = PageRequest.of(page, rows, sort);

        Specification<AiCallLog> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> preds = new ArrayList<>();
            if (vo.getCallType() != null && !vo.getCallType().isBlank()) {
                preds.add(cb.equal(root.get("callType"), vo.getCallType().trim()));
            }
            if (vo.getStatus() != null) {
                preds.add(cb.equal(root.get("status"), vo.getStatus()));
            }
            if (vo.getKeyword() != null && !vo.getKeyword().isBlank()) {
                preds.add(cb.like(root.get("inputSummary"), "%" + vo.getKeyword().trim() + "%"));
            }
            if (vo.getStartTime() != null && !vo.getStartTime().isBlank()) {
                try {
                    Timestamp ts = new Timestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(vo.getStartTime().trim()).getTime());
                    preds.add(cb.greaterThanOrEqualTo(root.get("createTime"), ts));
                } catch (Exception ignored) {
                }
            }
            if (vo.getEndTime() != null && !vo.getEndTime().isBlank()) {
                try {
                    Timestamp ts = new Timestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(vo.getEndTime().trim()).getTime());
                    preds.add(cb.lessThanOrEqualTo(root.get("createTime"), ts));
                } catch (Exception ignored) {
                }
            }
            return cb.and(preds.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<AiCallLog> p = aiCallLogRepository.findAll(spec, pr);
        PageResultVO<AiCallLog> result = new PageResultVO<>();
        result.setList(p.getContent());
        result.setTotal(p.getTotalElements());
        result.setPageNum(page);
        result.setPageSize(rows);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long log(LogEntry entry) {
        AiCallLog log = new AiCallLog();
        log.setUserId(entry.userId());
        log.setCallType(entry.callType());
        log.setTemplateCode(entry.templateCode());
        log.setModelCode(entry.modelCode());
        log.setInputSummary(maskAndTruncate(entry.inputSummary(), 512));
        log.setOutputLength(entry.outputLength());
        log.setPromptTokens(entry.promptTokens());
        log.setCompletionTokens(entry.completionTokens());
        Integer total = resolveTotalTokens(entry);
        if (total != null) {
            log.setTotalTokens(total);
        }
        log.setDurationMs(entry.durationMs());
        log.setStatus(entry.status());
        log.setErrorMessage(maskAndTruncate(entry.errorMessage(), 512));
        log.setIsFallback(entry.isFallback() ? 1 : 0);
        log.setReferencedChunkIds(entry.referencedChunkIds());
        log.setStageTimings(entry.stageTimings());
        aiCallLogRepository.save(log);
        return log.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void linkToPublish(Long callLogId, Long videoId, Long sessionId, Long userId) {
        if (callLogId == null || (videoId == null && sessionId == null)) {
            return;
        }
        aiCallLogRepository.findById(callLogId).ifPresent(log -> {
            // H1: 数据隔离 - 校验 callLogId 归属
            if (!log.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此日志");
            }
            if (videoId != null) log.setLinkedVideoId(videoId);
            if (sessionId != null) log.setLinkedSessionId(sessionId);
            aiCallLogRepository.save(log);
        });
    }

    private static String maskAndTruncate(String s, int maxLen) {
        if (s == null) return null;
        s = SensitiveDataMasker.mask(s);
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    /**
     * 汇总 total_tokens：优先 LLM 返回的 prompt+completion；否则对未记录用量的调用做粗算（如 kb_search 仅传了命中条数）。
     * 否则仪表盘「本月 Token 消耗」会对纯检索类恒为 0，与「今日调用」不一致。
     */
    private static Integer resolveTotalTokens(AiCallLogService.LogEntry entry) {
        Integer p = entry.promptTokens();
        Integer c = entry.completionTokens();
        if (p != null && c != null) {
            return p + c;
        }
        if (p != null) {
            return p + (c != null ? c : 0);
        }
        if (c != null) {
            return c;
        }
        int est = estimateTokensWhenLlmUsageMissing(entry);
        return est > 0 ? est : null;
    }

    private static int estimateTokensWhenLlmUsageMissing(AiCallLogService.LogEntry entry) {
        int est = 0;
        if (entry.inputSummary() != null && !entry.inputSummary().isBlank()) {
            est += Math.max(1, entry.inputSummary().length() / 2);
        }
        if (entry.outputLength() != null && entry.outputLength() > 0) {
            if ("kb_search".equals(entry.callType())) {
                // outputLength 为命中条数：粗算每条返回片段约 150 token（与向量/全文检索返回规模同量级）
                est += Math.min(entry.outputLength() * 150, 50_000);
            } else {
                est += Math.max(0, entry.outputLength() / 4);
            }
        }
        return Math.min(est, 500_000);
    }
}
