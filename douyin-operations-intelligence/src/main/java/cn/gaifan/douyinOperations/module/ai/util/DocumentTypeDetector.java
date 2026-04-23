package cn.gaifan.douyinOperations.module.ai.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 文档类型自动检测：script（话术）、mixed（混合）、general（通用）。
 * 话术关键词可从 application.yml app.ai.kb.detect.script-keywords 配置。
 */
@Component
public class DocumentTypeDetector {

    private static final List<String> DEFAULT_SCRIPT_KEYWORDS = List.of(
            "姐妹们", "宝子们", "家人们", "下单", "秒杀", "限时", "抢购", "套盒", "套装", "宝贝", "链接", "库存", "倒计时", "上车",
            "穿搭", "面料", "尺码", "智能", "续航", "芯片", "软装", "收纳", "家居", "零食", "口感", "配料"
    );

    private final List<String> scriptKeywords;

    public DocumentTypeDetector(
            @Value("${app.ai.kb.detect.script-keywords:}") String configKeywords) {
        if (configKeywords != null && !configKeywords.isBlank()) {
            this.scriptKeywords = Arrays.stream(configKeywords.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        } else {
            this.scriptKeywords = DEFAULT_SCRIPT_KEYWORDS;
        }
    }

    /**
     * 自动检测文档类型：script / mixed / general
     */
    public String detect(String content) {
        if (content == null || content.isBlank()) return "general";

        int scriptSignals = 0;
        int totalSignals = 0;

        int numberedItems = countPattern(content, "(?m)^\\d+[.、）)]");
        int bracketTitles = countPattern(content, "(?m)^[【\\[#]");
        countPattern(content, "(?m)^[-=]{3,}");

        if (numberedItems >= 3 || bracketTitles >= 3) scriptSignals += 2;
        totalSignals += 2;

        String[] paragraphs = content.split("\\n\\s*\\n");
        int shortParas = 0;
        int longParas = 0;
        for (String p : paragraphs) {
            int len = p.trim().length();
            if (len >= 30 && len <= 500) shortParas++;
            if (len > 500) longParas++;
        }
        if (shortParas >= 3 && longParas == 0) scriptSignals += 2;
        else if (shortParas >= 3 && longParas > 0) scriptSignals += 1;
        totalSignals += 2;

        int keywordHits = 0;
        for (String kw : scriptKeywords) {
            if (content.contains(kw)) keywordHits++;
        }
        if (keywordHits >= 3) scriptSignals += 2;
        else if (keywordHits >= 1) scriptSignals += 1;
        totalSignals += 2;

        double ratio = (double) scriptSignals / totalSignals;
        if (ratio >= 0.7) return "script";
        if (ratio >= 0.4) return "mixed";
        return "general";
    }

    private static int countPattern(String text, String regex) {
        return (int) Pattern.compile(regex).matcher(text).results().count();
    }
}
