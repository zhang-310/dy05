package cn.gaifan.douyinOperations.module.payment.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 支付退款查询 VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentRefundSearchVO extends BasicQueryDto {
    private String keyword;
    private String status;
}
