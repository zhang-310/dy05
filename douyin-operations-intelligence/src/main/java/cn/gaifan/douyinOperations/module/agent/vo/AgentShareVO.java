package cn.gaifan.douyinOperations.module.agent.vo;

import lombok.Data;

/**
 * 分享信息 VO
 */
@Data
public class AgentShareVO {
    /** 分享ID */
    private Long id;

    /** 分享码 */
    private String shareCode;

    /** 对话ID */
    private Long conversationId;

    /** 智能体ID */
    private Long agentId;

    /** 智能体名称 */
    private String agentName;

    /** 分享人名称 */
    private String ownerName;

    /** 对话标题 */
    private String title;

    /** 对话摘要 */
    private String summary;

    /** 消息数量 */
    private Integer messageCount;

    /** 浏览次数 */
    private Integer viewCount;

    /** 是否公开 */
    private Integer isPublic;

    /** 过期时间 */
    private String expiresAt;

    /** 创建时间 */
    private String createTime;

    /** 分享链接（前端拼接） */
    private String shareUrl;
}
