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
 * 订单保存 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSaveVO {

    private Long id;                    // 编辑时需要

    @NotBlank(message = "订单号不能为空")
    private String orderNo;             // 订单号（幂等）

    @NotNull(message = "产品 ID 不能为空")
    @Min(value = 1, message = "产品 ID 必须大于 0")
    private Long productId;             // 产品 ID

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于 0")
    private Integer quantity;           // 商品数量

    @NotNull(message = "订单金额不能为空")
    @Min(value = 1, message = "订单金额必须大于 0")
    private BigDecimal amount;          // 订单金额

    @NotNull(message = "实际支付金额不能为空")
    @Min(value = 1, message = "实际支付金额必须大于 0")
    private BigDecimal actualAmount;    // 实际支付金额

    private String remark;              // 备注
}
