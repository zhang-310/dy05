package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.entity.LiveMonitor;
import cn.gaifan.douyinOperations.module.live.repository.LiveMonitorRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveMonitorService;
import cn.gaifan.douyinOperations.module.live.vo.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 直播监控数据服务实现
 */
@Service
public class LiveMonitorServiceImpl implements LiveMonitorService {

    @Resource
    private LiveMonitorRepository liveMonitorRepository;

    private static final Set<String> SORTABLE_FIELDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("id", "sessionId", "timestamp", "createTime")));

    @Override
    public PageResultVO<LiveMonitorVO> search(LiveMonitorSearchVO vo) {
        vo.validateParams();
        String sortName = SORTABLE_FIELDS.contains(vo.getSortName()) ? vo.getSortName() : "id";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Page<LiveMonitor> page;
        if (vo.getSessionId() != null && vo.getSessionId() > 0) {
            page = liveMonitorRepository.findBySessionId(vo.getSessionId(), pageable);
        } else if (vo.getSessionIds() != null && !vo.getSessionIds().isEmpty()) {
            page = liveMonitorRepository.findBySessionIdIn(vo.getSessionIds(), pageable);
        } else {
            page = liveMonitorRepository.findAll(pageable);
        }

        List<LiveMonitorVO> list = page.getContent().stream().map(this::toLiveMonitorVO).collect(Collectors.toList());
        return PageResultVO.of(page.getTotalElements(), list, vo.getPage(), vo.getRows());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long save(LiveMonitorVO vo) {
        if (vo.getSessionId() == null || vo.getSessionId() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "直播场次 ID 无效");
        }

        LiveMonitor monitor = new LiveMonitor();
        monitor.setSessionId(vo.getSessionId());
        monitor.setTimestamp(vo.getTimestamp());
        monitor.setViewers(vo.getViewers());
        monitor.setLikes(vo.getLikes());
        monitor.setComments(vo.getComments());
        monitor.setShares(vo.getShares());
        monitor.setProductImpressions(vo.getProductImpressions());
        monitor = liveMonitorRepository.save(monitor);
        return monitor.getId();
    }

    @Override
    public List<LiveMonitorVO> getBySessionId(Long sessionId) {
        if (sessionId == null || sessionId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "直播场次 ID 无效");
        }
        return liveMonitorRepository.findBySessionId(sessionId).stream()
                .map(this::toLiveMonitorVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBySessionId(Long sessionId) {
        if (sessionId == null || sessionId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "直播场次 ID 无效");
        }
        liveMonitorRepository.deleteBySessionId(sessionId);
    }

    @Override
    public Map<String, Object> getCurrentSlot(Long sessionId) {
        return new HashMap<>();
    }

    @Override
    public List<Map<String, Object>> getScriptSlots(Long sessionId) {
        return new ArrayList<>();
    }

    @Override
    public Map<String, Object> getRealtimeMetrics(Long sessionId) {
        return new HashMap<>();
    }

    @Override
    public Map<String, Object> nextSlot(Long sessionId) {
        return new HashMap<>();
    }

    @Override
    public Map<String, Object> skipToSlot(Long sessionId, int slotIndex) {
        return new HashMap<>();
    }

    private LiveMonitorVO toLiveMonitorVO(LiveMonitor monitor) {
        LiveMonitorVO vo = new LiveMonitorVO();
        vo.setId(monitor.getId());
        vo.setSessionId(monitor.getSessionId());
        vo.setTimestamp(monitor.getTimestamp());
        vo.setViewers(monitor.getViewers());
        vo.setLikes(monitor.getLikes());
        vo.setComments(monitor.getComments());
        vo.setShares(monitor.getShares());
        vo.setProductImpressions(monitor.getProductImpressions());
        vo.setCreateTime(monitor.getCreateTime());
        return vo;
    }
}
