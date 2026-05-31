package cn.gaifan.douyinOperations.module.ai.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prompt 清洗器 — 对用户输入进行注入检测并按策略清洗或拒绝
 * <p>
 * 三层策略：
 * <ul>
 *   <li>HIGH → 拒绝请求（抛 PromptInjectionException）</li>
 *   <li>MEDIUM → 清洗后继续 + 记录告警日志</li>
 *   <li>LOW → 原样通过</li>
 * </ul>
 */
public final class PromptSanitizer {

    private static final Logger log = LoggerFactory.getLogger(PromptSanitizer.class);

    private PromptSanitizer() {}

    public record SanitizeResult(
            String sanitizedText,
            PromptInjectionDetector.RiskLevel riskLevel,
            java.util.List<String> detectedPatterns
    ) {
        public boolean isBlocked() { return riskLevel == PromptInjectionDetector.RiskLevel.HIGH; }
    }

    /**
     * 清洗用户输入
     * @param text 用户输入
     * @param fieldName 字段名（用于日志）
     * @param expectedMaxLength 预期最大长度，<=0 不检测
     * @return 清洗结果
     * @throws PromptInjectionException 当检测到 HIGH 风险时抛出
     */
    public static SanitizeResult sanitize(String text, String fieldName, int expectedMaxLength) {
        if (text == null || text.isBlank()) {
            return new SanitizeResult(text, PromptInjectionDetector.RiskLevel.LOW, java.util.List.of());
        }

        PromptInjectionDetector.DetectionResult detection = PromptInjectionDetector.detect(text, expectedMaxLength);

        return switch (detection.riskLevel()) {
            case HIGH -> {
                log.warn("[PromptSanitizer] 拒绝高风险输入: field={}, patterns={}", fieldName, detection.detectedPatterns());
                throw new PromptInjectionException(
                        "输入包含潜在注入内容，已被安全系统拦截",
                        detection.detectedPatterns()
                );
            }
            case MEDIUM -> {
                String cleaned = stripDangerousContent(text);
                log.warn("[PromptSanitizer] 清洗中等风险输入: field={}, patterns={}, original_len={}, cleaned_len={}",
                        fieldName, detection.detectedPatterns(), text.length(), cleaned.length());
                yield new SanitizeResult(cleaned, detection.riskLevel(), detection.detectedPatterns());
            }
            case LOW -> new SanitizeResult(text, detection.riskLevel(), detection.detectedPatterns());
        };
    }

    /** 简化版：无长度限制 */
    public static SanitizeResult sanitize(String text, String fieldName) {
        return sanitize(text, fieldName, 0);
    }

    /**
     * 清洗危险内容：移除零宽字符、HTML/伪标签、Markdown 注入
     */
    private static String stripDangerousContent(String text) {
        // 移除零宽字符
        String cleaned = text.replaceAll("[\\u200B\\u200C\\u200D\\uFEFF\\u00AD\\u2060\\u180E]", "");
        // 移除伪 system/assistant 标签
        cleaned = cleaned.replaceAll("(?i)<\\s*/?(system|assistant)\\s*>", "");
        // 移除 script 标签
        cleaned = cleaned.replaceAll("(?i)<\\s*script[^>]*>.*?</\\s*script\\s*>", "");
        cleaned = cleaned.replaceAll("(?i)<\\s*script\\b[^>]*>", "");
        // 移除模板注入 {{ }}
        cleaned = cleaned.replaceAll("\\{\\{.*?\\}\\}", "");
        return cleaned;
    }

    /**
     * Prompt 注入异常
     */
    public static class PromptInjectionException extends RuntimeException {
        private final java.util.List<String> detectedPatterns;

        public PromptInjectionException(String message, java.util.List<String> detectedPatterns) {
            super(message);
            this.detectedPatterns = detectedPatterns;
        }

        public java.util.List<String> getDetectedPatterns() {
            return detectedPatterns;
        }
    }
}
