package cn.gaifan.douyinOperations.module.benchmark.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Cookie验证请求VO
 */
@Data
public class ValidateCookieVO {

    /**
     * Cookie ID
     */
    @NotNull(message = "Cookie ID不能为空")
    private Long cookieId;
}
