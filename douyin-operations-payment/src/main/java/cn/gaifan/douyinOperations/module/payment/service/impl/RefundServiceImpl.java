package cn.gaifan.douyinOperations.module.payment.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.tenant.TenantOrgResolutionHelper;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.payment.entity.PaymentRefund;
import cn.gaifan.douyinOperations.module.payment.entity.PaymentOrder;
import cn.gaifan.douyinOperations.module.payment.entity.RefundStatus;
import cn.gaifan.douyinOperations.module.payment.entity.OrderStatus;
import cn.gaifan.douyinOperations.module.payment.repository.PaymentRefundRepository;
import cn.gaifan.douyinOperations.module.payment.repository.PaymentOrderRepository;
import cn.gaifan.douyinOperations.module.payment.service.RefundService;
import cn.gaifan.douyinOperations.module.payment.vo.PaymentRefundSearchVO;
import cn.gaifan.douyinOperations.module.payment.vo.PaymentRefundVO;
import cn.gaifan.douyinOperations.module.payment.vo.RefundSaveVO;
import cn.gaifan.douyinOperations.module.payment.vo.RefundVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    @Resource
    private TenantOrgResolutionHelper tenantOrgResolutionHelper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long createRefund(RefundSaveVO vo, Long userId) {
        if (vo == null || vo.getOrderId() == null || vo.getOrderId() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "参数校验失败");
        }
        Long ownerId = requireOwnerId(userId);

        // 验证订单存在
        PaymentOrder order = orderRepository.findById(vo.getOrderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在"));
        if (!Objects.equals(order.getUserId(), userId) || !isSameOwner(order, ownerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问该订单");
        }

        // 检查是否可以退款
        if (!canRefund(vo.getOrderId(), vo.getAmount())) {
            throw new BusinessException(ErrorCode.REFUND_AMOUNT_EXCEED, "退款金额超过可退款余额");
        }

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
    public RefundVO getRefund(Long refundId, Long userId) {
        PaymentRefund refund = getRefundEntity(refundId);
        Long ownerId = requireOwnerId(userId);
        if (!refund.getOwnerId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问该退款");
        }
        return convertToVO(refund);
    }

    @Override
    public List<RefundVO> getRefundsByOrderId(Long orderId, Long userId) {
        // P0-6: 验证订单归属
        PaymentOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在"));
        Long ownerId = requireOwnerId(userId);
        if (!Objects.equals(order.getUserId(), userId) || !isSameOwner(order, ownerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限访问该订单");
        }

        return refundRepository.findByOrderId(orderId).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public PageResultVO<PaymentRefundVO> searchRefunds(PaymentRefundSearchVO vo, Long userId) {
        if (vo == null) {
            vo = new PaymentRefundSearchVO();
        }
        vo.validateParams();
        Long ownerId = requireOwnerId(userId);

        RefundStatus status = parseStatus(vo.getStatus());
        String keyword = vo.getKeyword() == null ? "" : vo.getKeyword().trim();
        Long keywordId = parseLong(keyword);

        Specification<PaymentRefund> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("ownerId"), ownerId));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (keywordId != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("id"), keywordId),
                        cb.equal(root.get("orderId"), keywordId)
                ));
            } else if (!keyword.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("reason")), "%" + keyword.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort.Direction direction = "asc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortName = normalizeRefundSortName(vo.getSortName());
        Page<PaymentRefund> page = refundRepository.findAll(spec, PageRequest.of(vo.getPage(), vo.getRows(), Sort.by(direction, sortName)));

        Map<Long, PaymentOrder> ordersById = loadOrders(page.getContent());
        List<PaymentRefundVO> list = page.getContent().stream()
                .map(refund -> convertToPaymentRefundVO(refund, ordersById.get(refund.getOrderId())))
                .collect(Collectors.toList());

        return PageResultVO.of(page.getTotalElements(), list, vo.getPage(), vo.getRows());
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

    private RefundStatus parseStatus(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return RefundStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Long parseLong(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizeRefundSortName(String sortName) {
        if ("createTime".equals(sortName)) {
            return "createdAt";
        }
        if ("amount".equals(sortName) || "status".equals(sortName) || "orderId".equals(sortName)) {
            return sortName;
        }
        return "createdAt";
    }

    private Map<Long, PaymentOrder> loadOrders(List<PaymentRefund> refunds) {
        List<Long> orderIds = refunds.stream()
                .map(PaymentRefund::getOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, PaymentOrder> ordersById = new HashMap<>();
        orderRepository.findAllById(orderIds).forEach(order -> ordersById.put(order.getId(), order));
        return ordersById;
    }

    private Long requireOwnerId(Long userId) {
        Long orgId = tenantOrgResolutionHelper.organizationIdForUser(userId);
        if (orgId == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户未绑定组织，无法访问支付数据");
        }
        return orgId;
    }

    private boolean isSameOwner(PaymentOrder order, Long ownerId) {
        return Objects.equals(order.getOwnerId(), ownerId)
                || (order.getOrgId() != null && Objects.equals(order.getOrgId(), ownerId));
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

    private PaymentRefundVO convertToPaymentRefundVO(PaymentRefund refund, PaymentOrder order) {
        PaymentRefundVO vo = new PaymentRefundVO();
        vo.setId(refund.getId());
        vo.setOrderId(refund.getOrderId());
        vo.setOrderNo(order != null ? order.getOrderNo() : null);
        vo.setRefundNo("R" + refund.getId());
        vo.setTransactionNo(order != null ? order.getTransactionId() : null);
        vo.setAmount(refund.getAmount());
        vo.setStatus(refund.getStatus() != null ? refund.getStatus().name() : null);
        vo.setReason(refund.getReason());
        vo.setCreateTime(refund.getCreatedAt());
        vo.setApprovedAt(refund.getApprovedAt());
        vo.setCompletedAt(refund.getCompletedAt());
        return vo;
    }
}
