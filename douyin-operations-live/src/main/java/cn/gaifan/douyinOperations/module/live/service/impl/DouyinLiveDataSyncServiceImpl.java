package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.douyinapi.client.DouyinApiClient;
import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import cn.gaifan.douyinOperations.module.live.entity.LiveProductData;
import cn.gaifan.douyinOperations.module.live.entity.LiveSessionData;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductDataRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionDataRepository;
import cn.gaifan.douyinOperations.module.live.service.DouyinLiveDataSyncService;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionDataVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * 抖音开放平台直播数据同步实现
 * <p>
 * 依赖 DouyinApiClient，需配置 douyin.api.client-key、client-secret。
 * 抖音 API 文档：https://developer.open-douyin.com/docs/resource/zh-CN/dop/develop/openapi/
 * </p>
 */
@Service
public class DouyinLiveDataSyncServiceImpl implements DouyinLiveDataSyncService {

    private static final Logger log = LoggerFactory.getLogger(DouyinLiveDataSyncServiceImpl.class);

    @Resource
    private DouyinApiClient douyinApiClient;
    @Resource
    private LiveSessionDataRepository sessionDataRepository;
    @Resource
    private LiveProductDataRepository productDataRepository;
    @Resource
    private LiveProductRepository liveProductRepository;

    @Override
    public LiveSessionDataVO syncSessionDataFromDouyin(Long sessionId, String roomId, String accessToken) {
        if (douyinApiClient == null || roomId == null || accessToken == null) {
            log.warn("抖音直播数据同步跳过: 缺少 roomId/accessToken 或 DouyinApiClient 未配置");
            return null;
        }
        try {
            DouyinApiClient.LiveDataResponse data = douyinApiClient.getLiveData(roomId, accessToken);
            if (data == null) {
                log.warn("抖音 API 返回空数据: sessionId={}, roomId={}", sessionId, roomId);
                return null;
            }
            LiveSessionData entity = sessionDataRepository.findBySessionId(sessionId)
                    .orElseGet(() -> {
                        LiveSessionData d = new LiveSessionData();
                        d.setSessionId(sessionId);
                        return d;
                    });
            entity.setTotalViewers((int) Math.min(data.totalViewers(), Integer.MAX_VALUE));
            entity.setPeakViewers(Math.max(entity.getPeakViewers(), data.viewers()));
            entity.setTotalLikes(data.likes());
            entity.setTotalComments(data.comments());
            entity.setTotalShares(data.shares());
            entity.setSyncTime(new Timestamp(System.currentTimeMillis()));
            sessionDataRepository.save(entity);
            return toVO(entity);
        } catch (Exception e) {
            log.error("抖音直播数据同步失败: sessionId={}, roomId={}", sessionId, roomId, e);
            return null;
        }
    }

    @Override
    public boolean syncProductDataFromDouyin(Long sessionId, String roomId, String accessToken) {
        if (douyinApiClient == null || roomId == null || accessToken == null) {
            return false;
        }
        try {
            DouyinApiClient.ProductListResponse response = douyinApiClient.getProductList(roomId, accessToken);
            if (response == null || response.products() == null || response.products().isEmpty()) {
                return true; // 无商品也算成功
            }
            List<LiveProduct> liveProducts = liveProductRepository.findBySessionId(sessionId);
            for (DouyinApiClient.ProductInfo pi : response.products()) {
                Optional<LiveProduct> match = liveProducts.stream()
                        .filter(lp -> pi.productId().equals(String.valueOf(lp.getProductId()))
                                || pi.name().equals(lp.getProductName()))
                        .findFirst();
                if (match.isEmpty()) continue;
                LiveProduct lp = match.get();
                LiveProductData entity = productDataRepository
                        .findBySessionIdAndProductId(sessionId, lp.getProductId())
                        .orElseGet(() -> {
                            LiveProductData d = new LiveProductData();
                            d.setSessionId(sessionId);
                            d.setProductId(lp.getProductId());
                            return d;
                        });
                entity.setSaleQuantity(pi.sales());
                entity.setOrders(pi.sales());
                entity.setRevenue(BigDecimal.valueOf(pi.price() * pi.sales()));
                entity.setSyncTime(new Timestamp(System.currentTimeMillis()));
                productDataRepository.save(entity);
            }
            return true;
        } catch (Exception e) {
            log.error("抖音分产品数据同步失败: sessionId={}, roomId={}", sessionId, roomId, e);
            return false;
        }
    }

    private LiveSessionDataVO toVO(LiveSessionData e) {
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
}
