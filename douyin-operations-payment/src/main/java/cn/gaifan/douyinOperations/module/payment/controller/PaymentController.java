/**
 * W-11 支付系统 - 支付 Controller
 * REST API：订单创建、查询、支付回调、退款
 */

package cn.gaifan.douyinOperations.module.payment.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.annotation.Resource;
import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.payment.config.PaymentGatewayProperties;
import cn.gaifan.douyinOperations.module.payment.entity.OrderStatus;
import cn.gaifan.douyinOperations.module.payment.service.DouyinPaymentService;
import cn.gaifan.douyinOperations.module.payment.service.RefundService;
import cn.gaifan.douyinOperations.module.payment.entity.PaymentCallbackLog;
import cn.gaifan.douyinOperations.module.payment.entity.PaymentOrder;
import cn.gaifan.douyinOperations.module.payment.entity.TransactionStatus;
import cn.gaifan.douyinOperations.module.payment.repository.PaymentCallbackLogRepository;
import cn.gaifan.douyinOperations.module.payment.repository.PaymentOrderRepository;
import cn.gaifan.douyinOperations.module.payment.repository.PaymentTransactionLogRepository;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.payment.vo.RefundSaveVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 支付控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final DouyinPaymentService paymentService;
    private final PaymentGatewayProperties paymentGatewayProperties;
    private final PaymentOrderRepository orderRepository;
    private final PaymentTransactionLogRepository transactionLogRepository;
    private final RefundService refundService;

    @Resource
    private PaymentCallbackLogRepository callbackLogRepository;

    @Value("${app.payment.legacy.allow-unauthenticated-read:false}")
    private boolean allowUnauthenticatedLegacyRead;

    /**
     * 创建订单并生成支付链接
     * POST /api/v1/payment/create-order
     */
    @PostMapping("/create-order")
    public ResponseEntity<RESTResult<?>> createOrder(
            @Valid @RequestBody DouyinPaymentService.CreateOrderRequest request) {

        log.info("📦 创建订单请求：userId={}, productId={}", request.getUserId(), request.getProductId());

        try {
            // 参数校验
            if (request.getUserId() == null || request.getProductId() == null) {
                return ResponseEntity.badRequest().body(
                        RESTResult.error(1001, "userId 和 productId 不能为空")
                );
            }

            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(
                        RESTResult.error(1002, "订单金额必须大于 0")
                );
            }

            // 调用支付服务
            DouyinPaymentService.PaymentResponse response = paymentService.createOrder(request);

            if (response.isSuccess()) {
                return ResponseEntity.ok(RESTResult.success("订单创建成功", response));
            } else {
                return ResponseEntity.badRequest().body(
                        RESTResult.error(3001, response.getErrorMessage())
                );
            }

        } catch (Exception e) {
            log.error("✗ 订单创建失败", e);
            return ResponseEntity.status(500).body(
                    RESTResult.error(3000, "订单创建失败: " + e.getMessage())
            );
        }
    }

    /**
     * 查询订单状态
     * GET /api/v1/payment/order/:orderNo
     */
    @GetMapping("/order/{orderNo}")
    @SuppressWarnings("unused") // 匿名对象字段通过 JSON 序列化返回
    public ResponseEntity<RESTResult<?>> getOrder(@PathVariable String orderNo) {

        log.info("🔍 查询订单：orderNo={}", orderNo);

        try {
            PaymentOrder order = orderRepository.findByOrderNo(orderNo)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在"));
            return ResponseEntity.ok(RESTResult.success("查询成功", legacyOrderPayload(order)));

        } catch (Exception e) {
            log.error("✗ 订单查询失败", e);
            return ResponseEntity.status(500).body(
            RESTResult.error(3010, "订单查询失败")
            );
        }
    }

    /**
     * 支付回调处理（来自抖音支付）
     * POST /api/v1/payment/callback
     *
     * 注意：这是回调端点，抖音支付服务器会主动调用此接口
     * P1-7: 增强回调重试机制和日志记录
     */
    @PostMapping("/callback")
    @SuppressWarnings("unused")
    public ResponseEntity<?> handlePaymentCallback(
            @RequestBody DouyinPaymentService.PaymentCallbackRequest callback,
            HttpServletRequest request) {

        String callbackId = callback.getOrderId() + "_" + System.currentTimeMillis();
        log.info("🔔 收到支付回调：callbackId={}, orderId={}, status={}",
                callbackId, callback.getOrderId(), callback.getStatus());

        try {
            // P0-3: IP 白名单验证（防止非法回调和 DDoS 攻击）
            String clientIp = getClientIp(request);
            if (!isIpInWhitelist(clientIp)) {
                log.error("❌ 非法回调 IP: {}", clientIp);
                return ResponseEntity.status(403).body(new Object() {
                    public int code = -1;
                    public String msg = "Forbidden";
                });
            }

            // P1-7: 记录回调日志
            PaymentCallbackLog callbackLog = PaymentCallbackLog.builder()
                    .callbackId(callbackId)
                    .orderId(callback.getOrderId())
                    .status(callback.getStatus())
                    .requestBody(serializeCallback(callback))
                    .clientIp(clientIp)
                    .processStatus("PROCESSING")
                    .receivedAt(LocalDateTime.now())
                    .build();
            callbackLogRepository.save(callbackLog);

            // 验证签名（已在 DouyinPaymentService.handlePaymentCallback 中实现）
            // 处理回调
            paymentService.handlePaymentCallback(callback);

            // P1-7: 更新回调日志状态
            callbackLog.setProcessStatus("SUCCESS");
            callbackLog.setProcessedAt(LocalDateTime.now());
            callbackLogRepository.save(callbackLog);

            // 返回成功响应（抖音支付需要确认收到）
            return ResponseEntity.ok(new Object() {
                public int code = 0;
                public String msg = "success";
            });

        } catch (Exception e) {
            log.error("✗ 回调处理失败: callbackId={}", callbackId, e);

            // P1-7: 更新回调日志状态
            try {
                PaymentCallbackLog callbackLog = callbackLogRepository.findByCallbackId(callbackId)
                        .orElse(null);
                if (callbackLog != null) {
                    callbackLog.setProcessStatus("FAILED");
                    callbackLog.setErrorMessage(e.getMessage());
                    callbackLog.setProcessedAt(LocalDateTime.now());
                    callbackLogRepository.save(callbackLog);
                }

                // P1-7: 检查重试次数（最近 1 小时内）
                int retryCount = callbackLogRepository.countRecentCallbacks(
                        callback.getOrderId(), LocalDateTime.now().minusHours(1));
                if (retryCount >= 5) {
                    log.error("⚠️ 支付回调失败超过 5 次: orderId={}", callback.getOrderId());
                    // TODO: 发送告警通知
                    return ResponseEntity.ok(new Object() {
                        public int code = 0;
                        public String msg = "max retry exceeded";
                    });
                }
            } catch (Exception logError) {
                log.error("✗ 更新回调日志失败", logError);
            }

            // 返回失败响应，抖音支付会重试
            return ResponseEntity.status(500).body(new Object() {
                public int code = -1;
                public String msg = "处理失败，请重试";
            });
        }
    }

    /**
     * P1-7: 序列化回调对象为 JSON
     */
    private String serializeCallback(DouyinPaymentService.PaymentCallbackRequest callback) {
        try {
            return String.format("{\"orderId\":\"%s\",\"status\":\"%s\",\"transactionId\":\"%s\",\"amount\":%s}",
                    callback.getOrderId(), callback.getStatus(), callback.getTransactionId(), callback.getAmount());
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * P0-3: 获取客户端真实 IP（支持代理）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For 可能包含多个 IP，取第一个
            int index = ip.indexOf(',');
            if (index != -1) {
                return ip.substring(0, index).trim();
            }
            return ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * P0-3: 验证 IP 是否在白名单中（支持 CIDR）
     * 白名单配置示例：203.107.32.0/24,203.107.33.0/24,127.0.0.1
     */
    private boolean isIpInWhitelist(String clientIp) {
        String whitelist = paymentGatewayProperties.getDouyin().getCallbackIpWhitelist();

        if (whitelist == null || whitelist.isEmpty()) {
            log.warn("⚠️ IP 白名单未配置，允许所有 IP 访问");
            return true;
        }

        String[] allowedRanges = whitelist.split(",");
        for (String range : allowedRanges) {
            range = range.trim();
            if (range.isEmpty()) continue;

            // 精确匹配
            if (range.equals(clientIp)) {
                return true;
            }

            // CIDR 匹配（简化实现，仅支持 IPv4）
            if (range.contains("/")) {
                if (isIpInCidr(clientIp, range)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * P0-3: 检查 IP 是否在 CIDR 范围内（简化实现）
     */
    private boolean isIpInCidr(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            String networkIp = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);

            long ipLong = ipToLong(ip);
            long networkLong = ipToLong(networkIp);
            long mask = -1L << (32 - prefixLength);

            return (ipLong & mask) == (networkLong & mask);
        } catch (Exception e) {
            log.error("CIDR 解析失败: {}", cidr, e);
            return false;
        }
    }

    /**
     * P0-3: IP 地址转 long（用于 CIDR 匹配）
     */
    private long ipToLong(String ip) {
        String[] octets = ip.split("\\.");
        if (octets.length != 4) {
            throw new IllegalArgumentException("Invalid IP: " + ip);
        }
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result |= (Long.parseLong(octets[i]) << (24 - i * 8));
        }
        return result;
    }

    /**
     * 申请退款
     * POST /api/v1/payment/refund
     */
    @PostMapping("/refund")
    public ResponseEntity<RESTResult<?>> requestRefund(
            @RequestParam String orderNo,
            @RequestParam String reason,
            HttpServletRequest request) {

        log.info("💰 申请退款：orderNo={}, reason={}", orderNo, reason);

        try {
            Long userId = requireUserId(request);
            PaymentOrder order = orderRepository.findByOrderNo(orderNo)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "订单不存在"));
            long refundId = refundService.createRefund(RefundSaveVO.builder()
                    .orderId(order.getId())
                    .amount(order.getActualAmount())
                    .reason(reason)
                    .build(), userId);
            return ResponseEntity.ok(RESTResult.success(Map.of(
                    "refundId", refundId,
                    "status", "PENDING"
            )));

        } catch (Exception e) {
            log.error("✗ 退款申请失败", e);
            return ResponseEntity.status(500).body(
                    RESTResult.error(3020, "退款申请失败")
            );
        }
    }

    /**
     * 获取用户订单列表
     * GET /api/v1/payment/orders
     */
    @GetMapping("/orders")
    @SuppressWarnings("unused")
    public ResponseEntity<RESTResult<?>> listOrders(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer rows,
            HttpServletRequest request) {

        log.info("📋 获取订单列表：page={}, rows={}", page, rows);

        try {
            Long userId = currentUserId(request);
            PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.max(rows, 1),
                    Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<PaymentOrder> result = userId != null
                    ? orderRepository.findByUserIdAndDeleted(userId, 0, pageable)
                    : unauthenticatedLegacyPage(pageable);
            return ResponseEntity.ok(RESTResult.success(Map.of(
                    "total", result.getTotalElements(),
                    "list", result.getContent().stream().map(this::legacyOrderPayload).toList()
            )));

        } catch (Exception e) {
            log.error("✗ 订单列表查询失败", e);
            return ResponseEntity.status(500).body(
                    RESTResult.error(3030, "订单列表查询失败")
            );
        }
    }

    /**
     * 手动触发每日对账（仅限管理员）
     * POST /api/v1/payment/reconciliation
     */
    @PostMapping("/reconciliation")
    public ResponseEntity<RESTResult<?>> triggerReconciliation() {

        log.info("🔄 触发每日对账（管理员操作）");

        try {
            // 执行对账
            paymentService.dailyReconciliation();

            return ResponseEntity.ok(RESTResult.success("对账已启动", null));

        } catch (Exception e) {
            log.error("✗ 对账失败", e);
            return ResponseEntity.status(500).body(
                    RESTResult.error(3040, "对账失败")
            );
        }
    }

    /**
     * 获取支付统计
     * GET /api/v1/payment/stats
     */
    @GetMapping("/stats")
    @SuppressWarnings("unused")
    public ResponseEntity<RESTResult<?>> getPaymentStats(
            @RequestParam(defaultValue = "today") String period) {

        log.info("📊 获取支付统计：period={}", period);

        try {
            List<OrderStatus> paidStatuses = List.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.COMPLETED);
            long totalOrders = orderRepository.count();
            long paidOrders = orderRepository.countByStatusIn(paidStatuses);
            BigDecimal totalAmount = orderRepository.sumActualAmountByStatuses(paidStatuses);
            if (totalAmount == null) totalAmount = BigDecimal.ZERO;
            double successRate = totalOrders == 0 ? 0D : paidOrders * 100D / totalOrders;
            BigDecimal avgAmount = paidOrders == 0
                    ? BigDecimal.ZERO
                    : totalAmount.divide(BigDecimal.valueOf(paidOrders), 2, java.math.RoundingMode.HALF_UP);

            return ResponseEntity.ok(RESTResult.success(Map.of(
                    "period", period,
                    "totalOrders", totalOrders,
                    "paidOrders", paidOrders,
                    "failedTransactions", transactionLogRepository.countByStatus(TransactionStatus.FAILED),
                    "totalAmount", totalAmount,
                    "successRate", successRate,
                    "avgAmount", avgAmount,
                    "stub", false
            )));

        } catch (Exception e) {
            log.error("✗ 统计获取失败", e);
            return ResponseEntity.status(500).body(
                    RESTResult.error(3050, "统计获取失败")
            );
        }
    }

    private Map<String, Object> legacyOrderPayload(PaymentOrder order) {
        return Map.ofEntries(
                Map.entry("id", order.getId()),
                Map.entry("orderNo", order.getOrderNo()),
                Map.entry("userId", order.getUserId()),
                Map.entry("productId", order.getProductId()),
                Map.entry("amount", order.getAmount()),
                Map.entry("actualAmount", order.getActualAmount()),
                Map.entry("quantity", order.getQuantity()),
                Map.entry("status", order.getStatus().name()),
                Map.entry("paymentMethod", order.getPaymentMethod() == null ? "" : order.getPaymentMethod()),
                Map.entry("transactionId", order.getTransactionId() == null ? "" : order.getTransactionId()),
                Map.entry("paidAt", order.getPaidAt() == null ? "" : order.getPaidAt().toString()),
                Map.entry("trackingNumber", order.getTrackingNumber() == null ? "" : order.getTrackingNumber()),
                Map.entry("remark", order.getRemark() == null ? "" : order.getRemark()),
                Map.entry("createdAt", order.getCreatedAt() == null ? "" : order.getCreatedAt().toString())
        );
    }

    private Page<PaymentOrder> unauthenticatedLegacyPage(PageRequest pageable) {
        if (!allowUnauthenticatedLegacyRead) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return orderRepository.findAll(pageable);
    }

    private Long requireUserId(HttpServletRequest request) {
        Long userId = currentUserId(request);
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return userId;
    }

    private Long currentUserId(HttpServletRequest request) {
        return request != null ? AuthTokenFilter.getUserId(request) : null;
    }
}
