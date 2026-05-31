/**
 * W-11 支付系统 - 抖音支付 Service
 * 支付 SDK 集成、订单创建、支付回调、交易对账
 */

package cn.gaifan.douyinOperations.module.payment.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.gaifan.douyinOperations.module.payment.entity.*;
import cn.gaifan.douyinOperations.module.payment.config.PaymentGatewayProperties;
import cn.gaifan.douyinOperations.module.payment.repository.PaymentOrderRepository;
import cn.gaifan.douyinOperations.module.payment.repository.PaymentTransactionLogRepository;
import cn.gaifan.douyinOperations.module.payment.vo.OrderSaveVO;
import cn.gaifan.douyinOperations.contract.commerce.PaymentCreditGrantPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 抖音支付 Service
 * - 集成抖音支付 SDK
 * - 订单创建和支付链接生成
 * - 支付回调处理和验签
 * - 交易对账
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DouyinPaymentService {

    private final OrderService orderService;
    private final PaymentOrderRepository orderRepository;
    private final PaymentTransactionLogRepository transactionLogRepository;
    private final PaymentGatewayProperties paymentGatewayProperties;

    @Autowired(required = false)
    private PaymentCreditGrantPort paymentCreditGrantPort;

    /**
     * 支付配置常量
     */
    public static class PaymentConfig {
        // 抖音支付配置
        public static final String MERCHANT_ID = System.getenv("DOUYIN_MERCHANT_ID");
        public static final String MERCHANT_SECRET = System.getenv("DOUYIN_MERCHANT_SECRET");
        public static final String APP_ID = System.getenv("DOUYIN_APP_ID");

        // 环境隔离
        public static final boolean IS_SANDBOX = "sandbox".equals(System.getenv("PAYMENT_ENV"));
        public static final String API_ENDPOINT = IS_SANDBOX
                ? "https://payapi-sandbox.douyin.com"
                : "https://payapi.douyin.com";

        // 支付参数
        public static final String CURRENCY = "CNY";      // 人民币
        public static final int TIMEOUT_MINUTES = 30;      // 支付超时（分钟）
        public static final int ORDER_RETRY_TIMES = 3;    // 重试次数
    }

    /**
     * 创建订单并生成支付链接
     */
    public PaymentResponse createOrder(CreateOrderRequest request) {
        // P1-3: 敏感信息脱敏 - 不记录金额到日志
        log.info("📦 创建订单：userId={}, productId={}", request.userId, request.productId);

        try {
            if (request.userId == null || request.productId == null || request.amount == null) {
                return PaymentResponse.builder().success(false).errorMessage("参数不完整").build();
            }

            String orderNo = StringUtils.hasText(request.orderNo) ? request.orderNo : generateOrderNo(request.userId);
            PaymentOrder existingOrder = orderRepository.findByOrderNo(orderNo).orElse(null);
            if (existingOrder != null) {
                log.info("✓ 订单已存在（幂等）：orderNo={}", orderNo);
                return createPaymentLink(existingOrder);
            }

            int quantity = request.quantity != null ? request.quantity : 1;
            BigDecimal actualAmount = calculateActualAmount(request.amount, request.discountCode);
            OrderSaveVO vo = OrderSaveVO.builder()
                    .orderNo(orderNo)
                    .productId(request.productId)
                    .quantity(quantity)
                    .amount(request.amount)
                    .actualAmount(actualAmount)
                    .remark(request.remark)
                    .build();
            long orderId = orderService.createOrder(vo, request.userId);
            PaymentOrder order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalStateException("订单创建后未找到: " + orderId));
            PaymentResponse response = createPaymentLink(order);

            log.info("✓ 订单创建成功：orderNo={}, paymentUrl={}", orderNo, response.paymentUrl);
            return response;

        } catch (Exception e) {
            log.error("✗ 订单创建失败", e);
            throw new RuntimeException("订单创建失败", e);
        }
    }

    /**
     * 创建抖音支付链接
     */
    private PaymentResponse createPaymentLink(PaymentOrder order) {
        log.info("💳 创建抖音支付链接：orderId={}", order.getId());

        try {
            boolean mockMode = paymentGatewayProperties == null
                    || "mock".equalsIgnoreCase(paymentGatewayProperties.getGatewayMode());
            if (!mockMode && (!StringUtils.hasText(PaymentConfig.MERCHANT_ID)
                    || !StringUtils.hasText(PaymentConfig.MERCHANT_SECRET)
                    || !StringUtils.hasText(PaymentConfig.APP_ID))) {
                return PaymentResponse.builder()
                        .success(false)
                        .orderNo(order.getOrderNo())
                        .amount(order.getActualAmount())
                        .errorMessage("抖音支付未配置商户凭证，无法创建真实支付链接")
                        .build();
            }

            // 构建支付请求
            Map<String, Object> paymentRequest = new HashMap<>();
            paymentRequest.put("out_order_no", order.getOrderNo());
            paymentRequest.put("total_amount", order.getActualAmount().intValue() * 100); // 转换为分
            paymentRequest.put("currency", PaymentConfig.CURRENCY);
            paymentRequest.put("product_name", "DY01 商品");
            paymentRequest.put("product_description", order.getRemark());
            paymentRequest.put("merchant_id", PaymentConfig.MERCHANT_ID);
            paymentRequest.put("app_id", PaymentConfig.APP_ID);

            if (!mockMode) {
                String signature = generateSignature(paymentRequest);
                paymentRequest.put("sign", signature);
                String paymentUrl = PaymentConfig.API_ENDPOINT + "/v1/order/create";
                log.debug("调用抖音支付 API：url={}", paymentUrl);
                return PaymentResponse.builder()
                        .success(false)
                        .orderNo(order.getOrderNo())
                        .amount(order.getActualAmount())
                        .errorMessage("抖音支付 HTTP 客户端未接入，订单已创建但未生成支付链接")
                        .build();
            }

            String douyinPaymentUrl = "/mock-payment/douyin?order_token=" +
                    generateOrderToken(order.getOrderNo());

            return PaymentResponse.builder()
                    .success(true)
                    .orderNo(order.getOrderNo())
                    .paymentUrl(douyinPaymentUrl)
                    .amount(order.getActualAmount())
                    .expiresAt(LocalDateTime.now().plusMinutes(PaymentConfig.TIMEOUT_MINUTES))
                    .build();

        } catch (Exception e) {
            log.error("✗ 支付链接创建失败", e);
            return PaymentResponse.builder()
                    .success(false)
                    .errorMessage("支付链接创建失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 处理支付回调（P1-1: 添加幂等性保护）
     */
    public void handlePaymentCallback(PaymentCallbackRequest callback) {
        log.info("🔔 处理支付回调：orderId={}, status={}", callback.orderId, callback.status);

        // P1-1: 使用 synchronized 块保证单机幂等性（简化实现，生产环境应使用 Redis 分布式锁）
        String lockKey = "payment:callback:" + callback.orderId;
        synchronized (lockKey.intern()) {
            try {
                // 1. 验证签名
                if (!verifySignature(callback)) {
                    log.error("✗ 回调签名验证失败");
                    throw new RuntimeException("签名验证失败");
                }

                // 2. 获取订单
                PaymentOrder order = orderRepository.findByOrderNo(callback.orderId).orElse(null);
                if (order == null) {
                    log.error("✗ 订单不存在：orderId={}", callback.orderId);
                    throw new RuntimeException("订单不存在");
                }

                // 3. 检查订单状态（幂等性保护 - 防止重复处理）
                if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
                    log.info("⚠️ 订单已处理，忽略回调：orderId={}, currentStatus={}",
                            callback.orderId, order.getStatus());
                    return;
                }

                // 4. 更新订单状态
                if ("SUCCESS".equals(callback.status)) {
                    order.setStatus(OrderStatus.PAID);
                    order.setPaidAt(LocalDateTime.now());
                    order.setPaymentMethod("抖音支付");
                    order.setTransactionId(callback.transactionId);

                    // 保存订单更新
                    orderRepository.save(order);

                    // 记录交易日志
                    recordTransaction(order.getId(), TransactionType.PAYMENT,
                            order.getActualAmount(), TransactionStatus.SUCCESS, callback.transactionId);

                    log.info("✓ 支付成功：orderId={}, transactionId={}",
                            order.getId(), callback.transactionId);

                    // 触发支付成功事件（发送邮件、更新用户配额等）
                    handlePaymentSuccess(order);

                } else if ("FAILED".equals(callback.status)) {
                    order.setStatus(OrderStatus.CANCELLED);
                    orderRepository.save(order);

                    // 记录失败事务
                    recordTransaction(order.getId(), TransactionType.PAYMENT,
                            order.getActualAmount(), TransactionStatus.FAILED, callback.transactionId);

                    log.warn("⚠️ 支付失败：orderId={}, reason={}",
                            order.getId(), callback.failureReason);
                }

            } catch (Exception e) {
                log.error("✗ 回调处理失败", e);
                // 需要重试机制
                throw new RuntimeException("回调处理失败", e);
            }
        }
    }

    /**
     * 交易对账
     * 每天定时与抖音支付对账，检查是否有遗漏的交易
     */
    public void dailyReconciliation() {
        log.info("🔄 开始每日交易对账...");

        try {
            // 1. 获取昨天的订单
            List<PaymentOrder> yesterdayOrders = getOrdersByDateRange(
                    LocalDateTime.now().minusDays(1),
                    LocalDateTime.now()
            );

            log.info("待对账订单数：{}", yesterdayOrders.size());

            // 2. 从抖音支付查询这些订单的状态
            for (PaymentOrder order : yesterdayOrders) {
                // 调用抖音支付 API 查询订单状态
                PaymentQueryResponse queryResult = queryPaymentStatus(order.getOrderNo());
                if (queryResult == null || queryResult.status == null) {
                    continue;
                }

                // 3. 对比本地状态和支付方返回的状态
                if ("SUCCESS".equals(queryResult.status) &&
                    order.getStatus() != OrderStatus.PAID) {

                    // 状态不一致，更新本地订单
                    log.warn("⚠️ 对账发现不一致：orderId={}, local={}, remote={}",
                            order.getId(), order.getStatus(), queryResult.status);

                    order.setStatus(OrderStatus.PAID);
                    order.setPaidAt(queryResult.paidAt);
                    orderRepository.save(order);

                    // 生成对账差异报告
                    recordReconciliationDifference(order.getId(), "status_mismatch");
                }
            }

            log.info("✓ 每日对账完成");

        } catch (Exception e) {
            log.error("✗ 对账失败", e);
            // 发送告警通知
            notifyReconciliationFailure(e);
        }
    }

    /**
     * 支付成功处理
     */
    private void handlePaymentSuccess(PaymentOrder order) {
        log.info("📧 触发支付成功事件：orderId={}", order.getId());

        try {
            if (paymentCreditGrantPort != null && order.getActualAmount() != null
                    && order.getActualAmount().signum() > 0) {
                String tenantId = order.getOrgId() != null ? "org-" + order.getOrgId() : "user-" + order.getUserId();
                String productHint = "douyin-ops";
                paymentCreditGrantPort.grantOnPayment(
                        tenantId,
                        "user-" + order.getUserId(),
                        order.getActualAmount(),
                        "payment-callback-" + order.getOrderNo(),
                        productHint + ":支付回调履约发放积分"
                );
            }
        } catch (Exception e) {
            log.error("✗ 支付履约积分发放失败", e);
        }
    }

    /**
     * 辅助方法：生成订单号
     * P1-6: 使用安全随机数生成不可预测的订单号
     */
    private String generateOrderNo(Long userId) {
        // 使用时间戳 + 随机数 + 用户哈希
        String timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));
        String userHash = String.format("%04d", userId % 10000);
        return "ORD" + timestamp + random + userHash;
    }

    /**
     * 辅助方法：计算实际金额（处理折扣）
     */
    private BigDecimal calculateActualAmount(BigDecimal amount, String discountCode) {
        if (discountCode != null && "NEWUSER".equals(discountCode)) {
            return amount.multiply(new BigDecimal("0.9")); // 9 折
        }
        return amount;
    }

    /**
     * 辅助方法：生成签名
     */
    private String generateSignature(Map<String, Object> params) {
        if (!StringUtils.hasText(PaymentConfig.MERCHANT_SECRET)) {
            throw new IllegalStateException("DOUYIN_MERCHANT_SECRET 未配置");
        }
        // 实现签名算法（MD5 / SHA256）
        // 这里是示例实现
        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> sb.append(k).append(v));
        sb.append(PaymentConfig.MERCHANT_SECRET);

        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                    PaymentConfig.MERCHANT_SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(secretKey);
            return bytesToHex(mac.doFinal(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("支付签名生成失败", e);
        }
    }

    /**
     * 辅助方法：生成订单令牌
     */
    private String generateOrderToken(String orderNo) {
        return "token_" + Base64.getEncoder().encodeToString(orderNo.getBytes());
    }

    /**
     * 辅助方法：验证回调签名
     */
    private boolean verifySignature(PaymentCallbackRequest callback) {
        try {
            if (callback.getSign() == null || callback.getSign().isEmpty()) {
                log.warn("签名为空");
                return false;
            }

            if (PaymentConfig.MERCHANT_SECRET == null || PaymentConfig.MERCHANT_SECRET.isEmpty()) {
                log.error("MERCHANT_SECRET 未配置");
                return false;
            }

            // 1. 按字典序排列参数
            Map<String, String> params = new TreeMap<>();
            params.put("orderId", callback.getOrderId());
            params.put("status", callback.getStatus());
            params.put("transactionId", callback.getTransactionId());
            params.put("amount", callback.getAmount().toString());

            // 2. 拼接签名字符串
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
            sb.append("key=").append(PaymentConfig.MERCHANT_SECRET);

            // 3. 计算签名（使用 SHA256 + HMAC）
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                PaymentConfig.MERCHANT_SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            mac.init(secretKey);
            byte[] hash = mac.doFinal(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String calculatedSign = bytesToHex(hash);

            // 4. 比较签名（防时序攻击）
            return java.security.MessageDigest.isEqual(
                calculatedSign.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                callback.getSign().getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("签名验证失败", e);
            return false;
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private List<PaymentOrder> getOrdersByDateRange(LocalDateTime start, LocalDateTime end) {
        return orderRepository.findAll((root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("createdAt"), start),
                cb.lessThanOrEqualTo(root.get("createdAt"), end)
        ));
    }

    /**
     * 查询支付方状态。未接入支付方查询接口时返回 null，不伪造支付成功。
     */
    private PaymentQueryResponse queryPaymentStatus(String orderNo) {
        log.debug("抖音支付状态查询接口未配置，跳过远端状态查询: orderNo={}", orderNo);
        return null;
    }

    private void recordTransaction(Long orderId, TransactionType type, BigDecimal amount,
                                   TransactionStatus status, String txId) {
        PaymentTransactionLog logEntity = PaymentTransactionLog.builder()
                .orderId(orderId)
                .type(type)
                .amount(amount)
                .status(status)
                .externalTransactionId(txId)
                .createdAt(LocalDateTime.now())
                .build();
        transactionLogRepository.save(logEntity);
    }

    private void recordReconciliationDifference(Long orderId, String reason) {
        log.warn("记录对账差异：orderId={}, reason={}", orderId, reason);
    }

    private void notifyReconciliationFailure(Exception e) {
        log.error("支付对账失败，需接入告警通道", e);
    }

    // DTO 类
    @lombok.Data
    @lombok.Builder
    public static class CreateOrderRequest {
        private Long userId;
        private String orderNo;
        private Long productId;
        private BigDecimal amount;
        private Integer quantity;
        private String discountCode;
        private String remark;
    }

    @lombok.Data
    @lombok.Builder
    public static class PaymentResponse {
        private boolean success;
        private String orderNo;
        private String paymentUrl;
        private BigDecimal amount;
        private LocalDateTime expiresAt;
        private String errorMessage;
    }

    @lombok.Data
    public static class PaymentCallbackRequest {
        private String orderId;
        private String status;
        private String transactionId;
        private BigDecimal amount;
        private String failureReason;
        private String sign;
    }

    @lombok.Data
    public static class PaymentQueryResponse {
        private String status;
        private String transactionId;
        private BigDecimal amount;
        private LocalDateTime paidAt;
    }
}
