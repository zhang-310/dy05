package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import java.util.List;

/**
 * 爆款视频页元数据（yt-dlp / Playwright 共用）。
 */
public record ViralVideoMetadata(
        Long viewCount,
        Long likeCount,
        Long favoriteCount,
        Long commentCount,
        Long shareCount,
        Integer videoDuration,
        String description,
        String authorId,
        String authorName,
        Long authorFollowers,
        String musicName,
        List<String> hashtags,
        String coverUrl,
        String rawJson
) {
}
