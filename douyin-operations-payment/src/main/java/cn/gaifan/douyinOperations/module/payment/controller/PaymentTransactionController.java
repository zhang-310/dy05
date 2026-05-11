package cn.gaifan.douyinOperations.module.payment.controller;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.payment.vo.PaymentTransactionSearchVO;
import cn.gaifan.douyinOperations.module.payment.vo.PaymentTransactionVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

/**
 * 支付交易 Controller
 */
@RestController
@RequestMapping("/api/v1/payment/transaction")
public class PaymentTransactionController {

    @PostMapping("/search")
    public RESTResult<PageResultVO<PaymentTransactionVO>> search(@Valid @RequestBody PaymentTransactionSearchVO vo) {
        vo.validateParams();
        return RESTResult.success(new PageResultVO<>(0L, Collections.emptyList(), vo.getPage(), vo.getRows()));
    }
}
