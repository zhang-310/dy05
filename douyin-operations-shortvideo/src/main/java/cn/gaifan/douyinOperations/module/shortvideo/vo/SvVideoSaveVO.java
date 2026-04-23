package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;

@Data
public class SvVideoSaveVO {
    private Long id;
    @NotNull(message = "账号 ID 不能为空")
    private Long accountId;
    private Long ownerId;
    private String douyinVideoId;
    private String title;
    private String description;
    private String coverUrl;
    private String videoUrl;
    private Integer duration;
    private String tags;
    private Long categoryId;
    private Timestamp publishTime;
    private Boolean isViral;
    private Long aiCallLogId;
    private Boolean aiGenerated;
    private Long planId;
}
