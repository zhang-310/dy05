package cn.gaifan.douyinOperations.module.payment.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.payment.service.OrderService;
import cn.gaifan.douyinOperations.module.payment.vo.OrderSearchVO;
import cn.gaifan.douyinOperations.module.payment.vo.OrderSaveVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/api/v1/payment/order")
public class OrderController {

    @Resource
    private OrderService orderService;

    /**
     * 创建订单
     */
    @PostMapping("/create")
    public RESTResult<?> createOrder(@Valid @RequestBody OrderSaveVO vo, HttpServletRequest request) {
        long orderId = orderService.createOrder(vo, requireUserId(request));
        return RESTResult.success(orderId);
    }

    /**
     * 获取订单详情（P0-4: 验证归属）
     */
    @PostMapping("/get")
    public RESTResult<?> getOrder(@RequestBody Map<String, Long> request, HttpServletRequest httpRequest) {
        Long orderId = request.get("orderId");
        Long userId = requireUserId(httpRequest);
        return RESTResult.success(orderService.getOrder(orderId, userId));
    }

    /**
     * 按订单号获取（P0-4: 验证归属）
     */
    @PostMapping("/getByOrderNo")
    public RESTResult<?> getByOrderNo(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String orderNo = request.get("orderNo");
        Long userId = requireUserId(httpRequest);
        return RESTResult.success(orderService.getByOrderNo(orderNo, userId));
    }

    /**
     * 查询用户订单列表
     */
    @PostMapping("/list")
    public RESTResult<?> listOrders(@Valid @RequestBody OrderSearchVO vo, HttpServletRequest request) {
        return RESTResult.success(orderService.searchByUser(vo, requireUserId(request)));
    }

    /**
     * 确认支付
     */
    @PostMapping("/confirmPayment")
    public RESTResult<?> confirmPayment(@RequestBody Map<String, String> request) {
        Long orderId = Long.parseLong(request.get("orderId"));
        String transactionId = request.get("transactionId");
        String paymentMethod = request.get("paymentMethod");
        orderService.confirmPayment(orderId, transactionId, paymentMethod);
        return RESTResult.success();
    }

    /**
     * 发货
     */
    @PostMapping("/ship")
    public RESTResult<?> shipOrder(@RequestBody Map<String, String> request) {
        Long orderId = Long.parseLong(request.get("orderId"));
        String trackingNumber = request.get("trackingNumber");
        orderService.shipOrder(orderId, trackingNumber);
        return RESTResult.success();
    }

    /**
     * 完成订单
     */
    @PostMapping("/complete")
    public RESTResult<?> completeOrder(@RequestBody Map<String, Long> request) {
        Long orderId = request.get("orderId");
        orderService.completeOrder(orderId);
        return RESTResult.success();
    }

    /**
     * 取消订单
     */
    @PostMapping("/cancel")
    public RESTResult<?> cancelOrder(@RequestBody Map<String, Long> request) {
        Long orderId = request.get("orderId");
        orderService.cancelOrder(orderId);
        return RESTResult.success();
    }

    private Long requireUserId(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return userId;
    }
}
