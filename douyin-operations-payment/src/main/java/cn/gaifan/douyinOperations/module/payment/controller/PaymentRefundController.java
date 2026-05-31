package cn.gaifan.douyinOperations.module.payment.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.payment.service.RefundService;
import cn.gaifan.douyinOperations.module.payment.vo.PaymentRefundSearchVO;
import cn.gaifan.douyinOperations.module.payment.vo.PaymentRefundVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 支付退款 Controller
 */
@RestController
@RequestMapping("/api/v1/payment/refund")
public class PaymentRefundController {

    @Resource
    private RefundService refundService;

    @PostMapping("/search")
    public RESTResult<PageResultVO<PaymentRefundVO>> search(@Valid @RequestBody(required = false) PaymentRefundSearchVO vo,
            HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return RESTResult.success(refundService.searchRefunds(vo, userId));
    }
}
