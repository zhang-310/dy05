package cn.gaifan.douyinOperations.module.payment.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 退款保存 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundSaveVO {

    private Long id;                // 编辑时需要

    @NotNull(message = "订单 ID 不能为空")
    @Min(value = 1, message = "订单 ID 必须大于 0")
    private Long orderId;           // 订单 ID

    @NotNull(message = "退款金额不能为空")
    @Min(value = 1, message = "退款金额必须大于 0")
    private BigDecimal amount;      // 退款金额

    @NotBlank(message = "退款原因不能为空")
    private String reason;          // 退款原因
}
