package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvProject;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvProjectRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvProjectService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectSearchVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectVO;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

/**
 * 短视频项目 Service 实现
 */
@Service
public class SvProjectServiceImpl implements SvProjectService {

    @Resource
    private SvProjectRepository projectRepository;

    @Override
    public PageResultVO<SvProjectVO> search(SvProjectSearchVO vo, Long ownerId, List<Long> visibleOwnerIds) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        vo.validateParams();
        Specification<SvProject> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (visibleOwnerIds == null) {
            } else if (visibleOwnerIds.isEmpty()) {
                preds.add(cb.isNull(root.get("id")));
            } else {
                preds.add(root.get("ownerId").in(visibleOwnerIds));
            }
            preds.add(cb.equal(root.get("deleted"), 0));
            if (StringUtils.hasText(vo.getStatus())) {
                preds.add(cb.equal(root.get("status"), vo.getStatus().trim()));
            }
            if (StringUtils.hasText(vo.getProjectType())) {
                preds.add(cb.equal(root.get("projectType"), vo.getProjectType().trim()));
            }
            if (StringUtils.hasText(vo.getTitle())) {
                preds.add(cb.like(root.get("title"), "%" + vo.getTitle().trim() + "%"));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
        String sortName = StringUtils.hasText(vo.getSortName()) ? vo.getSortName() : "createTime";
        Sort sort = "asc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.by(sortName).ascending() : Sort.by(sortName).descending();
        vo.validateParams();
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(), sort);
        Page<SvProject> page = projectRepository.findAll(spec, pageable);
        List<SvProjectVO> list = page.getContent().stream().map(this::toVO).toList();
        return PageResultVO.of(page.getTotalElements(), list, vo.getPage(), vo.getRows());
    }

    @Override
    public SvProjectVO get(Long id, Long ownerId, List<Long> visibleOwnerIds) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        SvProject e = projectRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND, "项目不存在"));
        if (visibleOwnerIds != null && (visibleOwnerIds.isEmpty() || !visibleOwnerIds.contains(e.getOwnerId()))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问");
        }
        if (e.getDeleted() != 0) throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND, "项目不存在");
        return toVO(e);
    }

    @Override
    public Long save(SvProjectSaveVO vo, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        SvProject e;
        if (vo.getId() != null && vo.getId() > 0) {
            e = projectRepository.findById(vo.getId()).orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND, "项目不存在"));
            if (!e.getOwnerId().equals(ownerId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权限修改");
        } else {
            e = new SvProject();
            e.setOwnerId(ownerId);
        }
        e.setTitle(vo.getTitle());
        e.setProjectType(vo.getProjectType());
        if (vo.getPersonaId() != null) e.setPersonaId(vo.getPersonaId());
        if (StringUtils.hasText(vo.getScheduleDate())) {
            try {
                e.setScheduleDate(java.sql.Date.valueOf(vo.getScheduleDate()));
            } catch (Exception ignored) {}
        }
        if (StringUtils.hasText(vo.getShootStatus())) e.setShootStatus(vo.getShootStatus());
        if (StringUtils.hasText(vo.getStatus())) e.setStatus(vo.getStatus());
        else if (e.getId() == null) e.setStatus("draft");
        e.setAccountId(vo.getAccountId());
        e.setScriptId(vo.getScriptId());
        e.setShotListId(vo.getShotListId());
        e.setFinalVideoUrl(vo.getFinalVideoUrl());
        e.setThumbnailUrl(vo.getThumbnailUrl());
        if (vo.getCharacterReferenceUrl() != null) e.setCharacterReferenceUrl(vo.getCharacterReferenceUrl());
        if (vo.getSceneReferenceUrl() != null) e.setSceneReferenceUrl(vo.getSceneReferenceUrl());
        e.setDuration(vo.getDuration());
        e.setPublishTitle(vo.getPublishTitle());
        e.setPublishPlatforms(vo.getPublishPlatforms());
        if (StringUtils.hasText(vo.getPublishTime())) {
            try {
                e.setPublishTime(java.sql.Timestamp.valueOf(vo.getPublishTime().replace("T", " ").substring(0, 19)));
            } catch (Exception ignored) {}
        }
        e.setReviewStatus(vo.getReviewStatus());
        e.setReviewerId(vo.getReviewerId());
        e.setReviewComment(vo.getReviewComment());
        e = projectRepository.save(e);
        return e.getId();
    }

    @Override
    public void delete(Long id, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        SvProject e = projectRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND, "项目不存在"));
        if (!e.getOwnerId().equals(ownerId)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权限删除");
        e.setDeleted(1);
        projectRepository.save(e);
    }

    private SvProjectVO toVO(SvProject e) {
        SvProjectVO vo = new SvProjectVO();
        vo.setId(e.getId());
        vo.setOwnerId(e.getOwnerId());
        vo.setAccountId(e.getAccountId());
        vo.setTitle(e.getTitle());
        vo.setProjectType(e.getProjectType());
        vo.setPersonaId(e.getPersonaId());
        vo.setScheduleDate(e.getScheduleDate());
        vo.setShootStatus(e.getShootStatus());
        vo.setStatus(e.getStatus());
        vo.setScriptId(e.getScriptId());
        vo.setShotListId(e.getShotListId());
        vo.setFinalVideoUrl(e.getFinalVideoUrl());
        vo.setThumbnailUrl(e.getThumbnailUrl());
        vo.setCharacterReferenceUrl(e.getCharacterReferenceUrl());
        vo.setSceneReferenceUrl(e.getSceneReferenceUrl());
        vo.setDuration(e.getDuration());
        vo.setPublishTitle(e.getPublishTitle());
        vo.setPublishPlatforms(e.getPublishPlatforms());
        vo.setPublishTime(e.getPublishTime());
        vo.setReviewStatus(e.getReviewStatus());
        vo.setReviewerId(e.getReviewerId());
        vo.setReviewTime(e.getReviewTime());
        vo.setReviewComment(e.getReviewComment());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }
}
