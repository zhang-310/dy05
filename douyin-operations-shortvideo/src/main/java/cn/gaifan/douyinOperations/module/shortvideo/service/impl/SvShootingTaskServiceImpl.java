package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvShootingTask;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvShootingTaskRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvShootingTaskService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShootingTaskSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShootingTaskSearchVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShootingTaskVO;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class SvShootingTaskServiceImpl implements SvShootingTaskService {

    private static final Set<String> SORTABLE = Set.of("id", "createTime", "updateTime", "shootDate", "status", "title");

    @Resource
    private SvShootingTaskRepository shootingTaskRepository;

    @Override
    public PageResultVO<SvShootingTaskVO> search(SvShootingTaskSearchVO vo, Long currentUserId, List<Long> visibleOwnerIds) {
        if (currentUserId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        vo.validateParams();
        Specification<SvShootingTask> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("deleted"), 0));
            if (visibleOwnerIds == null) {
                // admin：不限制 owner
            } else if (visibleOwnerIds.isEmpty()) {
                preds.add(cb.isNull(root.get("id")));
            } else {
                Predicate ownerIn = root.get("ownerId").in(visibleOwnerIds);
                Predicate photo = cb.equal(root.get("photographerId"), currentUserId);
                Predicate anchor = cb.equal(root.get("anchorUserId"), currentUserId);
                preds.add(cb.or(ownerIn, photo, anchor));
            }
            if (StringUtils.hasText(vo.getTitle())) {
                preds.add(cb.like(root.get("title"), "%" + vo.getTitle().trim() + "%"));
            }
            if (vo.getStatus() != null) {
                preds.add(cb.equal(root.get("status"), vo.getStatus()));
            }
            if (vo.getPersonaId() != null) {
                preds.add(cb.equal(root.get("personaId"), vo.getPersonaId()));
            }
            if (vo.getPhotographerId() != null) {
                preds.add(cb.equal(root.get("photographerId"), vo.getPhotographerId()));
            }
            if (vo.getAnchorUserId() != null) {
                preds.add(cb.equal(root.get("anchorUserId"), vo.getAnchorUserId()));
            }
            if (StringUtils.hasText(vo.getShootDateFrom())) {
                try {
                    Date from = Date.valueOf(vo.getShootDateFrom().trim());
                    preds.add(cb.greaterThanOrEqualTo(root.get("shootDate"), from));
                } catch (Exception ignored) {
                }
            }
            if (StringUtils.hasText(vo.getShootDateTo())) {
                try {
                    Date to = Date.valueOf(vo.getShootDateTo().trim());
                    preds.add(cb.lessThanOrEqualTo(root.get("shootDate"), to));
                } catch (Exception ignored) {
                }
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
        String sortName = SORTABLE.contains(vo.getSortName()) ? vo.getSortName() : "shootDate";
        Sort sort = "asc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.by(sortName).ascending() : Sort.by(sortName).descending();
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(), sort);
        Page<SvShootingTask> page = shootingTaskRepository.findAll(spec, pageable);
        List<SvShootingTaskVO> list = page.getContent().stream().map(this::toVO).toList();
        return PageResultVO.of(page.getTotalElements(), list, vo.getPage(), vo.getRows());
    }

    @Override
    public SvShootingTaskVO get(Long id, Long currentUserId, List<Long> visibleOwnerIds) {
        if (currentUserId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        SvShootingTask e = shootingTaskRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "任务不存在"));
        if (e.getDeleted() != 0) throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "任务不存在");
        if (!canView(e, currentUserId, visibleOwnerIds)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问");
        }
        return toVO(e);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(SvShootingTaskSaveVO vo, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        Date shootDate;
        try {
            shootDate = Date.valueOf(vo.getShootDate().trim());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "shootDate 格式须为 yyyy-MM-dd");
        }
        SvShootingTask entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = shootingTaskRepository.findById(vo.getId()).orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "任务不存在"));
            if (entity.getDeleted() != 0) throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "任务不存在");
            if (!entity.getOwnerId().equals(ownerId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权限修改");
        } else {
            entity = new SvShootingTask();
            entity.setOwnerId(ownerId);
        }
        entity.setTitle(vo.getTitle().trim());
        entity.setShootDate(shootDate);
        entity.setAnchorUserId(vo.getAnchorUserId());
        entity.setPersonaId(vo.getPersonaId());
        entity.setPhotographerId(vo.getPhotographerId());
        entity.setScriptId(vo.getScriptId());
        entity.setProjectId(vo.getProjectId());
        entity.setDescription(vo.getDescription());
        entity.setScriptContent(vo.getScriptContent());
        entity.setShootingBrief(vo.getShootingBrief());
        if (vo.getPriority() != null) entity.setPriority(vo.getPriority());
        if (vo.getStatus() != null) entity.setStatus(vo.getStatus());
        entity.setMaterialUrls(vo.getMaterialUrls());
        entity.setReviewNotes(vo.getReviewNotes());
        entity.setReviewedBy(vo.getReviewedBy());
        entity.setReferenceVideoUrl(vo.getReferenceVideoUrl());
        entity.setReferenceVideoTaskId(vo.getReferenceVideoTaskId());
        if (StringUtils.hasText(vo.getReferenceGeneratedAt())) {
            try {
                String s = vo.getReferenceGeneratedAt().trim().replace("T", " ");
                if (s.length() >= 19) {
                    entity.setReferenceGeneratedAt(Timestamp.valueOf(s.substring(0, 19)));
                }
            } catch (Exception ignored) {
            }
        }
        entity = shootingTaskRepository.save(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        SvShootingTask e = shootingTaskRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "任务不存在"));
        if (!e.getOwnerId().equals(ownerId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权限删除");
        if (e.getDeleted() != 0) throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "任务不存在");
        e.setDeleted(1);
        shootingTaskRepository.save(e);
    }

    private boolean canView(SvShootingTask e, Long userId, List<Long> visibleOwnerIds) {
        if (visibleOwnerIds == null) {
            return true;
        }
        if (visibleOwnerIds.isEmpty()) {
            return false;
        }
        if (visibleOwnerIds.contains(e.getOwnerId())) {
            return true;
        }
        if (e.getPhotographerId() != null && e.getPhotographerId().equals(userId)) {
            return true;
        }
        return e.getAnchorUserId() != null && e.getAnchorUserId().equals(userId);
    }

    private SvShootingTaskVO toVO(SvShootingTask e) {
        SvShootingTaskVO vo = new SvShootingTaskVO();
        vo.setId(e.getId());
        vo.setOwnerId(e.getOwnerId());
        vo.setAnchorUserId(e.getAnchorUserId());
        vo.setPersonaId(e.getPersonaId());
        vo.setPhotographerId(e.getPhotographerId());
        vo.setScriptId(e.getScriptId());
        vo.setProjectId(e.getProjectId());
        vo.setTitle(e.getTitle());
        vo.setDescription(e.getDescription());
        vo.setScriptContent(e.getScriptContent());
        vo.setShootingBrief(e.getShootingBrief());
        if (e.getShootDate() != null) {
            vo.setShootDate(e.getShootDate().toString());
        }
        vo.setPriority(e.getPriority());
        vo.setStatus(e.getStatus());
        vo.setMaterialUrls(e.getMaterialUrls());
        vo.setReviewNotes(e.getReviewNotes());
        vo.setReviewedBy(e.getReviewedBy());
        vo.setReferenceVideoUrl(e.getReferenceVideoUrl());
        vo.setReferenceVideoTaskId(e.getReferenceVideoTaskId());
        vo.setReferenceGeneratedAt(e.getReferenceGeneratedAt());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }
}
