package cn.gaifan.douyinOperations.module.abtest.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class AbEventSaveVO {
    @NotNull(message = "实验 ID 不能为空")
    private Long experimentId;
    @NotNull(message = "变体 ID 不能为空")
    private Long variantId;
    @NotBlank(message = "事件类型不能为空")
    private String eventType;
    @NotBlank(message = "用户指纹不能为空")
    private String userFingerprint;
    private String sessionId;
}
