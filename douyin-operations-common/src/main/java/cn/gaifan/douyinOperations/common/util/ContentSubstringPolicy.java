package cn.gaifan.douyinOperations.common.util;

import java.util.List;
import java.util.Locale;

/**
 * 可配置子串策略：用于直播主题、爆款自动编排等「命中即拒绝/跳过」场景。
 */
public final class ContentSubstringPolicy {

    private ContentSubstringPolicy() {
    }

    public static boolean anyMatch(String haystack, List<String> needles) {
        if (haystack == null || haystack.isBlank() || needles == null || needles.isEmpty()) {
            return false;
        }
        String h = haystack.toLowerCase(Locale.ROOT);
        for (String n : needles) {
            if (n == null) {
                continue;
            }
            String t = n.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (h.contains(t.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public static String firstHit(String haystack, List<String> needles) {
        if (haystack == null || haystack.isBlank() || needles == null || needles.isEmpty()) {
            return null;
        }
        String h = haystack.toLowerCase(Locale.ROOT);
        for (String n : needles) {
            if (n == null) {
                continue;
            }
            String t = n.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (h.contains(t.toLowerCase(Locale.ROOT))) {
                return t;
            }
        }
        return null;
    }
}
