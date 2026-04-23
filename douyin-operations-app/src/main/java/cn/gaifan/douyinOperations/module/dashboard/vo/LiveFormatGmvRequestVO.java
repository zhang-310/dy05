package cn.gaifan.douyinOperations.module.dashboard.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * POST /dashboard/live-format-gmv 请求体：按已结束场次、按 live_format 聚合 GMV 的回溯天数。
 */
@Data
public class LiveFormatGmvRequestVO {

    @Min(1)
    @Max(730)
    private Integer lookbackDays = 90;
}
