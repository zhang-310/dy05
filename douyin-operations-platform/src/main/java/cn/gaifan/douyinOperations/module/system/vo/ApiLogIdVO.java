package cn.gaifan.douyinOperations.module.system.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * P1-9: API 日志 ID 请求 VO
 */
@Data
public class ApiLogIdVO {

    @NotNull(message = "日志 ID 不能为空")
    private Long id;
}
