package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.util.RequestRoleResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.entity.LiveCompetitiveInsight;
import cn.gaifan.douyinOperations.module.live.repository.LiveCompetitiveInsightRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveCompetitiveInsightService;
import cn.gaifan.douyinOperations.module.live.vo.LiveCompetitiveInsightSaveVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveCompetitiveInsightSearchVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveCompetitiveInsightVO;
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
public class LiveCompetitiveInsightServiceImpl implements LiveCompetitiveInsightService {

    private static final List<String> SORTABLE = List.of("id", "updateTime", "createTime", "competitorLabel", "gmvEstimate");

    @Resource
    private LiveCompetitiveInsightRepository repository;
    @Resource
    private LiveSessionRepository liveSessionRepository;

    @Override
    public PageResultVO<LiveCompetitiveInsightVO> search(LiveCompetitiveInsightSearchVO vo, Long ownerId) {
        if (ownerId == null || ownerId <= 0) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        vo.validateParams();
        String sortName = vo.getSortName() != null && SORTABLE.contains(vo.getSortName()) ? vo.getSortName() : "updateTime";
        String ord = "asc".equalsIgnoreCase(vo.getSortOrder()) ? "asc" : "desc";
        var pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equals(ord) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Specification<LiveCompetitiveInsight> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("deleted"), 0));
            ps.add(cb.equal(root.get("ownerId"), ownerId));
            if (vo.getSessionId() != null && vo.getSessionId() > 0) {
                ps.add(cb.equal(root.get("sessionId"), vo.getSessionId()));
            }
            if (vo.getKeyword() != null && !vo.getKeyword().isBlank()) {
                String kw = "%" + vo.getKeyword().trim() + "%";
                ps.add(cb.like(root.get("competitorLabel"), kw));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };

        Page<LiveCompetitiveInsight> page = repository.findAll(spec, pageable);
        List<LiveCompetitiveInsightVO> list = page.getContent().stream().map(this::toVo).toList();
        return PageResultVO.of(page.getTotalElements(), list, vo.getPage(), vo.getRows());
    }

    @Override
    public LiveCompetitiveInsightVO getById(Long id, Long ownerId) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "ID 无效");
        }
        LiveCompetitiveInsight e = repository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "记录不存在"));
        if (!ownerId.equals(e.getOwnerId()) && !RequestRoleResolver.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问");
        }
        return toVo(e);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long save(LiveCompetitiveInsightSaveVO vo, Long ownerId) {
        if (ownerId == null || ownerId <= 0) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (vo.getSessionId() != null && vo.getSessionId() > 0) {
            if (RequestRoleResolver.isAdmin()) {
                liveSessionRepository.findByIdAndDeleted(vo.getSessionId(), 0)
                        .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "场次不存在"));
            } else {
                liveSessionRepository.findByIdAndUserIdAndDeleted(vo.getSessionId(), ownerId, 0)
                        .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "场次不存在或无权重关联"));
            }
        }
        LiveCompetitiveInsight entity;
        if (vo.getId() != null && vo.getId() > 0) {
            entity = repository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "记录不存在"));
            if (!ownerId.equals(entity.getOwnerId()) && !RequestRoleResolver.isAdmin()) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权修改");
            }
        } else {
            entity = new LiveCompetitiveInsight();
            entity.setOwnerId(ownerId);
        }
        entity.setSessionId(vo.getSessionId());
        entity.setCompetitorLabel(vo.getCompetitorLabel().trim());
        entity.setProductPrice(vo.getProductPrice());
        entity.setMarketSharePercent(vo.getMarketSharePercent());
        entity.setGmvEstimate(vo.getGmvEstimate());
        entity.setWinLossNotes(vo.getWinLossNotes());
        entity = repository.save(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long ownerId) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "ID 无效");
        }
        LiveCompetitiveInsight e = repository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "记录不存在"));
        if (!ownerId.equals(e.getOwnerId()) && !RequestRoleResolver.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除");
        }
        e.setDeleted(1);
        repository.save(e);
    }

    private LiveCompetitiveInsightVO toVo(LiveCompetitiveInsight e) {
        LiveCompetitiveInsightVO vo = new LiveCompetitiveInsightVO();
        vo.setId(e.getId());
        vo.setOwnerId(e.getOwnerId());
        vo.setSessionId(e.getSessionId());
        vo.setCompetitorLabel(e.getCompetitorLabel());
        vo.setProductPrice(e.getProductPrice());
        vo.setMarketSharePercent(e.getMarketSharePercent());
        vo.setGmvEstimate(e.getGmvEstimate());
        vo.setWinLossNotes(e.getWinLossNotes());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }
}
