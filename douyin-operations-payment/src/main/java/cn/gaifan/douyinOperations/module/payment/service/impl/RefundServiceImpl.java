package cn.gaifan.douyinOperations.module.payment.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.payment.entity.PaymentRefund;
import cn.gaifan.douyinOperations.module.payment.entity.PaymentOrder;
import cn.gaifan.douyinOperations.module.payment.entity.RefundStatus;
import cn.gaifan.douyinOperations.module.payment.repository.PaymentRefundRepository;
import cn.gaifan.douyinOperations.module.payment.repository.PaymentOrderRepository;
import cn.gaifan.douyinOperations.module.payment.service.RefundService;
import cn.gaifan.douyinOperations.module.payment.vo.RefundSaveVO;
import cn.gaifan.douyinOperations.module.payment.vo.RefundVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 退款服务实现
 */
@Service
public class RefundServiceImpl implements RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundServiceImpl.class);

    @Resource
    private PaymentRefundRepository refundRepository;

    @Resource
    private PaymentOrderRepository orderRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long createRefund(RefundSaveVO vo) {
        if (vo == null || vo.getOrderId() == null || vo.getOrderId() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "参数校验失败");
        }

        // 验证订单存在
        PaymentOrder order = orderRepository.findById(vo.getOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在"));

        // 检查是否可以退款
        if (!canRefund(vo.getOrderId(), vo.getAmount())) {
            throw new BusinessException(ErrorCode.REFUND_AMOUNT_EXCEED, "退款金额超过可退款余额");
        }

        // 创建退款记录
        PaymentRefund refund = PaymentRefund.builder()
                .orderId(vo.getOrderId())
                .amount(vo.getAmount())
                .reason(vo.getReason())
                .status(RefundStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        refund = refundRepository.save(refund);
        log.info("Refund created: refundId={}, orderId={}, amount={}", refund.getId(), refund.getOrderId(), refund.getAmount());
        return refund.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveRefund(Long refundId) {
        PaymentRefund refund = getRefundEntity(refundId);

        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new BusinessException(ErrorCode.REFUND_STATUS_INVALID, "退款状态不允许批准");
        }

        refund.setStatus(RefundStatus.APPROVED);
        refund.setApprovedAt(LocalDateTime.now());
        refundRepository.save(refund);
        log.info("Refund approved: refundId={}", refundId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectRefund(Long refundId, String reason) {
        PaymentRefund refund = getRefundEntity(refundId);

        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new BusinessException(ErrorCode.REFUND_STATUS_INVALID, "退款状态不允许拒绝");
        }

        refund.setStatus(RefundStatus.REJECTED);
        refund.setReason(reason);
        refundRepository.save(refund);
        log.info("Refund rejected: refundId={}", refundId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeRefund(Long refundId) {
        PaymentRefund refund = getRefundEntity(refundId);

        if (refund.getStatus() != RefundStatus.APPROVED) {
            throw new BusinessException(ErrorCode.REFUND_STATUS_INVALID, "只有已批准的退款才能完成");
        }

        refund.setStatus(RefundStatus.COMPLETED);
        refund.setCompletedAt(LocalDateTime.now());
        refundRepository.save(refund);
        log.info("Refund completed: refundId={}", refundId);
    }

    @Override
    public RefundVO getRefund(Long refundId) {
        return convertToVO(getRefundEntity(refundId));
    }

    @Override
    public List<RefundVO> getRefundsByOrderId(Long orderId) {
        return refundRepository.findByOrderId(orderId).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal calculateRefundedAmount(Long orderId) {
        List<PaymentRefund> refunds = refundRepository.findByOrderId(orderId);
        return refunds.stream()
                .filter(r -> r.getStatus() == RefundStatus.COMPLETED || r.getStatus() == RefundStatus.APPROVED)
                .map(PaymentRefund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public boolean canRefund(Long orderId, BigDecimal requestAmount) {
        PaymentOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在"));

        BigDecimal refundedAmount = calculateRefundedAmount(orderId);
        BigDecimal refundableAmount = order.getActualAmount().subtract(refundedAmount);

        return requestAmount.compareTo(refundableAmount) <= 0 && requestAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    private PaymentRefund getRefundEntity(Long refundId) {
        return refundRepository.findById(refundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFUND_NOT_FOUND, "退款不存在"));
    }

    private RefundVO convertToVO(PaymentRefund refund) {
        return RefundVO.builder()
                .id(refund.getId())
                .orderId(refund.getOrderId())
                .amount(refund.getAmount())
                .status(refund.getStatus().name())
                .reason(refund.getReason())
                .createdAt(refund.getCreatedAt())
                .approvedAt(refund.getApprovedAt())
                .completedAt(refund.getCompletedAt())
                .build();
    }
}
