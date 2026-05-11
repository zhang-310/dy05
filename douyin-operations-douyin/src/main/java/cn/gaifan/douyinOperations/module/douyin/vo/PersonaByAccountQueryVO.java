package cn.gaifan.douyinOperations.module.douyin.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 按账号查询人设请求参数
 */
@Data
public class PersonaByAccountQueryVO {

    @NotNull(message = "accountId 不能为空")
    @Min(value = 1, message = "accountId 必须大于 0")
    private Long accountId;
}
