package cn.gaifan.douyinOperations.module.payment.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.payment.service.RefundService;
import cn.gaifan.douyinOperations.module.payment.vo.RefundSaveVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
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
     * 批准退款（P0-5: 需管理员权限）
     */
    @PostMapping("/approve")
    public RESTResult<?> approveRefund(@RequestBody Map<String, Long> request, HttpServletRequest httpRequest) {
        Long refundId = request.get("refundId");
        Long userId = AuthTokenFilter.getUserId(httpRequest);

        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }

        // P0-5: 验证管理员权限（简化实现：检查用户ID是否为管理员）
        // 实际应该查询 auth_user 表的 role 字段或使用 Spring Security 的 @PreAuthorize
        // 这里暂时硬编码管理员 userId = 1（需根据实际业务调整）
        if (!isAdmin(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可批准退款");
        }

        refundService.approveRefund(refundId);
        return RESTResult.success();
    }

    /**
     * 拒绝退款（P0-5: 需管理员权限）
     */
    @PostMapping("/reject")
    public RESTResult<?> rejectRefund(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        Long refundId = Long.parseLong(request.get("refundId"));
        String reason = request.get("reason");
        Long userId = AuthTokenFilter.getUserId(httpRequest);

        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }

        // P0-5: 验证管理员权限
        if (!isAdmin(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可拒绝退款");
        }

        refundService.rejectRefund(refundId, reason);
        return RESTResult.success();
    }

    /**
     * 完成退款（P0-5: 需管理员权限）
     */
    @PostMapping("/complete")
    public RESTResult<?> completeRefund(@RequestBody Map<String, Long> request, HttpServletRequest httpRequest) {
        Long refundId = request.get("refundId");
        Long userId = AuthTokenFilter.getUserId(httpRequest);

        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }

        // P0-5: 验证管理员权限
        if (!isAdmin(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅管理员可完成退款");
        }

        refundService.completeRefund(refundId);
        return RESTResult.success();
    }

    /**
     * P0-5: 检查是否为管理员（简化实现）
     * TODO: 实际应该查询 auth_user 表的 role 字段或使用 Spring Security 的 @PreAuthorize
     */
    private boolean isAdmin(Long userId) {
        // 简化实现：userId = 1 为管理员
        // 实际应该注入 AuthUserService 查询用户角色
        return userId != null && userId == 1L;
    }
}
