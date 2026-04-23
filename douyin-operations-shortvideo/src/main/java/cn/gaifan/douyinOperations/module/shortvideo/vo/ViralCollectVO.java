package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

/**
 * 爆款视频收藏 VO
 * viralVideoId: 从平台爆款收藏时传入（写入 sv_viral_favorite）
 * 其他字段: 从外部添加时传入（新建 sv_viral_video）
 */
@Data
public class ViralCollectVO {
    /** 平台爆款 ID，收藏时传入则写入 sv_viral_favorite */
    private Long viralVideoId;
    private String douyinVideoId;  // 抖音视频ID（外部添加时）
    private String title;
    private String coverUrl;
    private String videoUrl;
    private String authorName;
    private Long viewCount;
    private Long likeCount;
    private Long shareCount;
    private String tags;
}
