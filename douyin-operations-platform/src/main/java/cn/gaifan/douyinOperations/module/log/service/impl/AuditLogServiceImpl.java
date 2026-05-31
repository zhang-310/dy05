package cn.gaifan.douyinOperations.module.log.service.impl;

import cn.gaifan.douyinOperations.common.entity.AuditLog;
import cn.gaifan.douyinOperations.common.repository.AuditLogRepository;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.log.service.AuditLogService;
import cn.gaifan.douyinOperations.module.log.vo.LogAuditSearchVO;
import cn.gaifan.douyinOperations.module.log.vo.LogAuditVO;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 审计日志查询实现。
 */
@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Resource
    private AuditLogRepository auditLogRepository;

    @Override
    public PageResultVO<LogAuditVO> search(LogAuditSearchVO vo) {
        LogAuditSearchVO q = vo != null ? vo : new LogAuditSearchVO();
        q.validateParams();

        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(q.getKeyword())) {
                String keyword = "%" + escapeLikePattern(q.getKeyword().trim()) + "%";
                predicates.add(cb.or(
                        cb.like(root.get("username"), keyword),
                        cb.like(root.get("action"), keyword),
                        cb.like(root.get("entity"), keyword)
                ));
            }
            if (hasText(q.getUsername())) {
                predicates.add(cb.like(root.get("username"), "%" + escapeLikePattern(q.getUsername().trim()) + "%"));
            }
            if (hasText(q.getAction())) {
                predicates.add(cb.like(root.get("action"), "%" + escapeLikePattern(q.getAction().trim()) + "%"));
            }
            if (hasText(q.getEntity())) {
                predicates.add(cb.like(root.get("entity"), "%" + escapeLikePattern(q.getEntity().trim()) + "%"));
            }
            if (hasText(q.getAuditType())) {
                predicates.add(cb.like(root.get("action"), "%" + escapeLikePattern(q.getAuditType().trim()) + "%"));
            }
            if (q.getUserId() != null) {
                predicates.add(cb.equal(root.get("userId"), q.getUserId()));
            }
            if (q.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), q.getStatus()));
            }
            LocalDateTime start = parseDateTime(q.getStartTime(), true);
            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createTime"), start));
            }
            LocalDateTime end = parseDateTime(q.getEndTime(), false);
            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createTime"), end));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        String sortName = "createTime".equals(q.getSortName()) || "id".equals(q.getSortName()) ? q.getSortName() : "createTime";
        Sort.Direction direction = "asc".equalsIgnoreCase(q.getSortOrder()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Page<AuditLog> page = auditLogRepository.findAll(spec, PageRequest.of(q.getPage(), q.getRows(), Sort.by(direction, sortName)));
        List<LogAuditVO> list = page.getContent().stream().map(this::toVO).collect(Collectors.toList());
        return PageResultVO.of(page.getTotalElements(), list, q.getPage(), q.getRows());
    }

    private LogAuditVO toVO(AuditLog log) {
        LogAuditVO vo = new LogAuditVO();
        vo.setId(log.getId());
        vo.setUserId(log.getUserId());
        vo.setUsername(log.getUsername());
        vo.setAuditType(log.getAction());
        vo.setAction(log.getAction());
        vo.setModule(log.getEntity());
        vo.setResourceType(log.getEntity());
        vo.setResourceId(log.getEntityId() != null ? String.valueOf(log.getEntityId()) : null);
        vo.setEntity(log.getEntity());
        vo.setEntityId(log.getEntityId());
        vo.setTargetType(log.getEntity());
        vo.setTargetId(log.getEntityId());
        vo.setOldValue(log.getOldValue());
        vo.setNewValue(log.getNewValue());
        vo.setBeforeValue(log.getOldValue());
        vo.setAfterValue(log.getNewValue());
        vo.setIp(log.getIp());
        vo.setUserAgent(log.getUserAgent());
        vo.setStatus(log.getStatus());
        vo.setErrorMsg(log.getErrorMsg());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String escapeLikePattern(String input) {
        return input.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private static LocalDateTime parseDateTime(String value, boolean startOfDay) {
        if (!hasText(value)) {
            return null;
        }
        String normalized = value.trim().replace("T", " ");
        try {
            if (normalized.length() == 10) {
                LocalDate date = LocalDate.parse(normalized);
                return startOfDay ? date.atStartOfDay() : date.atTime(23, 59, 59);
            }
            return LocalDateTime.parse(normalized.replace(" ", "T"));
        } catch (Exception ignored) {
            return null;
        }
    }
}
