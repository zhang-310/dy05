package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvContentCalendar;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvContentCalendarRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvContentCalendarService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvContentCalendarSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvContentCalendarSearchVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvContentCalendarVO;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class SvContentCalendarServiceImpl implements SvContentCalendarService {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter ISO_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String[] ROTATION = {
            "review", "tutorial", "seeding", "skit", "vlog", "live_preview", "before_after", "trending", "unboxing"
    };

    @Resource
    private SvContentCalendarRepository repository;

    @Override
    public PageResultVO<SvContentCalendarVO> list(SvContentCalendarSearchVO vo, Long userId) {
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        vo.validateParams();
        Specification<SvContentCalendar> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("ownerId"), userId));
            preds.add(cb.equal(root.get("deleted"), 0));
            if (StringUtils.hasText(vo.getPlanDateFrom())) {
                try {
                    LocalDate f = LocalDate.parse(vo.getPlanDateFrom().trim(), ISO_DATE);
                    preds.add(cb.greaterThanOrEqualTo(root.get("planDate"), f));
                } catch (Exception ignored) {
                }
            }
            if (StringUtils.hasText(vo.getPlanDateTo())) {
                try {
                    LocalDate t = LocalDate.parse(vo.getPlanDateTo().trim(), ISO_DATE);
                    preds.add(cb.lessThanOrEqualTo(root.get("planDate"), t));
                } catch (Exception ignored) {
                }
            }
            if (vo.getPersonaId() != null) {
                preds.add(cb.equal(root.get("personaId"), vo.getPersonaId()));
            }
            if (vo.getAccountId() != null) {
                preds.add(cb.equal(root.get("accountId"), vo.getAccountId()));
            }
            if (StringUtils.hasText(vo.getContentType())) {
                preds.add(cb.equal(root.get("contentType"), vo.getContentType().trim()));
            }
            if (vo.getStatus() != null) {
                preds.add(cb.equal(root.get("status"), vo.getStatus()));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
        String sortName = vo.getSortName() != null && !vo.getSortName().isBlank() ? vo.getSortName() : "planDate";
        Sort sort = "desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.by(sortName).descending() : Sort.by(sortName).ascending();
        PageRequest pageable = PageRequest.of(vo.getPage(), vo.getRows(), sort);
        Page<SvContentCalendar> page = repository.findAll(spec, pageable);
        List<SvContentCalendarVO> list = page.getContent().stream().map(this::toVO).toList();
        return PageResultVO.of(page.getTotalElements(), list, vo.getPage(), vo.getRows());
    }

    @Override
    public SvContentCalendarVO get(Long id, Long userId) {
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        SvContentCalendar e = repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "记录不存在"));
        if (e.getDeleted() != 0 || !userId.equals(e.getOwnerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限");
        }
        return toVO(e);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(SvContentCalendarSaveVO vo, Long userId) {
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        LocalDate planDate;
        try {
            planDate = LocalDate.parse(vo.getPlanDate().trim(), ISO_DATE);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "planDate 须为 yyyy-MM-dd");
        }
        SvContentCalendar e;
        if (vo.getId() != null && vo.getId() > 0) {
            e = repository.findById(vo.getId()).orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "记录不存在"));
            if (!userId.equals(e.getOwnerId())) throw new BusinessException(ErrorCode.FORBIDDEN, "无权限");
        } else {
            e = new SvContentCalendar();
            e.setOwnerId(userId);
        }
        e.setPersonaId(vo.getPersonaId());
        e.setPlanDate(planDate);
        e.setContentType(vo.getContentType().trim());
        e.setTitle(vo.getTitle());
        e.setBrief(vo.getBrief());
        e.setScriptId(vo.getScriptId());
        e.setProjectId(vo.getProjectId());
        if (vo.getPriority() != null) e.setPriority(vo.getPriority());
        e.setPublishTime(vo.getPublishTime());
        e.setAccountId(vo.getAccountId());
        e.setTags(vo.getTags());
        if (vo.getStatus() != null) e.setStatus(vo.getStatus());
        e = repository.save(e);
        return e.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId) {
        SvContentCalendar e = repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "记录不存在"));
        if (!userId.equals(e.getOwnerId())) throw new BusinessException(ErrorCode.FORBIDDEN, "无权限");
        e.setDeleted(1);
        repository.save(e);
    }

    @Override
    public List<SvContentCalendarVO> listByDateRange(String from, String to, Long personaId, Long userId) {
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        LocalDate a = LocalDate.parse(from.trim(), ISO_DATE);
        LocalDate b = LocalDate.parse(to.trim(), ISO_DATE);
        Specification<SvContentCalendar> spec = (root, q, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("ownerId"), userId));
            preds.add(cb.equal(root.get("deleted"), 0));
            preds.add(cb.between(root.get("planDate"), a, b));
            if (personaId != null) {
                preds.add(cb.equal(root.get("personaId"), personaId));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
        String sortName = "planDate";
        Sort sort = Sort.by(sortName).ascending().and(Sort.by("priority").descending());
        return repository.findAll(spec, sort).stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoGenerate(Long personaId, String from, String to, Long userId) {
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (personaId == null) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "personaId 必填");
        LocalDate a = LocalDate.parse(from.trim(), ISO_DATE);
        LocalDate b = LocalDate.parse(to.trim(), ISO_DATE);
        int n = 0;
        int idx = 0;
        for (LocalDate d = a; !d.isAfter(b); d = d.plusDays(1)) {
            LocalDate fd = d;
            long exists = repository.count((root, q, cb) -> cb.and(
                    cb.equal(root.get("ownerId"), userId),
                    cb.equal(root.get("personaId"), personaId),
                    cb.equal(root.get("planDate"), fd),
                    cb.equal(root.get("deleted"), 0)
            ));
            if (exists > 0) {
                idx++;
                continue;
            }
            SvContentCalendar e = new SvContentCalendar();
            e.setOwnerId(userId);
            e.setPersonaId(personaId);
            e.setPlanDate(d);
            String ct = ROTATION[idx % ROTATION.length];
            e.setContentType(ct);
            e.setTitle("自动规划 · " + ct);
            e.setBrief("LF-04 轮换生成");
            e.setStatus(0);
            e.setPriority(0);
            repository.save(e);
            n++;
            idx++;
        }
        return n;
    }

    private SvContentCalendarVO toVO(SvContentCalendar e) {
        SvContentCalendarVO v = new SvContentCalendarVO();
        v.setId(e.getId());
        v.setOwnerId(e.getOwnerId());
        v.setPersonaId(e.getPersonaId());
        v.setPlanDate(e.getPlanDate() != null ? e.getPlanDate().format(ISO_DATE) : null);
        v.setContentType(e.getContentType());
        v.setTitle(e.getTitle());
        v.setBrief(e.getBrief());
        v.setScriptId(e.getScriptId());
        v.setProjectId(e.getProjectId());
        v.setShootingTaskId(e.getShootingTaskId());
        v.setStatus(e.getStatus());
        v.setPriority(e.getPriority());
        v.setPublishTime(e.getPublishTime());
        v.setAccountId(e.getAccountId());
        v.setTags(e.getTags());
        v.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().format(ISO_DT) : null);
        v.setUpdateTime(e.getUpdateTime() != null ? e.getUpdateTime().format(ISO_DT) : null);
        return v;
    }
}
