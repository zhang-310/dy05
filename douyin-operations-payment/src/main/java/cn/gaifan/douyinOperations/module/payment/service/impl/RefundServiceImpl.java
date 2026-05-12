package cn.gaifan.douyinOperations.module.payment.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.payment.entity.PaymentRefund;
import cn.gaifan.douyinOperations.module.payment.entity.PaymentOrder;
import cn.gaifan.douyinOperations.module.payment.entity.RefundStatus;
import cn.gaifan.douyinOperations.module.payment.entity.OrderStatus;
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

        // P0-6: 获取 ownerId（从订单继承）
        Long ownerId = order.getOwnerId();

        // 创建退款记录
        PaymentRefund refund = PaymentRefund.builder()
                .orderId(vo.getOrderId())
                .ownerId(ownerId)
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
        PaymentRefund refund = getRefundEntity(refundId);
        // P0-6: 验证 ownerId（数据隔离）
        Long ownerId = 1L; // TODO: 从上下文获取
        if (!refund.getOwnerId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问该退款");
        }
        return convertToVO(refund);
    }

    @Override
    public List<RefundVO> getRefundsByOrderId(Long orderId) {
        // P0-6: 验证订单归属
        PaymentOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在"));
        Long ownerId = 1L; // TODO: 从上下文获取
        if (!order.getOwnerId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问该订单");
        }

        return refundRepository.findByOrderId(orderId).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal calculateRefundedAmount(Long orderId) {
        // P1-11: 退款金额计算优化 - 使用数据库聚合查询替代内存计算
        // 原实现：查询所有退款记录到内存，然后过滤和求和（N+1 查询问题）
        // 优化后：使用数据库 SUM 聚合，减少内存占用和网络传输
        BigDecimal sum = refundRepository.sumRefundedAmountByOrderId(orderId);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    public boolean canRefund(Long orderId, BigDecimal requestAmount) {
        // P1-4: 退款金额验证增强
        // 1. 验证退款金额格式
        if (requestAmount == null || requestAmount.scale() > 2) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "退款金额格式错误");
        }

        if (requestAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "退款金额必须大于 0");
        }

        // 2. 获取订单
        PaymentOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在"));

        // 3. 验证订单状态（仅已支付订单可退款）
        if (order.getStatus() != OrderStatus.PAID &&
            order.getStatus() != OrderStatus.SHIPPED &&
            order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "订单状态不允许退款");
        }

        // 4. 计算可退款金额
        BigDecimal refundedAmount = calculateRefundedAmount(orderId);
        BigDecimal refundableAmount = order.getActualAmount().subtract(refundedAmount);

        return requestAmount.compareTo(refundableAmount) <= 0;
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
