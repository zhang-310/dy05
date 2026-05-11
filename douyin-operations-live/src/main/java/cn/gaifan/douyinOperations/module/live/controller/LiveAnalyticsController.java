package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.vo.LiveAnalyticsOverviewVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 直播数据分析 Controller (Stub)
 */
@RestController
@RequestMapping("/api/v1/live/analytics")
public class LiveAnalyticsController {

    @PostMapping("/overview")
    public RESTResult<LiveAnalyticsOverviewVO> overview() {
        LiveAnalyticsOverviewVO vo = new LiveAnalyticsOverviewVO();
        vo.setTotalSessions(0L);
        vo.setTotalGmv(0.0);
        vo.setAvgViewers(0.0);
        return RESTResult.success(vo);
    }
}
