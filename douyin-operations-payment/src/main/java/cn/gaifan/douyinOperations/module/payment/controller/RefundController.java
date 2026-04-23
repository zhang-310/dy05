package cn.gaifan.douyinOperations.module.payment.controller;

import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.payment.service.RefundService;
import cn.gaifan.douyinOperations.module.payment.vo.RefundSaveVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.Map;

/**
 * 退款控制器
 */
@RestController
@RequestMapping("/api/v1/payment/refund")
public class RefundController {

    @Resource
    private RefundService refundService;

    /**
     * 创建退款申请
     */
    @PostMapping("/create")
    public RESTResult<?> createRefund(@Valid @RequestBody RefundSaveVO vo) {
        long refundId = refundService.createRefund(vo);
        return RESTResult.success(refundId);
    }

    /**
     * 获取退款详情
     */
    @PostMapping("/get")
    public RESTResult<?> getRefund(@RequestBody Map<String, Long> request) {
        Long refundId = request.get("refundId");
        return RESTResult.success(refundService.getRefund(refundId));
    }

    /**
     * 查询订单的所有退款
     */
    @PostMapping("/listByOrder")
    public RESTResult<?> listByOrder(@RequestBody Map<String, Long> request) {
        Long orderId = request.get("orderId");
        return RESTResult.success(refundService.getRefundsByOrderId(orderId));
    }

    /**
     * 批准退款
     */
    @PostMapping("/approve")
    public RESTResult<?> approveRefund(@RequestBody Map<String, Long> request) {
        Long refundId = request.get("refundId");
        refundService.approveRefund(refundId);
        return RESTResult.success();
    }

    /**
     * 拒绝退款
     */
    @PostMapping("/reject")
    public RESTResult<?> rejectRefund(@RequestBody Map<String, String> request) {
        Long refundId = Long.parseLong(request.get("refundId"));
        String reason = request.get("reason");
        refundService.rejectRefund(refundId, reason);
        return RESTResult.success();
    }

    /**
     * 完成退款
     */
    @PostMapping("/complete")
    public RESTResult<?> completeRefund(@RequestBody Map<String, Long> request) {
        Long refundId = request.get("refundId");
        refundService.completeRefund(refundId);
        return RESTResult.success();
    }
}
