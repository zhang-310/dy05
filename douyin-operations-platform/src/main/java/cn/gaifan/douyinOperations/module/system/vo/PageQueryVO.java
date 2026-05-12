package cn.gaifan.douyinOperations.module.system.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * P1-6: 分页查询请求 VO（强类型，避免 Map 手动转换）
 */
@Data
public class PageQueryVO {

    @Min(value = 0, message = "页码必须大于等于 0")
    private Integer page = 0;

    @Min(value = 1, message = "每页行数必须大于 0")
    @Max(value = 1000, message = "每页行数不能超过 1000")
    private Integer rows = 30;
}
