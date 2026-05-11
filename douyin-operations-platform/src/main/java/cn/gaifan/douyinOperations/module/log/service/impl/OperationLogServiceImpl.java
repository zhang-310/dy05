package cn.gaifan.douyinOperations.module.log.service.impl;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.log.entity.OperationLog;
import cn.gaifan.douyinOperations.module.log.repository.OperationLogRepository;
import cn.gaifan.douyinOperations.module.log.service.OperationLogService;
import cn.gaifan.douyinOperations.module.log.vo.OperationLogSearchVO;
import cn.gaifan.douyinOperations.module.log.vo.OperationLogVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作日志查询实现
 */
@Service
public class OperationLogServiceImpl implements OperationLogService {

    private static final Logger log = LoggerFactory.getLogger(OperationLogServiceImpl.class);

    @Resource
    private OperationLogRepository operationLogRepository;

    @Override
    public PageResultVO<OperationLogVO> search(OperationLogSearchVO vo) {
        OperationLogSearchVO q = vo != null ? vo : new OperationLogSearchVO();
        q.validateParams();
        Specification<OperationLog> spec = (root, query, cb) -> {
            List<Predicate> list = new ArrayList<>();
            // P0-1: SQL 注入防护 - LIKE 查询转义特殊字符
            if (q.getModule() != null && !q.getModule().trim().isEmpty()) {
                String escaped = escapeLikePattern(q.getModule().trim());
                list.add(cb.like(root.get("module"), "%" + escaped + "%"));
            }
            if (q.getAction() != null && !q.getAction().trim().isEmpty()) {
                String escaped = escapeLikePattern(q.getAction().trim());
                list.add(cb.like(root.get("action"), "%" + escaped + "%"));
            }
            if (q.getUsername() != null && !q.getUsername().trim().isEmpty()) {
                String escaped = escapeLikePattern(q.getUsername().trim());
                list.add(cb.like(root.get("username"), "%" + escaped + "%"));
            }
            if (q.getStatus() != null) {
                list.add(cb.equal(root.get("status"), q.getStatus()));
            }
            Timestamp start = parseTimestamp(q.getStartTime(), true);
            if (start != null) list.add(cb.greaterThanOrEqualTo(root.get("createTime"), start));
            Timestamp end = parseTimestamp(q.getEndTime(), false);
            if (end != null) list.add(cb.lessThanOrEqualTo(root.get("createTime"), end));
            return cb.and(list.toArray(new Predicate[0]));
        };
        String sortName = q.getSortName() != null ? q.getSortName() : "createTime";
        Sort sort = Sort.by("desc".equalsIgnoreCase(q.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName);
        Page<OperationLog> page = operationLogRepository.findAll(spec, PageRequest.of(q.getPage(), q.getRows(), sort));
        List<OperationLogVO> list = page.getContent().stream().map(this::toVO).collect(Collectors.toList());
        return PageResultVO.of(page.getTotalElements(), list, q.getPage(), q.getRows());
    }

    @Override
    public void save(Long userId, String username, String module, String action, String requestUri, String requestMethod,
                    String ip, String userAgent, Integer durationMs, int status, String errorMsg, String traceId,
                    String requestBody, String responseBody) {
        try {
            OperationLog entity = new OperationLog();
            entity.setUserId(userId);
            // P0-2: 日志注入防护 - 转义换行符
            entity.setUsername(sanitizeLogInput(truncate(username, 64)));
            entity.setModule(truncate(module, 64));
            entity.setAction(truncate(action, 32));
            entity.setRequestUri(truncate(requestUri, 256));
            entity.setRequestMethod(truncate(requestMethod, 16));
            entity.setIp(truncate(ip, 64));
            entity.setUserAgent(sanitizeLogInput(truncate(userAgent, 256)));
            entity.setDurationMs(durationMs);
            entity.setStatus(status);
            entity.setErrorMsg(errorMsg != null ? sanitizeLogInput(truncate(errorMsg, 512)) : null);
            entity.setTraceId(truncate(traceId, 64));
            entity.setRequestBody(requestBody != null ? truncate(requestBody, 2000) : null);
            entity.setResponseBody(responseBody != null ? truncate(responseBody, 2000) : null);
            operationLogRepository.save(entity);
        } catch (Exception e) {
            log.warn("操作日志落库失败，忽略: {}", e.getMessage());
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    private OperationLogVO toVO(OperationLog e) {
        OperationLogVO v = new OperationLogVO();
        v.setId(e.getId());
        v.setTraceId(e.getTraceId());
        v.setUserId(e.getUserId());
        v.setUsername(e.getUsername());
        v.setModule(e.getModule());
        v.setAction(e.getAction());
        v.setRequestUri(e.getRequestUri());
        v.setRequestMethod(e.getRequestMethod());
        v.setIp(e.getIp());
        v.setUserAgent(e.getUserAgent());
        v.setDurationMs(e.getDurationMs());
        v.setStatus(e.getStatus());
        v.setErrorMsg(e.getErrorMsg());
        v.setRequestBody(e.getRequestBody());
        v.setResponseBody(e.getResponseBody());
        v.setCreateTime(e.getCreateTime());
        return v;
    }

    /** P0-1: LIKE 查询转义特殊字符（防止 SQL 注入） */
    private String escapeLikePattern(String input) {
        if (input == null) return null;
        return input.replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
    }

    /** P0-2: 日志注入防护 - 转义换行符和控制字符 */
    private String sanitizeLogInput(String input) {
        if (input == null) return null;
        return input.replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t")
                    .replace("\0", "");
    }

    private static Timestamp parseTimestamp(String s, boolean startOfDay) {
        if (s == null || s.trim().isEmpty()) return null;
        s = s.trim();
        try {
            if (s.matches("^\\d+$")) {
                long ms = Long.parseLong(s);
                if (s.length() <= 10) ms *= 1000;
                return new Timestamp(ms);
            }
            if (s.length() == 10) s += startOfDay ? " 00:00:00" : " 23:59:59";
            return Timestamp.valueOf(s.replace("T", " "));
        } catch (Exception e) {
            return null;
        }
    }
}
