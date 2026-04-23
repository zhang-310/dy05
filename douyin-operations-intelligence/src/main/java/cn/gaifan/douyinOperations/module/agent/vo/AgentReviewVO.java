package cn.gaifan.douyinOperations.module.agent.vo;

import lombok.Data;
import java.sql.Timestamp;

/**
 * 智能体评分与评论返回值
 */
@Data
public class AgentReviewVO {
    private Long id;
    private Long agentId;
    private Long userId;
    private String userName;
    private Integer rating;
    private String content;
    private String replyContent;
    private Timestamp replyTime;
    private Timestamp createdAt;
}
