package cn.gaifan.douyinOperations.module.shortvideo.vo;

import jakarta.validation.constraints.NotNull;

/** 通用视频 ID 请求 VO — 用于需要 videoId 的接口 */
public class VideoIdQueryVO {

    @NotNull(message = "videoId 不能为空")
    private Long videoId;

    public Long getVideoId() { return videoId; }
    public void setVideoId(Long videoId) { this.videoId = videoId; }
}
