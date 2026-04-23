package cn.gaifan.douyinOperations.module.benchmark.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DouyinQrLoginSessionIdVO {

    @NotBlank(message = "sessionId 不能为空")
    private String sessionId;
}
