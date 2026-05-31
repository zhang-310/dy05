package cn.gaifan.douyinOperations.module.ai.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Prompt 注入检测器 — 三层防护（正则/模式/语义）
 * <p>
 * 检测维度：
 * 1. 正则层：ignore/disregard/forget + previous/instructions 组合，角色覆盖，标签注入
 * 2. 模式层：Base64/Unicode 编码绕过，零宽字符，同形字替换
 * 3. 语义层：关键词密度异常，输入长度异常
 */
public final class PromptInjectionDetector {

    private PromptInjectionDetector() {}

    public enum RiskLevel { LOW, MEDIUM, HIGH }

    public record DetectionResult(RiskLevel riskLevel, List<String> detectedPatterns) {
        public boolean isBlocked() { return riskLevel == RiskLevel.HIGH; }
    }

    // ─── 正则层：常见注入模式 ───
    private static final List<Pattern> HIGH_RISK_PATTERNS = List.of(
            // 英文：ignore/disregard/forget + previous/above/all instructions
            Pattern.compile("(?i)(ignore|disregard|forget|override|bypass)\\s+(all\\s+)?(previous|above|prior|earlier|system|initial)\\s+(instructions|prompts?|rules?|constraints?)"),
            // 中文变体
            Pattern.compile("(忽略|无视|放弃|覆盖|绕过|跳过)(所有|全部|之前的|上面的|系统的|初始的)?(指令|提示|规则|约束|限制|设定)"),
            // 角色覆盖尝试
            Pattern.compile("(?i)\"role\"\\s*:\\s*\"system\""),
            Pattern.compile("(?i)<\\s*/?(system|assistant)\\s*>"),
            // 直接角色声明
            Pattern.compile("(?i)(you\\s+are\\s+now|from\\s+now\\s+on|new\\s+instructions?:)"),
            Pattern.compile("(你现在是|从现在起|新的指令[：:])"),
            // 越狱关键词
            Pattern.compile("(?i)(DAN|jailbreak|do\\s+anything\\s+now)"),
            // 提示泄露
            Pattern.compile("(?i)(reveal|show|print|output|display)\\s+(your|the|system)\\s+(prompt|instructions|rules)")
    );

    private static final List<Pattern> MEDIUM_RISK_PATTERNS = List.of(
            // 单独的 ignore/forget（不跟 instructions 时风险较低）
            Pattern.compile("(?i)\\b(ignore|disregard)\\b.{0,20}\\b(above|below|text|message)\\b"),
            // Markdown/HTML 注入尝试
            Pattern.compile("(?i)<\\s*script\\b"),
            Pattern.compile("(?i)\\{\\{.*\\}\\}"),
            // 模拟对话格式
            Pattern.compile("(?i)(human|user|assistant)\\s*:\\s*.{20,}")
    );

    // ─── 模式层：编码绕过检测 ───
    private static final Pattern ZERO_WIDTH_CHARS = Pattern.compile("[\\u200B\\u200C\\u200D\\uFEFF\\u00AD\\u2060\\u180E]");
    private static final Pattern BASE64_BLOCK = Pattern.compile("[A-Za-z0-9+/]{40,}={0,2}");

    // ─── 语义层 ───
    private static final int MAX_KEYWORD_REPEAT = 5;
    private static final double LENGTH_ANOMALY_FACTOR = 1.5;

    /**
     * 检测输入文本中的注入风险
     * @param text 用户输入文本
     * @param expectedMaxLength 该字段预期最大长度（用于异常长度检测），<=0 则不检测
     */
    public static DetectionResult detect(String text, int expectedMaxLength) {
        if (text == null || text.isBlank()) {
            return new DetectionResult(RiskLevel.LOW, List.of());
        }

        List<String> detected = new ArrayList<>();
        RiskLevel maxLevel = RiskLevel.LOW;

        // ─── 第一层：正则匹配 ───
        for (Pattern p : HIGH_RISK_PATTERNS) {
            if (p.matcher(text).find()) {
                detected.add("HIGH_REGEX: " + p.pattern().substring(0, Math.min(60, p.pattern().length())));
                maxLevel = RiskLevel.HIGH;
            }
        }
        for (Pattern p : MEDIUM_RISK_PATTERNS) {
            if (p.matcher(text).find()) {
                detected.add("MEDIUM_REGEX: " + p.pattern().substring(0, Math.min(60, p.pattern().length())));
                if (maxLevel.ordinal() < RiskLevel.MEDIUM.ordinal()) {
                    maxLevel = RiskLevel.MEDIUM;
                }
            }
        }

        // ─── 第二层：编码绕过检测 ───
        if (ZERO_WIDTH_CHARS.matcher(text).find()) {
            detected.add("ENCODING: zero-width characters detected");
            if (maxLevel.ordinal() < RiskLevel.MEDIUM.ordinal()) {
                maxLevel = RiskLevel.MEDIUM;
            }
        }

        // Base64 编码块检测（解码后再检查是否含注入模式）
        var base64Matcher = BASE64_BLOCK.matcher(text);
        while (base64Matcher.find()) {
            try {
                String decoded = new String(Base64.getDecoder().decode(base64Matcher.group()), StandardCharsets.UTF_8);
                DetectionResult inner = detect(decoded, 0);
                if (inner.riskLevel.ordinal() >= RiskLevel.MEDIUM.ordinal()) {
                    detected.add("ENCODING: base64 encoded injection attempt");
                    maxLevel = RiskLevel.HIGH;
                    break;
                }
            } catch (IllegalArgumentException _ignored) {
                // 不是有效 base64，忽略
            }
        }

        // ─── 第三层：语义异常 ───
        // 关键词密度异常（同一词重复 > MAX_KEYWORD_REPEAT 次）
        String[] words = text.toLowerCase().split("\\s+");
        java.util.Map<String, Integer> freq = new java.util.HashMap<>();
        for (String w : words) {
            if (w.length() >= 3) {
                freq.merge(w, 1, Integer::sum);
            }
        }
        for (var entry : freq.entrySet()) {
            if (entry.getValue() > MAX_KEYWORD_REPEAT) {
                detected.add("SEMANTIC: keyword '" + entry.getKey() + "' repeated " + entry.getValue() + " times");
                if (maxLevel.ordinal() < RiskLevel.MEDIUM.ordinal()) {
                    maxLevel = RiskLevel.MEDIUM;
                }
                break;
            }
        }

        // 输入长度异常
        if (expectedMaxLength > 0 && text.length() > expectedMaxLength * LENGTH_ANOMALY_FACTOR) {
            detected.add("SEMANTIC: input length " + text.length() + " exceeds expected " + expectedMaxLength);
            if (maxLevel.ordinal() < RiskLevel.MEDIUM.ordinal()) {
                maxLevel = RiskLevel.MEDIUM;
            }
        }

        return new DetectionResult(maxLevel, detected);
    }

    /** 简化检测（不做长度校验） */
    public static DetectionResult detect(String text) {
        return detect(text, 0);
    }
}
