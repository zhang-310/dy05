package cn.gaifan.douyinOperations.module.agent.vo;

import lombok.Data;
import java.sql.Timestamp;
import java.util.List;

/**
 * 工作流返回值
 */
@Data
public class AgentWorkflowVO {
    private Long id;
    private Long userId;
    private String name;
    private String description;
    private Integer version;
    private Integer status;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private List<AgentWorkflowStepVO> steps;
}
