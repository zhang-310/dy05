package cn.gaifan.douyinOperations.module.agent.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

/**
 * 工作流保存入参
 */
@Data
public class AgentWorkflowSaveVO {

    private Long id;

    @NotBlank(message = "工作流名称不能为空")
    @Size(max = 128, message = "工作流名称不超过128字")
    private String name;

    @Size(max = 512, message = "描述不超过512字")
    private String description;

    @NotNull(message = "步骤不能为空")
    @NotEmpty(message = "至少需要1个步骤")
    @Valid
    private List<StepVO> steps;

    private Integer status = 1;

    @Data
    public static class StepVO {
        private Long id;
        private Integer stepOrder;
        @NotNull(message = "智能体ID不能为空")
        private Long agentId;
        @Size(max = 128, message = "步骤名称不超过128字")
        private String stepName;
        @Size(max = 2000, message = "输入模板不超过2000字")
        private String inputTemplate;
        @Size(max = 128, message = "输出key不超过128字")
        private String outputKey;
        @Size(max = 512, message = "跳过条件不超过512字")
        private String skipCondition;
        /** 依赖的 outputKey 列表，如 ["step1", "step2"] */
        private List<String> dependsOn;
        /** 执行模式：0=顺序，1=强制并行 */
        private Integer executionMode = 0;
        /** 重试次数，默认1 */
        private Integer retryCount = 1;
        private Integer timeoutSeconds = 120;
    }
}
