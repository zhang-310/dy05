package cn.gaifan.douyinOperations.module.product.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SalesHistorySearchVO extends BasicQueryDto {
    private Long productId;
    private String channelSource;
    private String sessionId;
    /** 销售时间起始（ISO 8601 或 yyyy-MM-dd） */
    private String startTime;
    /** 销售时间截止（ISO 8601 或 yyyy-MM-dd） */
    private String endTime;
}
