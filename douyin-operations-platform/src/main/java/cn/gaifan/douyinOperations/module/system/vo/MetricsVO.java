package cn.gaifan.douyinOperations.module.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 指标 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricsVO {

    private String name;                // 指标名称
    private String value;               // 指标值
    private String unit;                // 单位
    private String status;              // 状态（normal/warning/critical）
    private LocalDateTime timestamp;
}
