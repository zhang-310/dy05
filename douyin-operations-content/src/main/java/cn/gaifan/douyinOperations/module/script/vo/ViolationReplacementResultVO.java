package cn.gaifan.douyinOperations.module.script.vo;

import lombok.Data;

import java.util.List;

/**
 * 违规词替换建议结果 VO
 */
@Data
public class ViolationReplacementResultVO {

    /** 原始文本 */
    private String originalText;

    /** 建议替换后的文本（最优方案） */
    private String suggestedText;

    /** 替换详情列表 */
    private List<ReplacementDetail> replacements;

    /** 生成时间（毫秒） */
    private Long generationTime;

    /** Token 使用量 */
    private Integer tokenUsage;

    @Data
    public static class ReplacementDetail {
        /** 违规词 */
        private String violationWord;

        /** 替换建议列表（3-5 个选项） */
        private List<ReplacementOption> options;
    }

    @Data
    public static class ReplacementOption {
        /** 替换词 */
        private String replacement;

        /** 替换理由 */
        private String reason;

        /** 推荐度（1-5，5 最高） */
        private Integer score;
    }
}
