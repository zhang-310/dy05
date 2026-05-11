package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveSessionService;
import cn.gaifan.douyinOperations.module.live.vo.*;
import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

    private static final Logger log = LoggerFactory.getLogger(LiveSessionServiceImpl.class);

    @Resource
    private LiveSessionRepository liveSessionRepository;

    // P0-10: 注入 Caffeine L1 缓存
    @Resource(name = "liveSessionListCache")
    private Cache<String, Object> liveSessionListCache;

    @Resource(name = "liveSessionDetailCache")
    private Cache<Long, Object> liveSessionDetailCache;

    private static final Set<String> SORTABLE_FIELDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("id", "userId", "status", "startTime", "endTime", "viewers", "likes", "createTime", "updateTime")));

    @Override
    public PageResultVO<LiveSessionVO> search(LiveSessionSearchVO vo) {
        vo.validateParams();

        // P0-10: L1 缓存查询
        String cacheKey = buildListCacheKey(vo);
        PageResultVO<LiveSessionVO> cached = (PageResultVO<LiveSessionVO>) liveSessionListCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("[LiveCache] L1 list cache hit: {}", cacheKey);
            return cached;
        }

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
        PageResultVO<LiveSessionVO> result = PageResultVO.of(page.getTotalElements(), list, vo.getPage(), vo.getRows());

        // P0-10: 存入 L1 缓存
        liveSessionListCache.put(cacheKey, result);
        log.debug("[LiveCache] L1 list cache stored: {}", cacheKey);

        return result;
    }

    private String buildListCacheKey(LiveSessionSearchVO vo) {
        return String.format("list:%d:%d:%s:%s:%s:%s:%s",
                vo.getUserId() != null ? vo.getUserId() : 0,
                vo.getStatus() != null ? vo.getStatus() : -1,
                vo.getKeyword() != null ? vo.getKeyword() : "",
                vo.getLiveTitle() != null ? vo.getLiveTitle() : "",
                vo.getSortName(),
                vo.getSortOrder(),
                vo.getPage() + ":" + vo.getRows());
    }

    // P0-3: 添加缓存 - 场次详情查询
    @Override
    @Cacheable(value = "live:session", key = "#id")
    public LiveSessionVO getById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "直播场次 ID 无效");
        }

        // P0-10: L1 缓存查询
        LiveSessionVO cached = (LiveSessionVO) liveSessionDetailCache.getIfPresent(id);
        if (cached != null) {
            log.debug("[LiveCache] L1 detail cache hit: {}", id);
            return cached;
        }

        LiveSession session = liveSessionRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
        LiveSessionVO vo = toLiveSessionVO(session);

        // P0-10: 存入 L1 缓存
        liveSessionDetailCache.put(id, vo);
        log.debug("[LiveCache] L1 detail cache stored: {}", id);

        return vo;
    }

    // P0-3: 保存时清除缓存
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "live:session", key = "#vo.id", condition = "#vo.id != null")
    public long save(LiveSessionSaveVO vo) {
        if (vo.getUserId() == null || vo.getUserId() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "用户 ID 无效");
        }
        LiveSession session;
        if (vo.getId() != null && vo.getId() > 0) {
            session = liveSessionRepository.findByIdAndDeleted(vo.getId(), 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
            // P0-10: 清除 L1 缓存
            liveSessionDetailCache.invalidate(vo.getId());
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

        // P0-10: 清除 L1 列表缓存
        liveSessionListCache.invalidateAll();
        log.debug("[LiveCache] L1 caches invalidated after save: id={}", session.getId());

        return session.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    // P0-3: 删除时清除缓存
    @CacheEvict(value = "live:session", key = "#id")
    public void delete(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "直播场次 ID 无效");
        }
        LiveSession session = liveSessionRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
        session.setDeleted(1);
        liveSessionRepository.save(session);

        // P0-10: 清除 L1 缓存
        liveSessionDetailCache.invalidate(id);
        liveSessionListCache.invalidateAll();
        log.debug("[LiveCache] L1 caches invalidated after delete: id={}", id);
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

        // P0-10: 清除 L1 缓存
        liveSessionDetailCache.invalidate(id);
        liveSessionListCache.invalidateAll();
        log.debug("[LiveCache] L1 caches invalidated after updateStatus: id={}", id);
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

        // P0-10: 清除 L1 缓存
        liveSessionDetailCache.invalidate(id);
        liveSessionListCache.invalidateAll();
        log.debug("[LiveCache] L1 caches invalidated after updateViewers: id={}", id);
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

        // P0-10: 清除 L1 缓存
        liveSessionDetailCache.invalidate(id);
        liveSessionListCache.invalidateAll();
        log.debug("[LiveCache] L1 caches invalidated after updateLikes: id={}", id);
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
