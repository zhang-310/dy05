package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class SvCommentSaveVO {
    private Long id;
    @NotNull(message = "视频 ID 不能为空")
    private Long videoId;
    @NotBlank(message = "评论内容不能为空")
    private String content;
    private String douyinCommentId;
    private String authorName;
    private String authorAvatar;
    private String sentiment;
    private BigDecimal sentimentScore;
    private Timestamp commentTime;
}
