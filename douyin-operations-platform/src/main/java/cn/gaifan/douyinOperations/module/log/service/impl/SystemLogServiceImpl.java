package cn.gaifan.douyinOperations.module.log.service.impl;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.log.entity.SystemLog;
import cn.gaifan.douyinOperations.module.log.repository.SystemLogRepository;
import cn.gaifan.douyinOperations.module.log.service.SystemLogService;
import cn.gaifan.douyinOperations.module.log.vo.SystemLogSearchVO;
import cn.gaifan.douyinOperations.module.log.vo.SystemLogVO;
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
 * 系统日志查询实现
 */
@Service
public class SystemLogServiceImpl implements SystemLogService {

    private static final Logger log = LoggerFactory.getLogger(SystemLogServiceImpl.class);

    @Resource
    private SystemLogRepository systemLogRepository;

    @Override
    public PageResultVO<SystemLogVO> search(SystemLogSearchVO vo) {
        SystemLogSearchVO q = vo != null ? vo : new SystemLogSearchVO();
        q.validateParams();
        Specification<SystemLog> spec = (root, query, cb) -> {
            List<Predicate> list = new ArrayList<>();
            // P0-1: SQL 注入防护 - LIKE 查询转义特殊字符
            if (q.getModule() != null && !q.getModule().trim().isEmpty()) {
                String escaped = escapeLikePattern(q.getModule().trim());
                list.add(cb.like(root.get("module"), "%" + escaped + "%"));
            }
            if (q.getEventType() != null && !q.getEventType().trim().isEmpty()) {
                String escaped = escapeLikePattern(q.getEventType().trim());
                list.add(cb.like(root.get("eventType"), "%" + escaped + "%"));
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
        Page<SystemLog> page = systemLogRepository.findAll(spec, PageRequest.of(q.getPage(), q.getRows(), sort));
        List<SystemLogVO> list = page.getContent().stream().map(this::toVO).collect(Collectors.toList());
        return PageResultVO.of(page.getTotalElements(), list, q.getPage(), q.getRows());
    }

    @Override
    public void save(String module, String eventType, String summary, String detail, int status) {
        try {
            SystemLog entity = new SystemLog();
            entity.setModule(truncate(module, 64));
            entity.setEventType(eventType != null && eventType.length() > 32 ? eventType.substring(0, 32) : eventType);
            entity.setSummary(truncate(summary, 256));
            entity.setDetail(detail);
            entity.setStatus(status);
            systemLogRepository.save(entity);
        } catch (Exception e) {
            log.warn("系统日志落库失败，忽略: {}", e.getMessage());
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    private SystemLogVO toVO(SystemLog e) {
        SystemLogVO v = new SystemLogVO();
        v.setId(e.getId());
        v.setModule(e.getModule());
        v.setEventType(e.getEventType());
        v.setSummary(e.getSummary());
        v.setDetail(e.getDetail());
        v.setStatus(e.getStatus());
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
