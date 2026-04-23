package cn.gaifan.douyinOperations.module.benchmark.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对标视频返回VO
 */
@Data
public class BenchmarkVideoVO {

    private Long id;
    private Long benchmarkAccountId;
    private String videoId;
    private String title;
    private String description;
    private String coverUrl;
    private String videoUrl;
    private Integer duration;
    private Long viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer shareCount;
    private Integer favoriteCount;
    private LocalDateTime publishTime;
    private Boolean isQualified;
    private String analysisStatus;
    private String localVideoPath;
    private String bosVideoUrl;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
