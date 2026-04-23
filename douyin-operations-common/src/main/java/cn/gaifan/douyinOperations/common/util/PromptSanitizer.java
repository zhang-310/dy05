package cn.gaifan.douyinOperations.common.util;

import java.util.regex.Pattern;

/**
 * Prompt 注入防护：过滤系统指令尝试、限制长度
 */
public final class PromptSanitizer {

    private static final int MAX_USER_INPUT_LEN = 2000;
    private static final Pattern INJECTION = Pattern.compile(
            "(?i)(ignore|disregard|forget)\\s+(previous|above|prior|all)\\s+(instructions?|prompts?|context)",
            Pattern.CASE_INSENSITIVE
    );
    private static final String FILTERED = "[FILTERED]";

    private PromptSanitizer() {}

    public static String sanitize(String userInput) {
        if (userInput == null) return "";
        String s = userInput.trim();
        s = INJECTION.matcher(s).replaceAll(FILTERED);
        if (s.length() > MAX_USER_INPUT_LEN) {
            s = s.substring(0, MAX_USER_INPUT_LEN);
        }
        return s;
    }

    public static String sanitizeForSearch(String query) {
        if (query == null || query.isBlank()) return "";
        return sanitize(query);
    }
}
