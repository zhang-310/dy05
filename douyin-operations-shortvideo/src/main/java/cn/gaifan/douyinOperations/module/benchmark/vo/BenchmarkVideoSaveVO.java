package cn.gaifan.douyinOperations.module.benchmark.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对标视频保存VO
 */
@Data
public class BenchmarkVideoSaveVO {

    /**
     * ID（更新时必填）
     */
    private Long id;

    /**
     * 账号ID
     */
    @NotNull(message = "账号ID不能为空")
    private Long benchmarkAccountId;

    /**
     * 视频ID
     */
    @NotBlank(message = "视频ID不能为空")
    private String videoId;

    /**
     * 标题
     */
    private String title;

    /**
     * 描述
     */
    private String description;

    /**
     * 封面URL
     */
    private String coverUrl;

    /**
     * 视频URL
     */
    private String videoUrl;

    /**
     * 时长（秒）
     */
    private Integer duration;

    /**
     * 播放量
     */
    private Long viewCount;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 分享数
     */
    private Integer shareCount;

    /**
     * 收藏数
     */
    private Integer favoriteCount;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 是否符合条件
     */
    private Boolean isQualified;

    /**
     * 分析状态
     */
    private String analysisStatus;

    /**
     * 本地视频路径
     */
    private String localVideoPath;

    /**
     * BOS视频URL
     */
    private String bosVideoUrl;
}
