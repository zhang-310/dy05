package cn.gaifan.douyinOperations.module.shortvideo.vo;

/** 发布管理 - 生成标题请求 VO */
public class GenerateTitleRequestVO {
    private Long projectId;
    private String videoUrl;
    private String script;
    private java.util.List<String> keywords;
    private Integer count;

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getScript() { return script; }
    public void setScript(String script) { this.script = script; }
    public java.util.List<String> getKeywords() { return keywords; }
    public void setKeywords(java.util.List<String> keywords) { this.keywords = keywords; }
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
}
