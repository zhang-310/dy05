package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import cn.gaifan.douyinOperations.module.live.entity.LiveProductData;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.entity.LiveSessionData;
import cn.gaifan.douyinOperations.module.live.entity.LiveMonitor;
import cn.gaifan.douyinOperations.module.live.repository.LiveMonitorRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductDataRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionDataRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveDataSyncService;
import cn.gaifan.douyinOperations.module.live.vo.*;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LiveDataSyncServiceImpl implements LiveDataSyncService {

    @Resource
    private LiveSessionDataRepository sessionDataRepository;
    @Resource
    private LiveProductDataRepository productDataRepository;
    @Resource
    private LiveSessionRepository sessionRepository;
    @Resource
    private LiveProductRepository liveProductRepository;
    @Resource
    private LiveMonitorRepository monitorRepository;
    @Resource
    private DataScopeResolver dataScopeService;

    @Override
    public LiveSessionDataVO getSessionData(Long sessionId) {
        return sessionDataRepository.findBySessionId(sessionId)
                .map(this::toSessionDataVO)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "场次数据不存在"));
    }

    @Override
    public SessionDataWithCompareVO getSessionDataWithCompare(Long sessionId) {
        LiveSession currentSession = sessionRepository.findByIdAndDeleted(sessionId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "场次不存在"));
        LiveSessionDataVO current = sessionDataRepository.findBySessionId(sessionId)
                .map(this::toSessionDataVO)
                .orElse(null);
        if (current == null) {
            SessionDataWithCompareVO vo = new SessionDataWithCompareVO();
            vo.setCurrent(new LiveSessionDataVO());
            vo.setPrevious(null);
            return vo;
        }
        Long accountId = currentSession.getAccountId();
        Timestamp beforeTime = currentSession.getEndTime() != null ? currentSession.getEndTime() : new Timestamp(System.currentTimeMillis());
        LiveSessionDataVO previous = null;
        if (accountId != null && accountId > 0) {
            LiveSession prevSession = sessionRepository.findTop1ByAccountIdAndStatusAndDeletedAndEndTimeBeforeOrderByEndTimeDesc(
                    accountId, 2, 0, beforeTime).orElse(null);
            if (prevSession != null) {
                previous = sessionDataRepository.findBySessionId(prevSession.getId()).map(this::toSessionDataVO).orElse(null);
            }
        }
        SessionDataWithCompareVO vo = new SessionDataWithCompareVO();
        vo.setCurrent(current);
        vo.setPrevious(previous);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LiveSessionDataVO saveSessionData(LiveSessionDataSaveVO vo) {
        sessionRepository.findById(vo.getSessionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));

        LiveSessionData entity = sessionDataRepository.findBySessionId(vo.getSessionId())
                .orElseGet(() -> {
                    LiveSessionData d = new LiveSessionData();
                    d.setSessionId(vo.getSessionId());
                    return d;
                });

        if (vo.getTotalViewers() != null) entity.setTotalViewers(vo.getTotalViewers());
        if (vo.getPeakViewers() != null) entity.setPeakViewers(vo.getPeakViewers());
        if (vo.getTotalLikes() != null) entity.setTotalLikes(vo.getTotalLikes());
        if (vo.getTotalComments() != null) entity.setTotalComments(vo.getTotalComments());
        if (vo.getTotalShares() != null) entity.setTotalShares(vo.getTotalShares());
        if (vo.getTotalRevenue() != null) entity.setTotalRevenue(vo.getTotalRevenue());
        if (vo.getTotalOrders() != null) entity.setTotalOrders(vo.getTotalOrders());
        if (vo.getAvgStayTime() != null) entity.setAvgStayTime(vo.getAvgStayTime());
        if (vo.getNewFollowers() != null) entity.setNewFollowers(vo.getNewFollowers());
        entity.setSyncTime(new Timestamp(System.currentTimeMillis()));

        return toSessionDataVO(sessionDataRepository.save(entity));
    }

    @Override
    public List<LiveHistoryItemVO> getHistory(Long userId, String roleCode, Long accountId, int limit, Integer days) {
        List<Long> visibleUserIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        Pageable pageable = PageRequest.of(0, Math.min(limit, 50), Sort.by(Sort.Direction.DESC, "endTime"));
        Page<LiveSession> sessions;
        if (visibleUserIds == null || visibleUserIds.isEmpty()) {
            sessions = accountId != null && accountId > 0
                    ? sessionRepository.findByAccountIdAndStatusAndDeletedOrderByEndTimeDesc(accountId, 2, 0, pageable)
                    : sessionRepository.findByStatusAndDeletedOrderByEndTimeDesc(2, 0, pageable);
        } else {
            sessions = accountId != null && accountId > 0
                    ? sessionRepository.findByUserIdInAndAccountIdAndStatusAndDeletedOrderByEndTimeDesc(
                            visibleUserIds, accountId, 2, 0, pageable)
                    : sessionRepository.findByUserIdInAndStatusAndDeletedOrderByEndTimeDesc(
                            visibleUserIds, 2, 0, pageable);
        }

        List<LiveSession> content = sessions.getContent();
        if (days != null && days > 0) {
            long cutoff = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000;
            content = content.stream()
                    .filter(s -> s.getEndTime() != null && s.getEndTime().getTime() >= cutoff)
                    .toList();
        }
        List<Long> sessionIds = content.stream().map(LiveSession::getId).toList();
        if (sessionIds.isEmpty()) return new ArrayList<>();

        Map<Long, LiveSessionData> dataMap = sessionDataRepository.findBySessionIdIn(sessionIds).stream()
                .collect(Collectors.toMap(LiveSessionData::getSessionId, d -> d));

        List<LiveHistoryItemVO> result = new ArrayList<>();
        for (LiveSession s : content) {
            LiveHistoryItemVO item = new LiveHistoryItemVO();
            item.setSessionId(s.getId());
            item.setLiveTitle(s.getLiveTitle());
            item.setStartTime(s.getStartTime());
            item.setEndTime(s.getEndTime());
            item.setStatus(s.getStatus());
            LiveSessionData sd = dataMap.get(s.getId());
            item.setSessionData(sd != null ? toSessionDataVO(sd) : null);
            result.add(item);
        }
        return result;
    }

    @Override
    public List<LiveProductDataVO> getProductDataBySession(Long sessionId) {
        return productDataRepository.findBySessionId(sessionId)
                .stream().map(e -> toProductDataVO(e, sessionId)).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LiveProductDataVO saveProductData(LiveProductDataSaveVO vo) {
        LiveProductData entity = productDataRepository
                .findBySessionIdAndProductId(vo.getSessionId(), vo.getProductId())
                .orElseGet(() -> {
                    LiveProductData d = new LiveProductData();
                    d.setSessionId(vo.getSessionId());
                    d.setProductId(vo.getProductId());
                    return d;
                });

        if (vo.getImpressions() != null) entity.setImpressions(vo.getImpressions());
        if (vo.getClicks() != null) entity.setClicks(vo.getClicks());
        if (vo.getOrders() != null) entity.setOrders(vo.getOrders());
        if (vo.getSaleQuantity() != null) entity.setSaleQuantity(vo.getSaleQuantity());
        if (vo.getRevenue() != null) entity.setRevenue(vo.getRevenue());
        if (vo.getRefundQuantity() != null) entity.setRefundQuantity(vo.getRefundQuantity());
        if (vo.getConversionRate() != null) entity.setConversionRate(vo.getConversionRate());
        entity.setSyncTime(new Timestamp(System.currentTimeMillis()));

        return toProductDataVO(productDataRepository.save(entity), vo.getSessionId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LiveSessionDataVO syncSessionData(Long sessionId) {
        sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));

        List<LiveMonitor> monitors = monitorRepository.findBySessionId(sessionId);

        LiveSessionData entity = sessionDataRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    LiveSessionData d = new LiveSessionData();
                    d.setSessionId(sessionId);
                    return d;
                });

        if (!monitors.isEmpty()) {
            int peakViewers = monitors.stream().mapToInt(LiveMonitor::getViewers).max().orElse(0);
            long totalLikes = monitors.stream().mapToLong(LiveMonitor::getLikes).max().orElse(0L);
            int totalComments = monitors.stream().mapToInt(LiveMonitor::getComments).max().orElse(0);
            int totalShares = monitors.stream().mapToInt(LiveMonitor::getShares).max().orElse(0);

            entity.setPeakViewers(peakViewers);
            entity.setTotalLikes(totalLikes);
            entity.setTotalComments(totalComments);
            entity.setTotalShares(totalShares);
        }
        entity.setSyncTime(new Timestamp(System.currentTimeMillis()));

        return toSessionDataVO(sessionDataRepository.save(entity));
    }

    private LiveSessionDataVO toSessionDataVO(LiveSessionData e) {
        LiveSessionDataVO vo = new LiveSessionDataVO();
        vo.setId(e.getId());
        vo.setSessionId(e.getSessionId());
        vo.setTotalViewers(e.getTotalViewers());
        vo.setPeakViewers(e.getPeakViewers());
        vo.setTotalLikes(e.getTotalLikes());
        vo.setTotalComments(e.getTotalComments());
        vo.setTotalShares(e.getTotalShares());
        vo.setTotalRevenue(e.getTotalRevenue());
        vo.setTotalOrders(e.getTotalOrders());
        vo.setAvgStayTime(e.getAvgStayTime());
        vo.setNewFollowers(e.getNewFollowers());
        vo.setSyncTime(e.getSyncTime());
        vo.setAiAnalysis(e.getAiAnalysis());
        vo.setAiReviewId(e.getAiReviewId());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }

    private LiveProductDataVO toProductDataVO(LiveProductData e, Long sessionId) {
        LiveProductDataVO vo = new LiveProductDataVO();
        vo.setId(e.getId());
        vo.setSessionId(e.getSessionId());
        vo.setProductId(e.getProductId());
        liveProductRepository.findBySessionIdAndProductId(sessionId != null ? sessionId : e.getSessionId(), e.getProductId())
                .map(LiveProduct::getProductName)
                .ifPresent(vo::setProductName);
        vo.setImpressions(e.getImpressions());
        vo.setClicks(e.getClicks());
        vo.setOrders(e.getOrders());
        vo.setSaleQuantity(e.getSaleQuantity());
        vo.setRevenue(e.getRevenue());
        vo.setRefundQuantity(e.getRefundQuantity());
        vo.setConversionRate(e.getConversionRate());
        vo.setSyncTime(e.getSyncTime());
        vo.setCreateTime(e.getCreateTime());
        vo.setUpdateTime(e.getUpdateTime());
        return vo;
    }
}
