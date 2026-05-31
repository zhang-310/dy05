package cn.gaifan.douyinOperations.module.payment.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.payment.vo.PaymentRefundSearchVO;
import cn.gaifan.douyinOperations.module.payment.vo.PaymentRefundVO;
import cn.gaifan.douyinOperations.module.payment.vo.RefundSaveVO;
import cn.gaifan.douyinOperations.module.payment.vo.RefundVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 退款服务接口
 */
public interface RefundService {

    /**
     * 创建退款申请
     */
    long createRefund(RefundSaveVO vo, Long userId);

    /**
     * 批准退款
     */
    void approveRefund(Long refundId);

    /**
     * 拒绝退款
     */
    void rejectRefund(Long refundId, String reason);

    /**
     * 完成退款
     */
    void completeRefund(Long refundId);

    /**
     * 查询退款详情
     */
    RefundVO getRefund(Long refundId, Long userId);

    /**
     * 查询订单的所有退款
     */
    List<RefundVO> getRefundsByOrderId(Long orderId, Long userId);

    /**
     * 查询当前租户退款分页
     */
    PageResultVO<PaymentRefundVO> searchRefunds(PaymentRefundSearchVO vo, Long userId);

    /**
     * 计算订单已退款金额
     */
    BigDecimal calculateRefundedAmount(Long orderId);

    /**
     * 检查是否可以退款
     */
    boolean canRefund(Long orderId, BigDecimal amount);
}
