package cn.gaifan.douyinOperations.module.payment.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.payment.service.UsageQuotaService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment/usage")
public class UsageQuotaController {

    @Resource
    private UsageQuotaService usageQuotaService;

    @PostMapping("/quota")
    public RESTResult<Map<String, Object>> quota(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return RESTResult.success(usageQuotaService.getUsageQuotaSummary(userId));
    }
}
