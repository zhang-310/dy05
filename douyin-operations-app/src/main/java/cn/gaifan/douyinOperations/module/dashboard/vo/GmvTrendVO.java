package cn.gaifan.douyinOperations.module.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * GMV 趋势响应（按日聚合的时间序列）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GmvTrendVO {

    private String granularity;
    private int lookbackDays;
    private List<GmvTrendPointVO> points;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GmvTrendPointVO {
        private String date;
        private BigDecimal gmv;
    }
}
