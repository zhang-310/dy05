package cn.gaifan.douyinOperations.module.payment.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 支付交易查询 VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentTransactionSearchVO extends BasicQueryDto {
    private String keyword;
    private String status;
    private String paymentMethod;
}
