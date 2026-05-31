package cn.gaifan.douyinOperations.module.script.vo;

import java.math.BigDecimal;
import java.util.List;

public class ScriptOptimizationVO {
    private String optimizedContent;
    private BigDecimal originalScore;
    private BigDecimal optimizedScore;
    private List<String> suggestions;
    private Long generationTime;
    private String style;
    private String goal;

    public ScriptOptimizationVO() {
    }

    public ScriptOptimizationVO(
            String optimizedContent,
            BigDecimal originalScore,
            BigDecimal optimizedScore,
            List<String> suggestions,
            Long generationTime,
            String style,
            String goal) {
        this.optimizedContent = optimizedContent;
        this.originalScore = originalScore;
        this.optimizedScore = optimizedScore;
        this.suggestions = suggestions;
        this.generationTime = generationTime;
        this.style = style;
        this.goal = goal;
    }

    public String getOptimizedContent() {
        return optimizedContent;
    }

    public void setOptimizedContent(String optimizedContent) {
        this.optimizedContent = optimizedContent;
    }

    public BigDecimal getOriginalScore() {
        return originalScore;
    }

    public void setOriginalScore(BigDecimal originalScore) {
        this.originalScore = originalScore;
    }

    public BigDecimal getOptimizedScore() {
        return optimizedScore;
    }

    public void setOptimizedScore(BigDecimal optimizedScore) {
        this.optimizedScore = optimizedScore;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public Long getGenerationTime() {
        return generationTime;
    }

    public void setGenerationTime(Long generationTime) {
        this.generationTime = generationTime;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }
}
