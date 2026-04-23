package cn.gaifan.douyinOperations.module.dashboard.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 全角色工作台业务数据（GMV / 场次），与 POST /api/v1/dashboard/business 对齐
 */
@Data
public class BusinessDashboardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 今日 GMV（live_product.revenue，按 create_time 落在今日） */
    private BigDecimal todayGmv = BigDecimal.ZERO;

    /** 昨日同日 GMV */
    private BigDecimal yesterdayGmv = BigDecimal.ZERO;

    /** 今日新建场次数 */
    private long todayLiveSessionCount;

    /** 直播中场次数（status=1） */
    private long ongoingLiveSessionCount;

    /** 优先展示的一场直播中会话（若无则为 null） */
    private BusinessOngoingSessionVO ongoingSession;
}
