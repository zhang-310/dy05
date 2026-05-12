package cn.gaifan.douyinOperations.module.system.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * P1-6: 告警记录 ID 请求 VO（强类型，避免 Map 手动转换）
 */
@Data
public class AlertRecordIdVO {

    @NotNull(message = "记录 ID 不能为空")
    private Long recordId;
}
