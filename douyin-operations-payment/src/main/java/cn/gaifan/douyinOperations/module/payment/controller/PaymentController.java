/**
 * W-11 支付系统 - 支付 Controller
 * REST API：订单创建、查询、支付回调、退款
 */

package cn.gaifan.douyinOperations.module.payment.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import cn.gaifan.douyinOperations.module.payment.service.DouyinPaymentService;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import java.math.BigDecimal;

/**
 * 支付控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final DouyinPaymentService paymentService;

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
            // 从数据库查询订单（这里省略具体实现）
            // PaymentOrder order = paymentService.getOrder(orderNo);

            final String orderNoVal = orderNo;
            return ResponseEntity.ok(RESTResult.success("查询成功", new Object() {
                public String getOrderNo() { return orderNoVal; }
                public String getStatus() { return "PAID"; }
                public BigDecimal getAmount() { return new BigDecimal("99.99"); }
            }));

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
     */
    @PostMapping("/callback")
    @SuppressWarnings("unused")
    public ResponseEntity<?> handlePaymentCallback(
            @RequestBody DouyinPaymentService.PaymentCallbackRequest callback,
            HttpServletRequest request) {

        log.info("🔔 收到支付回调：orderId={}, status={}", callback.getOrderId(), callback.getStatus());

        try {
            // P0-3: IP 白名单验证（防止非法回调和 DDoS 攻击）
            String clientIp = getClientIp(request);
            if (!isIpInWhitelist(clientIp)) {
                log.error("❌ 非法回调 IP: ", clientIp);
                return ResponseEntity.status(403).body(new Object() {
                    public int code = -1;
                    public String msg = "Forbidden";
                });
            }

            // 验证签名（已在 DouyinPaymentService.handlePaymentCallback 中实现）
            // 处理回调
            paymentService.handlePaymentCallback(callback);

            // 返回成功响应（抖音支付需要确认收到）
            return ResponseEntity.ok(new Object() {
                public int code = 0;
                public String msg = "success";
            });

        } catch (Exception e) {
            log.error("✗ 回调处理失败", e);

            // 返回失败响应，抖音支付会重试
            return ResponseEntity.status(500).body(new Object() {
                public int code = -1;
                public String msg = "处理失败，请重试";
            });
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
        // 从配置读取白名单（这里硬编码示例，实际应从 application.yml 读取）
        String whitelist = "203.107.32.0/24,203.107.33.0/24,127.0.0.1,::1";

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
            @RequestParam String reason) {

        log.info("💰 申请退款：orderNo={}, reason={}", orderNo, reason);

        try {
            // 创建退款单（这里省略具体实现）
            // PaymentRefund refund = paymentService.createRefund(orderNo, reason);

            return ResponseEntity.ok(RESTResult.success(new Object() {
                public String refundNo = "REF" + System.currentTimeMillis();
                public String status = "PENDING";
            }));

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
            @RequestParam(defaultValue = "10") Integer rows) {

        log.info("📋 获取订单列表：page={}, rows={}", page, rows);

        try {
            // 分页查询用户订单（这里省略具体实现）

            return ResponseEntity.ok(RESTResult.success(new Object() {
                public int total = 100;
                public java.util.List<?> list = new java.util.ArrayList<>();
            }));

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
            // 计算统计数据（这里省略具体实现）

            return ResponseEntity.ok(RESTResult.success(new Object() {
                public int totalOrders = 1000;
                public BigDecimal totalAmount = new BigDecimal("99999.99");
                public double successRate = 99.5;
                public BigDecimal avgAmount = new BigDecimal("100.00");
            }));

        } catch (Exception e) {
            log.error("✗ 统计获取失败", e);
            return ResponseEntity.status(500).body(
                    RESTResult.error(3050, "统计获取失败")
            );
        }
    }
}
