package cn.gaifan.douyinOperations.module.ai.vo;

import lombok.Data;

import java.sql.Timestamp;

/**
 * 任务-模型配置列表/详情行：与 {@link cn.gaifan.douyinOperations.module.ai.entity.AiTaskModelConfig} 对齐，并附带模型展示名。
 */
@Data
public class TaskModelConfigRowVO {

    private Long id;
    private String taskCode;
    private String taskName;
    private String taskGroup;
    private Long primaryModelId;
    private Long fallbackModelId;
    private Long fallback2ModelId;
    private String primaryModelName;
    private String fallbackModelName;
    private String fallback2ModelName;
    private Integer timeoutSeconds;
    private Integer maxRetries;
    private Integer sortOrder;
    private Integer status;
    private Timestamp createTime;
    private Timestamp updateTime;
}
