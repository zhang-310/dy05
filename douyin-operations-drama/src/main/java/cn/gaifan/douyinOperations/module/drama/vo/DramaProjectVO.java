package cn.gaifan.douyinOperations.module.drama.vo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class DramaProjectVO {
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private String genre;
    private String status;
    private String script;
    private Integer episodeCount;
    private String visibility;
    private Long costCredits;
    private Timestamp createTime;
    private Timestamp updateTime;
}
