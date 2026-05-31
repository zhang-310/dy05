package cn.gaifan.douyinOperations.module.agent.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 智能体评论提交入参
 */
@Data
public class AgentReviewSaveVO {

    private Long id;

    @NotNull(message = "智能体ID不能为空")
    private Long agentId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最小为1星")
    @Max(value = 5, message = "评分最大为5星")
    private Integer rating;

    @Size(max = 1000, message = "评论内容不超过1000字")
    private String content;
}
