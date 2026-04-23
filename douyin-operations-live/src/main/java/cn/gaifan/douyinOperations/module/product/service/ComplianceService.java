package cn.gaifan.douyinOperations.module.product.service;

/**
 * 产品话术内容合规检测服务
 * 检测绝对化用语、医疗功效宣称等，支持自动修复
 */
public interface ComplianceService {

    /**
     * 检测并尝试自动修复
     *
     * @param text 待检测文本
     * @return 检测结果，含是否通过、修复后文本、违规项
     */
    ComplianceResult checkAndFix(String text);

    record ComplianceResult(boolean passed, String fixedText, java.util.List<String> violations) {
        public static ComplianceResult pass(String text) {
            return new ComplianceResult(true, text, java.util.Collections.emptyList());
        }

        public static ComplianceResult fail(String text, java.util.List<String> violations) {
            return new ComplianceResult(false, text, violations);
        }
    }
}
