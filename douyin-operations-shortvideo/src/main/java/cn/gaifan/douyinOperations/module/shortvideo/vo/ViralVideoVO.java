package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

import java.sql.Timestamp;

/**
 * 爆款视频返回 VO
 */
@Data
public class ViralVideoVO {

    private Long id;
    private Long ownerId;
    private Long svAccountId;
    private String douyinVideoId;
    private String title;
    private String coverUrl;
    private String videoUrl;
    private String authorName;

    // 数据统计
    private Long viewCount;
    private Long likeCount;
    private Long shareCount;
    private Long commentCount;
    private Long favoriteCount;

    // 视频信息
    private Integer videoDuration;
    private Integer viralScore;
    private String tags;
    private Timestamp publishTime;

    // 采集信息
    private Boolean autoCollected;
    private String collectSource;
    private Long collectTaskId;

    // 深度分析
    private String deepAnalyzeStatus;
    /** 列表接口不返回大 JSON，一般为 null；详情接口可带全文 */
    private String deepAnalysisResult;
    /** 细粒度进度文案或短 JSON，列表可返回 */
    private String deepAnalyzeProgress;
    private Timestamp deepAnalyzedAt;

    private Timestamp createTime;
    private Timestamp updateTime;
}
