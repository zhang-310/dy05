package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * 话术行内评论响应 VO
 */
@Data
public class LiveScriptCommentVO {

    private Long id;
    private Long scriptId;
    private Long sessionId;
    private Long userId;
    private String userName;
    private String content;
    private Integer resolved;
    private Long resolvedBy;
    private LocalDateTime resolvedAt;
    private Long parentId;
    private Timestamp createTime;
    private Timestamp updateTime;
}
