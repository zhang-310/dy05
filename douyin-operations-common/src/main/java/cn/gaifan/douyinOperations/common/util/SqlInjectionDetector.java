package cn.gaifan.douyinOperations.common.util;

import java.util.regex.Pattern;

/**
 * SQL 注入检测工具
 */
public class SqlInjectionDetector {

    private static final Pattern SQL_INJECTION_PATTERN =
        Pattern.compile("(.*)(--|;|'|\\*|\\||\\(|\\)|\\[|\\]|\\{|\\}|\\^|%|~|`|\\\\|\\n|\\r)(.*)");

    /**
     * 检测输入是否包含可疑的 SQL 注入特征
     *
     * @param input 输入字符串
     * @return 是否可疑
     */
    public static boolean isSuspicious(String input) {
        if (input == null) {
            return false;
        }
        return SQL_INJECTION_PATTERN.matcher(input).matches();
    }

    /**
     * 清理输入字符串，移除可疑字符
     *
     * @param input 输入字符串
     * @return 清理后的字符串
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("[;'\"\\-\\*\\|\\(\\)\\[\\]\\{\\}\\^%~`\\\\]", "");
    }
}
