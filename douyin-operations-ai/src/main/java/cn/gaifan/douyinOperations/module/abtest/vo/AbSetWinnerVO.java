package cn.gaifan.douyinOperations.module.abtest.vo;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class AbSetWinnerVO {
    @NotNull(message = "实验 ID 不能为空")
    private Long experimentId;
    @NotNull(message = "获胜变体 ID 不能为空")
    private Long variantId;
    private String conclusion;
}
