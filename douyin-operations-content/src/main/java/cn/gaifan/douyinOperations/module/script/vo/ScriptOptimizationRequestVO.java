package cn.gaifan.douyinOperations.module.script.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ScriptOptimizationRequestVO {
    @NotBlank(message = "原始话术不能为空")
    @Size(max = 5000, message = "原始话术不超过5000字")
    private String originalContent;

    @Size(max = 100, message = "优化目标不超过100字")
    private String goal;

    @Size(max = 50, message = "优化风格不超过50字")
    private String style;

    public String getOriginalContent() {
        return originalContent;
    }

    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }
}
