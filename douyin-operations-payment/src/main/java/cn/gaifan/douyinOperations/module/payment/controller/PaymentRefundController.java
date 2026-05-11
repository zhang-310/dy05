package cn.gaifan.douyinOperations.module.payment.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.payment.vo.PaymentRefundSearchVO;
import cn.gaifan.douyinOperations.module.payment.vo.PaymentRefundVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

/**
 * 支付退款 Controller
 */
@RestController
@RequestMapping("/api/v1/payment/refund")
public class PaymentRefundController {

    @PostMapping("/search")
    public RESTResult<PageResultVO<PaymentRefundVO>> search(@Valid @RequestBody PaymentRefundSearchVO vo) {
        vo.validateParams();
        return RESTResult.success(new PageResultVO<>(0L, Collections.emptyList(), vo.getPage(), vo.getRows()));
    }
}
