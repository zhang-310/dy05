package cn.gaifan.douyinOperations.module.live.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * R-6：多直播间实时指标并排查询
 */
@Data
public class MultiSessionMetricsRequestVO {

    @NotEmpty(message = "至少选择一个场次")
    @Size(max = 8, message = "最多对比 8 个场次")
    private List<Long> sessionIds;
}
