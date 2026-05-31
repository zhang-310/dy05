package cn.gaifan.douyinOperations.module.agent.util;

import java.util.regex.Pattern;

/**
 * Prompt 注入检测工具
 * P0-3: 防御 Prompt 注入攻击
 */
public class PromptInjectionDetector {

    // 危险关键词模式（不区分大小写）
    private static final String[] DANGEROUS_KEYWORDS = {
        "忽略", "ignore", "forget", "disregard",
        "system", "prompt", "instruction", "指令",
        "新的指令", "new instruction", "override",
        "覆盖", "替换", "replace",
        "你现在是", "you are now", "act as",
        "扮演", "pretend", "假装"
    };

    // 危险分隔符模式
    private static final Pattern DANGEROUS_SEPARATORS = Pattern.compile(
        "(?i)(---|###|===|\\*\\*\\*|<system>|</system>|<prompt>|</prompt>)"
    );

    // 最大输入长度
    private static final int MAX_INPUT_LENGTH = 2000;

    /**
     * 检测输入是否包含 Prompt 注入攻击
     * @param input 用户输入
     * @return true 如果检测到注入攻击
     */
    public static boolean detectInjection(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }

        String lower = input.toLowerCase();

        // 1. 检测危险关键词
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        // 2. 检测危险分隔符
        if (DANGEROUS_SEPARATORS.matcher(input).find()) {
            return true;
        }

        // 3. 检测过长输入（可能包含大量注入内容）
        if (input.length() > MAX_INPUT_LENGTH) {
            return true;
        }

        return false;
    }

    /**
     * 清理用户输入，移除潜在的注入内容
     * @param input 用户输入
     * @return 清理后的输入
     */
    public static String sanitize(String input) {
        if (input == null) {
            return "";
        }

        String sanitized = input;

        // 1. 移除危险关键词（替换为空格）
        for (String keyword : DANGEROUS_KEYWORDS) {
            sanitized = sanitized.replaceAll("(?i)" + Pattern.quote(keyword), " ");
        }

        // 2. 移除危险分隔符
        sanitized = DANGEROUS_SEPARATORS.matcher(sanitized).replaceAll(" ");

        // 3. 限制长度
        if (sanitized.length() > MAX_INPUT_LENGTH) {
            sanitized = sanitized.substring(0, MAX_INPUT_LENGTH);
        }

        // 4. 清理多余空格
        sanitized = sanitized.replaceAll("\\s+", " ").trim();

        return sanitized;
    }

    /**
     * 包装用户输入为结构化格式，防止注入
     * @param input 用户输入
     * @return 包装后的输入
     */
    public static String wrapUserInput(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        // 使用明确的标记包装用户输入
        return "【用户问题】\n" + sanitize(input) + "\n【问题结束】\n\n请基于系统提示词回答上述问题。";
    }
}
