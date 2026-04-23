package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;
import java.util.List;

/**
 * AI 分析报告 VO
 */
@Data
public class LiveAnalysisVO {
    private String rating;
    private String summary;
    private List<String> highlights;
    private List<String> issues;
    private List<String> suggestions;
    private List<ScriptEffectiveness> scriptEffectiveness;

    @Data
    public static class ScriptEffectiveness {
        private Long scriptId;
        private String scriptType;
        private Integer viewerDelta;
        private Double score;
    }
}
