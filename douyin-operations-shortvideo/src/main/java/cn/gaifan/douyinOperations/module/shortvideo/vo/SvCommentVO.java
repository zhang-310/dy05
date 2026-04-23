package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class SvCommentVO {
    private Long id;
    private Long videoId;
    private String douyinCommentId;
    private String content;
    private String authorName;
    private String authorAvatar;
    private Integer likeCount;
    private Integer replyCount;
    private String sentiment;
    private BigDecimal sentimentScore;
    private Timestamp commentTime;
    private Timestamp createTime;
}
