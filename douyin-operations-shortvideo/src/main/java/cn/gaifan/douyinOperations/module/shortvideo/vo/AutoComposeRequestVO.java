package cn.gaifan.douyinOperations.module.shortvideo.vo;

/** AI 编辑控制器 - 合成请求 VO */
public class AutoComposeRequestVO {
    private Long projectId;
    private java.util.List<String> videoUrls;
    private java.util.List<String> voiceClipUrls;
    private String scriptText;
    private java.util.List<java.util.Map<String, Object>> subtitles;
    private String bgmUrl;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public java.util.List<String> getVideoUrls() { return videoUrls; }
    public void setVideoUrls(java.util.List<String> videoUrls) { this.videoUrls = videoUrls; }
    public java.util.List<String> getVoiceClipUrls() { return voiceClipUrls; }
    public void setVoiceClipUrls(java.util.List<String> voiceClipUrls) { this.voiceClipUrls = voiceClipUrls; }
    public String getScriptText() { return scriptText; }
    public void setScriptText(String scriptText) { this.scriptText = scriptText; }
    public java.util.List<java.util.Map<String, Object>> getSubtitles() { return subtitles; }
    public void setSubtitles(java.util.List<java.util.Map<String, Object>> subtitles) { this.subtitles = subtitles; }
    public String getBgmUrl() { return bgmUrl; }
    public void setBgmUrl(String bgmUrl) { this.bgmUrl = bgmUrl; }
}
