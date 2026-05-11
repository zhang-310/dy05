package cn.gaifan.douyinOperations.common.util;

import java.util.regex.Pattern;

/**
 * Prompt 注入检测工具
 *
 * <p>用于检测和防御 AI Prompt 注入攻击，包括：
 * <ul>
 *   <li>指令覆盖攻击（ignore/disregard previous instructions）</li>
 *   <li>系统 Prompt 泄露（repeat your system prompt）</li>
 *   <li>分隔符注入（--- END OF / SYSTEM:）</li>
 * </ul>
 *
 * @see <a href="https://owasp.org/www-project-top-10-for-large-language-model-applications/">OWASP Top 10 for LLM</a>
 */
public class PromptInjectionDetector {

    /**
     * Prompt 注入检测模式
     */
    private static final Pattern[] INJECTION_PATTERNS = {
        // 指令覆盖：ignore/disregard/forget/override/bypass previous/above/prior/all instructions
        Pattern.compile("(?i)(ignore|disregard|forget|override|bypass)\\s+(previous|above|prior|all|your|the|system)\\s+(instructions?|prompts?|context|rules?)"),

        // 系统指令注入：SYSTEM: / <system>
        Pattern.compile("(?i)system\\s*:\\s*"),
        Pattern.compile("(?i)<\\s*system\\s*>"),

        // 分隔符注入：--- END OF
        Pattern.compile("(?i)---\\s*END\\s+OF"),

        // Prompt 泄露：repeat/show/tell me your system prompt/instructions
        Pattern.compile("(?i)(repeat|show|tell\\s+me|what\\s+(is|are))\\s+(your|the)\\s+(system\\s+)?(prompt|instructions?)"),

        // 角色覆盖：you are now / pretend you are
        Pattern.compile("(?i)(you\\s+are\\s+now|pretend\\s+you\\s+are|act\\s+as)\\s+"),

        // 限制绕过：without any restrictions / no limitations
        Pattern.compile("(?i)(without\\s+any|no)\\s+(restrictions?|limitations?|rules?|filters?)"),
    };

    /**
     * 最大输入长度（防止 DoS 攻击）
     */
    private static final int MAX_INPUT_LENGTH = 2000;

    /**
     * 检测输入是否包含疑似 Prompt 注入
     *
     * @param input 用户输入
     * @return true 如果检测到疑似注入，false 否则
     */
    public static boolean isSuspicious(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }

        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 清理用户输入（移除控制字符、限制长度）
     *
     * @param input 用户输入
     * @return 清理后的输入
     */
    public static String sanitize(String input) {
        if (input == null) {
            return "";
        }

        // 移除控制字符（保留换行符和制表符）
        String sanitized = input.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");

        // 限制长度（防止 DoS）
        if (sanitized.length() > MAX_INPUT_LENGTH) {
            sanitized = sanitized.substring(0, MAX_INPUT_LENGTH);
        }

        return sanitized;
    }

    /**
     * 检测并清理用户输入
     *
     * @param input 用户输入
     * @return 清理后的输入
     * @throws IllegalArgumentException 如果检测到疑似注入
     */
    public static String detectAndSanitize(String input) {
        String sanitized = sanitize(input);

        if (isSuspicious(sanitized)) {
            throw new IllegalArgumentException("输入包含不安全内容");
        }

        return sanitized;
    }

    private PromptInjectionDetector() {
        // 工具类，禁止实例化
    }
}
