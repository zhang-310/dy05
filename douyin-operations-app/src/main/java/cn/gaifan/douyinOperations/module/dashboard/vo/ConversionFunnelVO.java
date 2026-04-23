package cn.gaifan.douyinOperations.module.dashboard.vo;

import lombok.Data;

import java.util.List;

/**
 * 转化漏斗 VO：曝光 → 互动 → 加购 → 成交
 */
@Data
public class ConversionFunnelVO {
    private int lookbackDays;
    private List<FunnelStepVO> steps;

    @Data
    public static class FunnelStepVO {
        private String name;
        private long value;
        private double rate;

        public FunnelStepVO(String name, long value, double rate) {
            this.name = name;
            this.value = value;
            this.rate = rate;
        }
    }
}
