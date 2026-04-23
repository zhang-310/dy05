package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveSessionService;
import cn.gaifan.douyinOperations.module.live.vo.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 直播场次服务实现
 */
@Service
public class LiveSessionServiceImpl implements LiveSessionService {

    @Resource
    private LiveSessionRepository liveSessionRepository;

    private static final Set<String> SORTABLE_FIELDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("id", "userId", "status", "startTime", "endTime", "viewers", "likes", "createTime", "updateTime")));

    @Override
    public PageResultVO<LiveSessionVO> search(LiveSessionSearchVO vo) {
        vo.validateParams();
        String sortName = SORTABLE_FIELDS.contains(vo.getSortName()) ? vo.getSortName() : "id";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Specification<LiveSession> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));

            if (vo.getUserId() != null && vo.getUserId() > 0) {
                predicates.add(cb.equal(root.get("userId"), vo.getUserId()));
            }
            if (vo.getUserIds() != null && !vo.getUserIds().isEmpty()) {
                predicates.add(root.get("userId").in(vo.getUserIds()));
            }
            if (vo.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), vo.getStatus()));
            }
            if (vo.getKeyword() != null && !vo.getKeyword().trim().isEmpty()) {
                String kw = "%" + vo.getKeyword().trim() + "%";
                predicates.add(cb.or(
                    cb.like(root.get("liveTitle"), kw),
                    cb.like(root.get("liveDescription"), kw)
                ));
            }
            if (vo.getLiveTitle() != null && !vo.getLiveTitle().trim().isEmpty()) {
                predicates.add(cb.like(root.get("liveTitle"), "%" + vo.getLiveTitle().trim() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<LiveSession> page = liveSessionRepository.findAll(spec, pageable);
        List<LiveSessionVO> list = page.getContent().stream().map(this::toLiveSessionVO).collect(Collectors.toList());
        return PageResultVO.of(page.getTotalElements(), list, vo.getPage(), vo.getRows());
    }

    @Override
    public LiveSessionVO getById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "直播场次 ID 无效");
        }
        LiveSession session = liveSessionRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
        return toLiveSessionVO(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long save(LiveSessionSaveVO vo) {
        if (vo.getUserId() == null || vo.getUserId() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "用户 ID 无效");
        }
        LiveSession session;
        if (vo.getId() != null && vo.getId() > 0) {
            session = liveSessionRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
        } else {
            session = new LiveSession();
            session.setUserId(vo.getUserId());
        }
        session.setAccountId(vo.getAccountId());
        session.setLiveTitle(vo.getLiveTitle());
        session.setLiveDescription(vo.getLiveDescription());
        session.setLiveUrl(vo.getLiveUrl());
        if (vo.getStatus() != null) {
            session.setStatus(vo.getStatus());
        }
        session = liveSessionRepository.save(session);
        return session.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "直播场次 ID 无效");
        }
        LiveSession session = liveSessionRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
        session.setDeleted(1);
        liveSessionRepository.save(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "直播场次 ID 无效");
        }
        LiveSession session = liveSessionRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
        session.setStatus(status);
        liveSessionRepository.save(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateViewers(Long id, Integer viewers) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "直播场次 ID 无效");
        }
        LiveSession session = liveSessionRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
        session.setViewers(viewers);
        liveSessionRepository.save(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLikes(Long id, Long likes) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "直播场次 ID 无效");
        }
        LiveSession session = liveSessionRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
        session.setLikes(likes);
        liveSessionRepository.save(session);
    }

    @Override
    public LiveSessionVO getByIdWithScope(Long id, List<Long> visibleUserIds) {
        LiveSessionVO vo = getById(id);
        if (visibleUserIds != null && !visibleUserIds.isEmpty() && vo.getUserId() != null
                && !visibleUserIds.contains(vo.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问该场次");
        }
        return vo;
    }

    @Override
    public LiveSessionOverviewVO getOverview(Long id) {
        LiveSession session = liveSessionRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
        LiveSessionOverviewVO vo = new LiveSessionOverviewVO();
        vo.setSession(toLiveSessionVO(session));
        vo.setSessionData(null);
        vo.setProductDataList(Collections.emptyList());
        vo.setScriptTop5(Collections.emptyList());
        return vo;
    }

    @Override
    public LiveReadinessVO getReadiness(Long id) {
        liveSessionRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
        LiveReadinessVO vo = new LiveReadinessVO();
        vo.setReady(false);
        LiveReadinessVO.Checks checks = new LiveReadinessVO.Checks();
        checks.setProducts(new LiveReadinessVO.CheckItem());
        checks.setPersona(new LiveReadinessVO.CheckItem());
        checks.setScripts(new LiveReadinessVO.CheckItem());
        checks.setCompliance(new LiveReadinessVO.CheckItem());
        vo.setChecks(checks);
        return vo;
    }

    @Override
    public LiveTrendResultVO analyzeTrend(LiveTrendRequestVO vo, Long userId) {
        LiveTrendResultVO result = new LiveTrendResultVO();
        result.setAccountId(vo != null ? vo.getAccountId() : null);
        result.setStartDate(vo != null ? vo.getStartDate() : null);
        result.setEndDate(vo != null ? vo.getEndDate() : null);
        result.setDailyTrends(Collections.emptyList());
        result.setTimeSlotTrends(Collections.emptyList());
        result.setSummary("暂无趋势数据");
        return result;
    }

    private LiveSessionVO toLiveSessionVO(LiveSession session) {
        LiveSessionVO vo = new LiveSessionVO();
        vo.setId(session.getId());
        vo.setUserId(session.getUserId());
        vo.setAccountId(session.getAccountId());
        vo.setLiveTitle(session.getLiveTitle());
        vo.setLiveDescription(session.getLiveDescription());
        vo.setScheduledTime(session.getScheduledTime());
        vo.setStartTime(session.getStartTime());
        vo.setEndTime(session.getEndTime());
        vo.setLiveUrl(session.getLiveUrl());
        vo.setViewers(session.getViewers());
        vo.setLikes(session.getLikes());
        vo.setStatus(session.getStatus());
        vo.setRecordingUrl(session.getRecordingUrl());
        vo.setRecordingDuration(session.getRecordingDuration());
        vo.setCreateTime(session.getCreateTime());
        vo.setUpdateTime(session.getUpdateTime());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long cloneSession(Long sessionId, Long userId, String newTitle) {
        LiveSession src = liveSessionRepository.findByIdAndDeleted(sessionId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "场次不存在"));
        LiveSession clone = new LiveSession();
        clone.setUserId(userId);
        clone.setAccountId(src.getAccountId());
        clone.setPersonaId(src.getPersonaId());
        clone.setLiveTitle(newTitle != null && !newTitle.isBlank() ? newTitle : src.getLiveTitle() + " (副本)");
        clone.setLiveDescription(src.getLiveDescription());
        clone.setScriptStyle(src.getScriptStyle());
        clone.setStatus(0); // 草稿
        liveSessionRepository.save(clone);
        return clone.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long exportToShortVideo(Long sessionId, Long userId) {
        // 简单实现：创建一个短视频项目占位，返回 sessionId 作为项目 ID（后续可扩展）
        liveSessionRepository.findByIdAndDeleted(sessionId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "场次不存在"));
        // 返回 sessionId 作为导出标识（真实实现应创建 SvProject）
        return sessionId;
    }
}
