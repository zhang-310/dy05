package cn.gaifan.douyinOperations.module.attribution.vo;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class AttributionTriggerVO {

    @NotNull(message = "场次 ID 不能为空")
    private Long sessionId;
}
