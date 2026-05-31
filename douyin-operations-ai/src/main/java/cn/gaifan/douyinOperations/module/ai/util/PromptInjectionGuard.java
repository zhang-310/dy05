package cn.gaifan.douyinOperations.module.ai.util;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * Prompt 注入防护工具类
 * 用于检测和清理用户输入中的 Prompt 注入攻击
 */
@Slf4j
public class PromptInjectionGuard {

    // 危险指令模式
    private static final Pattern[] DANGEROUS_PATTERNS = {
        Pattern.compile("(?i)ignore\\s+(previous|all|above|prior)\\s+(instructions?|prompts?|rules?|commands?)"),
        Pattern.compile("(?i)(forget|disregard|override)\\s+(everything|all|previous|above)"),
        Pattern.compile("(?i)you\\s+are\\s+(now|a)\\s+(different|new|another)"),
        Pattern.compile("(?i)system\\s*:\\s*"),
        Pattern.compile("(?i)assistant\\s*:\\s*"),
        Pattern.compile("(?i)repeat\\s+(your|the)\\s+(system\\s+)?prompt"),
        Pattern.compile("(?i)show\\s+(me\\s+)?(your|the)\\s+(system\\s+)?prompt"),
        Pattern.compile("(?i)---\\s*end\\s+of\\s+(user\\s+)?input\\s*---"),
        Pattern.compile("(?i)<\\|im_start\\|>"),
        Pattern.compile("(?i)<\\|im_end\\|>"),
    };

    // 最大输入长度
    private static final int MAX_INPUT_LENGTH = 10000;

    /**
     * 检测用户输入是否包含 Prompt 注入攻击
     * @param userInput 用户输入
     * @return true 如果检测到注入攻击
     */
    public static boolean detectInjection(String userInput) {
        if (userInput == null || userInput.isEmpty()) {
            return false;
        }

        // 检查长度
        if (userInput.length() > MAX_INPUT_LENGTH) {
            log.warn("用户输入超长: {} 字符", userInput.length());
            return true;
        }

        // 检查危险模式
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            if (pattern.matcher(userInput).find()) {
                log.warn("检测到 Prompt 注入攻击: pattern={}", pattern.pattern());
                return true;
            }
        }

        return false;
    }

    /**
     * 清理用户输入，移除潜在的注入攻击
     * @param userInput 用户输入
     * @return 清理后的输入
     */
    public static String sanitize(String userInput) {
        if (userInput == null) {
            return "";
        }

        // 截断超长输入
        if (userInput.length() > MAX_INPUT_LENGTH) {
            userInput = userInput.substring(0, MAX_INPUT_LENGTH);
        }

        // 移除特殊标记
        userInput = userInput.replaceAll("(?i)<\\|im_start\\|>", "");
        userInput = userInput.replaceAll("(?i)<\\|im_end\\|>", "");
        userInput = userInput.replaceAll("---\\s*end\\s+of\\s+(user\\s+)?input\\s*---", "");

        // 转义特殊字符（保留基本标点）
        // 不转义常见标点，只转义可能用于注入的字符
        return userInput;
    }

    /**
     * 构建安全的 Prompt（使用结构化格式隔离用户输入）
     * @param systemPrompt 系统 Prompt
     * @param userInput 用户输入
     * @return 安全的完整 Prompt
     */
    public static String buildSafePrompt(String systemPrompt, String userInput) {
        // 检测注入
        if (detectInjection(userInput)) {
            log.warn("拒绝包含注入攻击的输入");
            throw new IllegalArgumentException("输入包含不允许的内容");
        }

        // 清理输入
        String sanitizedInput = sanitize(userInput);

        // 使用 XML 标签隔离用户输入
        return systemPrompt + "\n\n<user_input>\n" + sanitizedInput + "\n</user_input>\n\n请基于上述用户输入回答问题。";
    }

    /**
     * 验证用户输入是否安全
     * @param userInput 用户输入
     * @throws IllegalArgumentException 如果检测到注入攻击
     */
    public static void validateInput(String userInput) {
        if (detectInjection(userInput)) {
            throw new IllegalArgumentException("输入包含不允许的内容");
        }
    }
}
