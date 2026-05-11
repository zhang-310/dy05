package cn.gaifan.douyinOperations.common.compliance.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 违规检测结果 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceCheckResult {

    /**
     * 检测结果: pass=通过, warning=警告, reject=拒绝
     */
    private String result;

    /**
     * 风险评分（0-100，越高越危险）
     */
    private BigDecimal riskScore;

    /**
     * 匹配的违规规则列表
     */
    private List<MatchedRule> matchedRules;

    /**
     * 修改建议
     */
    private String suggestions;

    /**
     * 检测耗时（毫秒）
     */
    private Integer checkDurationMs;

    /**
     * 匹配的规则详情
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchedRule {
        /**
         * 规则编码
         */
        private String ruleCode;

        /**
         * 规则名称
         */
        private String ruleName;

        /**
         * 严重程度
         */
        private String severity;

        /**
         * 匹配原因
         */
        private String matchReason;

        /**
         * 处罚措施
         */
        private String punishment;
    }
}
