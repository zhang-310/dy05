package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.util.RequestRoleResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.entity.LiveCompetitorScript;
import cn.gaifan.douyinOperations.module.live.repository.LiveCompetitorScriptRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveCompetitorScriptService;
import cn.gaifan.douyinOperations.module.live.vo.LiveCompetitorScriptSaveVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveCompetitorScriptSearchVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveCompetitorScriptVO;
import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class LiveCompetitorScriptServiceImpl implements LiveCompetitorScriptService {

    private static final List<String> SORTABLE = List.of("id", "updateTime", "createTime", "title");

    @Resource
    private LiveCompetitorScriptRepository repository;

    @Override
    public PageResultVO<LiveCompetitorScriptVO> search(LiveCompetitorScriptSearchVO vo, Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        vo.validateParams();
        String sortName = vo.getSortName() != null && SORTABLE.contains(vo.getSortName()) ? vo.getSortName() : "updateTime";
        String ord = "asc".equalsIgnoreCase(vo.getSortOrder()) ? "asc" : "desc";
        var pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equals(ord) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Specification<LiveCompetitorScript> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("deleted"), 0));
            ps.add(cb.equal(root.get("ownerId"), userId));
            if (vo.getPlatform() != null && !vo.getPlatform().isBlank()) {
                ps.add(cb.equal(root.get("platform"), vo.getPlatform().trim()));
            }
            if (vo.getKeyword() != null && !vo.getKeyword().isBlank()) {
                String kw = "%" + vo.getKeyword().trim() + "%";
                ps.add(cb.or(
                        cb.like(root.get("title"), kw),
                        cb.like(root.get("scriptContent"), kw),
                        cb.like(cb.coalesce(root.get("competitorName"), ""), kw)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };

        Page<LiveCompetitorScript> page = repository.findAll(spec, pageable);
        List<LiveCompetitorScriptVO> list = page.getContent().stream().map(this::toVo).toList();
        return PageResultVO.of(page.getTotalElements(), list, vo.getPage(), vo.getRows());
    }

    @Override
    public LiveCompetitorScriptVO getById(Long id, Long userId) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "ID 无效");
        }
        LiveCompetitorScript e = repository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "记录不存在"));
        if (!userId.equals(e.getOwnerId()) && !RequestRoleResolver.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问");
        }
        return toVo(e);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long save(LiveCompetitorScriptSaveVO vo, Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        LiveCompetitorScript entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = repository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "记录不存在"));
            if (!userId.equals(entity.getOwnerId()) && !RequestRoleResolver.isAdmin()) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权修改");
            }
        } else {
            entity = new LiveCompetitorScript();
            entity.setOwnerId(userId);
        }
        entity.setTitle(vo.getTitle().trim());
        entity.setCompetitorName(vo.getCompetitorName());
        entity.setPlatform(vo.getPlatform() != null && !vo.getPlatform().isBlank() ? vo.getPlatform().trim() : "douyin");
        entity.setScriptContent(vo.getScriptContent());
        entity.setSourceUrl(vo.getSourceUrl());
        entity.setTags(vo.getTags());
        entity.setNotes(vo.getNotes());
        entity = repository.save(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "ID 无效");
        }
        LiveCompetitorScript e = repository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "记录不存在"));
        if (!userId.equals(e.getOwnerId()) && !RequestRoleResolver.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除");
        }
        e.setDeleted(1);
        repository.save(e);
    }

    private LiveCompetitorScriptVO toVo(LiveCompetitorScript e) {
        LiveCompetitorScriptVO vo = new LiveCompetitorScriptVO();
        vo.setId(e.getId());
        vo.setOwnerId(e.getOwnerId());
        vo.setTitle(e.getTitle());
        vo.setCompetitorName(e.getCompetitorName());
        vo.setPlatform(e.getPlatform());
        vo.setScriptContent(e.getScriptContent());
        vo.setSourceUrl(e.getSourceUrl());
        vo.setTags(e.getTags());
        vo.setNotes(e.getNotes());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }
}
