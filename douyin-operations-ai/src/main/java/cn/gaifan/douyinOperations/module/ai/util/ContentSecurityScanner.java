package cn.gaifan.douyinOperations.module.ai.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 内容安全扫描：隐私信息、违禁用语、异常编码。不阻断导入，仅返回警告供人工审核。
 */
public final class ContentSecurityScanner {

    private static final Logger log = LoggerFactory.getLogger(ContentSecurityScanner.class);

    private static final String[] SENSITIVE_PATTERNS = {
        "\\b1[3-9]\\d{9}\\b",
        "\\b\\d{6}(18|19|20)\\d{2}(0[1-9]|1[0-2]).*\\b",
        "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b"
    };

    private static final String[] PROHIBITED_CLAIMS = {
        "国家级", "世界级", "最高级", "最佳", "第一", "唯一",
        "治愈", "根治", "药到病除", "包治百病"
    };

    private ContentSecurityScanner() {
    }

    /**
     * 扫描内容，返回警告列表（不阻断）
     */
    public static List<SecurityWarning> scan(String content, String filename) {
        List<SecurityWarning> warnings = new ArrayList<>();
        if (content == null) content = "";

        int privacyCount = 0;
        for (String pattern : SENSITIVE_PATTERNS) {
            Matcher m = Pattern.compile(pattern).matcher(content);
            while (m.find()) privacyCount++;
        }
        if (privacyCount > 0) {
            warnings.add(new SecurityWarning("privacy",
                "检测到 " + privacyCount + " 处疑似个人隐私信息（手机号/身份证/邮箱）",
                "建议脱敏后再导入"));
        }

        List<String> found = new ArrayList<>();
        for (String word : PROHIBITED_CLAIMS) {
            if (content.contains(word)) found.add(word);
        }
        if (!found.isEmpty()) {
            warnings.add(new SecurityWarning("compliance",
                "检测到违禁广告用语: " + String.join("、", found),
                "入库后检索使用时需注意合规"));
        }

        int len = content.length();
        if (len > 0) {
            int nonPrintable = 0;
            for (char c : content.toCharArray()) {
                if (c < 0x20 && c != '\n' && c != '\r' && c != '\t') nonPrintable++;
            }
            if (nonPrintable > len * 0.05) {
                warnings.add(new SecurityWarning("encoding",
                    "内容中含大量不可打印字符（占比 " + (nonPrintable * 100 / len) + "%）",
                    "文件可能解析异常或含二进制内容"));
            }
        }

        if (!warnings.isEmpty() && filename != null) {
            log.warn("内容安全扫描发现 {} 个问题: {}", warnings.size(), filename);
        }
        return warnings;
    }

    public record SecurityWarning(String type, String message, String suggestion) {}

    /**
     * 获取内容风险级别（0=低 1=中 2=高）
     * @param content 待检测内容
     * @param context 上下文标识
     * @param strict 是否严格模式
     * @return 最高风险级别
     */
    public static int getRiskLevel(String content, String context, boolean strict) {
        List<SecurityWarning> warnings = scan(content, context);
        if (warnings.isEmpty()) return 0;
        boolean hasHigh = warnings.stream().anyMatch(w -> "privacy".equals(w.type()) || "security".equals(w.type()));
        if (hasHigh) return 2;
        boolean hasMedium = warnings.stream().anyMatch(w -> "compliance".equals(w.type()));
        return hasMedium ? 1 : 0;
    }
}
