package cn.gaifan.douyinOperations.module.script.vo;

import java.math.BigDecimal;

public class ScriptVariantVO {
    private String id;
    private String content;
    private BigDecimal score;
    private String keyPoints;
    private Integer likes;
    private Integer uses;

    public ScriptVariantVO() {}

    public ScriptVariantVO(String id, String content, BigDecimal score, String keyPoints) {
        this.id = id;
        this.content = content;
        this.score = score;
        this.keyPoints = keyPoints;
        this.likes = 0;
        this.uses = 0;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }

    public String getKeyPoints() { return keyPoints; }
    public void setKeyPoints(String keyPoints) { this.keyPoints = keyPoints; }

    public Integer getLikes() { return likes; }
    public void setLikes(Integer likes) { this.likes = likes; }

    public Integer getUses() { return uses; }
    public void setUses(Integer uses) { this.uses = uses; }
}
