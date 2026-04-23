package cn.gaifan.douyinOperations.module.shortvideo.util;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.shortvideo.vo.ViralCollectVO;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从抖音「复制链接」整段文案中提取视频 URL；兼容单独粘贴 http(s) 直链（含视频号等）。
 */
public final class DouyinSharePasteParser {

    private DouyinSharePasteParser() {}

    private static final Pattern V_DOUYIN = Pattern.compile(
            "https?://v\\.douyin\\.com/[A-Za-z0-9\\-_]+/?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern WWW_DOUYIN = Pattern.compile(
            "https?://(www\\.)?douyin\\.com/(video|note)/[0-9]+/?(?:\\?[^\\s]*)?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern TITLE_BRACKET = Pattern.compile("【([^】]{1,300})】");

    private static final Pattern HTTP_START = Pattern.compile("https?://", Pattern.CASE_INSENSITIVE);

    private static final Set<String> COLLECT_TITLE_PLACEHOLDERS = Set.of("爆款分析");

    public static Optional<String> firstDouyinVideoUrl(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Optional.empty();
        }
        String text = raw.trim();
        int bestPos = Integer.MAX_VALUE;
        String best = null;
        Matcher m1 = V_DOUYIN.matcher(text);
        while (m1.find()) {
            int s = m1.start();
            if (s < bestPos) {
                bestPos = s;
                best = trimTrailingJunk(m1.group());
            }
        }
        Matcher m2 = WWW_DOUYIN.matcher(text);
        while (m2.find()) {
            int s = m2.start();
            if (s < bestPos) {
                bestPos = s;
                best = trimTrailingJunk(m2.group());
            }
        }
        return best == null ? Optional.empty() : Optional.of(best);
    }

    public static Optional<String> resolveCollectVideoUrl(String raw) {
        Optional<String> douyin = firstDouyinVideoUrl(raw);
        if (douyin.isPresent()) {
            return douyin;
        }
        if (!StringUtils.hasText(raw)) {
            return Optional.empty();
        }
        String t = raw.trim();
        boolean singleLine = t.indexOf('\n') < 0 && t.indexOf('\r') < 0;
        if (singleLine && (t.startsWith("http://") || t.startsWith("https://"))) {
            return Optional.of(trimTrailingJunk(t));
        }
        return Optional.empty();
    }

    public static Optional<String> titleFromBrackets(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Optional.empty();
        }
        Matcher m = TITLE_BRACKET.matcher(raw.trim());
        if (m.find()) {
            String s = m.group(1).trim();
            return s.isEmpty() ? Optional.empty() : Optional.of(s);
        }
        return Optional.empty();
    }

    public static Optional<String> primaryTitleFromDouyinSharePaste(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Optional.empty();
        }
        String text = raw.trim();
        Matcher bm = TITLE_BRACKET.matcher(text);
        if (!bm.find()) {
            return Optional.empty();
        }
        String inBracket = bm.group(1).trim();
        if (!inBracket.endsWith("的作品")) {
            return Optional.empty();
        }
        String rest = text.substring(bm.end()).trim();
        if (rest.isEmpty()) {
            return Optional.empty();
        }
        int hash = rest.indexOf('#');
        String segment = hash >= 0 ? rest.substring(0, hash) : rest;
        Matcher urlm = HTTP_START.matcher(segment);
        if (urlm.find()) {
            segment = segment.substring(0, urlm.start()).trim();
        }
        String t = segment.trim();
        t = t.replaceFirst("\\.{3,}\\s*$", "").trim();
        if (t.length() < 4) {
            return Optional.empty();
        }
        return Optional.of(t);
    }

    public static void applyToCollectVo(ViralCollectVO vo) {
        if (vo == null || !StringUtils.hasText(vo.getVideoUrl())) {
            return;
        }
        String raw = vo.getVideoUrl().trim();
        Optional<String> resolved = resolveCollectVideoUrl(raw);
        if (resolved.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL,
                    "未识别到有效视频链接：请粘贴抖音分享全文（含 https://v.douyin.com/… 或 www.douyin.com/video/…），"
                            + "或单独一行填写以 http 开头的直链");
        }
        vo.setVideoUrl(resolved.get());
        boolean titleMissing = !StringUtils.hasText(vo.getTitle());
        boolean placeholderTitle = !titleMissing && COLLECT_TITLE_PLACEHOLDERS.contains(vo.getTitle().trim());
        if (titleMissing || placeholderTitle) {
            primaryTitleFromDouyinSharePaste(raw)
                    .or(() -> titleFromBrackets(raw))
                    .ifPresent(vo::setTitle);
        }
    }

    static String trimTrailingJunk(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        String u = url.trim();
        while (!u.isEmpty()) {
            char c = u.charAt(u.length() - 1);
            if (c == ')' || c == ']' || c == '}' || c == '.' || c == ',' || c == '，' || c == '。'
                    || c == '、' || c == ';' || c == '；' || c == '"' || c == '\'' || c == '`' || c == '*') {
                u = u.substring(0, u.length() - 1);
            } else {
                break;
            }
        }
        return u;
    }
}
