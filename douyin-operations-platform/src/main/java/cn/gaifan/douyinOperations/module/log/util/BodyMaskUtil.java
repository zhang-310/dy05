package cn.gaifan.douyinOperations.module.log.util;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * 操作日志请求/响应体脱敏与截断。
 * 用于 request_body、response_body 写入前：隐藏密码、token 等敏感字段值，并限制长度。
 */
public final class BodyMaskUtil {

    private static final int MAX_LENGTH = 2000;
    private static final String MASK = "***";

    /** JSON 风格键值对： "key" : "value" 或 "key":"value"，
     * 将 value 替换为 ***（不区分大小写匹配键名） */
    private static final Pattern[] SENSITIVE_PATTERNS = new Pattern[] {
        Pattern.compile("(\"(?:password|passwd|pwd)\"\\s*:\\s*)\"[^\"]*\"", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\"(?:token|access_token|accessToken|refresh_token|refreshToken)\"\\s*:\\s*)\"[^\"]*\"", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\"(?:api_key|apiKey|apikey)\"\\s*:\\s*)\"[^\"]*\"", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\"(?:secret|api_secret|apiSecret)\"\\s*:\\s*)\"[^\"]*\"", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\"(?:authorization|Authorization)\"\\s*:\\s*)\"[^\"]*\"", Pattern.CASE_INSENSITIVE),
    };

    private BodyMaskUtil() {}

    /**
     * 对 body 字符串做脱敏并截断到 maxLength（默认 2000）。
     * 非文本或空返回 null；脱敏后超过长度则截断并追加 "..."。
     */
    public static String maskAndTruncate(byte[] content, String contentType, int maxLength) {
        if (content == null || content.length == 0) return null;
        if (maxLength <= 0) maxLength = MAX_LENGTH;
        if (!isTextContent(contentType)) return null;
        String s = new String(content, StandardCharsets.UTF_8);
        return maskAndTruncate(s, maxLength);
    }

    public static String maskAndTruncate(String raw, int maxLength) {
        if (raw == null) return null;
        if (maxLength <= 0) maxLength = MAX_LENGTH;
        String masked = raw;
        for (Pattern p : SENSITIVE_PATTERNS) {
            masked = p.matcher(masked).replaceAll("$1\"" + MASK + "\"");
        }
        if (masked.length() <= maxLength) return masked;
        return masked.substring(0, maxLength - 3) + "...";
    }

    private static boolean isTextContent(String contentType) {
        if (contentType == null) return true;
        String lower = contentType.toLowerCase();
        return lower.contains("json") || lower.contains("text/") || lower.contains("xml")
                || lower.contains("application/x-www-form-urlencoded");
    }
}
