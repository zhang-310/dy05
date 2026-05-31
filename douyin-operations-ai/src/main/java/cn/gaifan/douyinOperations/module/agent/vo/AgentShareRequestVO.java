package cn.gaifan.douyinOperations.module.agent.vo;

import lombok.Data;

/**
 * 分享请求 VO
 */
@Data
public class AgentShareRequestVO {
    /** 对话ID */
    private Long conversationId;

    /** 对话标题（可选，自动从对话获取） */
    private String title;

    /** 对话摘要（可选，AI生成或手动） */
    private String summary;

    /** 是否公开：1=公开, 0=私密 */
    private Integer isPublic = 1;

    /** 过期天数（可选，默认永不过期，7=7天过期） */
    private Integer expiresDays;
}
