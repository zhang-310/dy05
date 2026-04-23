package cn.gaifan.douyinOperations.module.dashboard.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 单条 live_format 聚合（已结束场次、live_session_data.total_revenue 汇总）。
 */
@Data
public class LiveFormatGmvRowVO {

    private String liveFormat;
    private long sessionCount;
    private BigDecimal totalGmv;
}
