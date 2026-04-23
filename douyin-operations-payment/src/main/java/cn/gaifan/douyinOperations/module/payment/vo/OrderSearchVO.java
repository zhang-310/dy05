package cn.gaifan.douyinOperations.module.payment.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单搜索 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSearchVO extends BasicQueryDto {

    private String status;      // 订单状态
    private Long productId;     // 产品 ID
    private String startDate;   // 开始日期
    private String endDate;     // 结束日期
}
