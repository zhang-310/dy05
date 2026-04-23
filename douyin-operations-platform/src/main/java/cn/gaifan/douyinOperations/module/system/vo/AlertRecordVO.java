package cn.gaifan.douyinOperations.module.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 告警记录 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertRecordVO {

    private Long id;

    private Long ruleId;                // 规则 ID

    private String ruleName;            // 规则名称

    private String metricName;          // 指标名称

    private String message;             // 告警消息

    private String status;              // 触发/恢复

    private String severity;            // warning/critical

    private Double value;               // 指标值

    private Double threshold;           // 阈值

    private LocalDateTime triggeredAt;  // 触发时间

    private LocalDateTime resolvedAt;   // 恢复时间
}
