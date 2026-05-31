package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.repository.LiveMonitorRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.vo.LiveAnalyticsOverviewVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 直播数据分析 Controller。
 */
@RestController
@RequestMapping("/api/v1/live/analytics")
public class LiveAnalyticsController {

    @Resource
    private LiveSessionRepository liveSessionRepository;

    @Resource
    private LiveMonitorRepository liveMonitorRepository;

    @PostMapping("/overview")
    public RESTResult<LiveAnalyticsOverviewVO> overview() {
        long totalSessions = liveSessionRepository.countByDeleted(0);
        long monitorRecords = liveMonitorRepository.countActiveRecords();
        BigDecimal maxGmv = liveMonitorRepository.findGlobalMaxGmv();
        Double avgViewers = liveMonitorRepository.findGlobalAvgViewers();

        LiveAnalyticsOverviewVO vo = new LiveAnalyticsOverviewVO();
        vo.setTotalSessions(totalSessions);
        vo.setTotalGmv(maxGmv != null ? maxGmv.doubleValue() : 0.0);
        vo.setAvgViewers(avgViewers != null ? avgViewers : 0.0);
        vo.setSource(monitorRecords > 0 ? "live_session+live_monitor" : "live_session");
        vo.setDegraded(monitorRecords == 0);
        vo.setFallbackReason(monitorRecords == 0 ? "live_monitor 无可聚合监控数据，返回场次计数和空指标" : "");
        return RESTResult.success(vo);
    }
}
