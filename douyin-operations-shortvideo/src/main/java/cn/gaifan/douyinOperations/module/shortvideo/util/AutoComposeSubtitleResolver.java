package cn.gaifan.douyinOperations.module.shortvideo.util;

import cn.gaifan.douyinOperations.module.ai.service.VideoEditService;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * V-3：与 {@code ShortVideoEditController#autoCompose} 一致的字幕解析优先级：
 * {@code materials.subtitles}（优先）→ {@code srt} → {@code srtUrl}；受 {@code burnSubtitles} 控制。
 */
public final class AutoComposeSubtitleResolver {

    private static final int MAX_SRT_FETCH_BYTES = 512 * 1024;
    private static final Pattern SRT_TIME_LINE = Pattern.compile(
            "(\\d{1,2}:\\d{2}:\\d{2}[.,]\\d{3})\\s*-->\\s*(\\d{1,2}:\\d{2}:\\d{2}[.,]\\d{3})");

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private AutoComposeSubtitleResolver() {
    }

    public static boolean parseBooleanLoose(Object o, boolean defaultVal) {
        if (o instanceof Boolean b) {
            return b;
        }
        if (o instanceof String s && !s.isBlank()) {
            return !"false".equalsIgnoreCase(s.trim()) && !"0".equals(s.trim());
        }
        if (o instanceof Number n) {
            return n.intValue() != 0;
        }
        return defaultVal;
    }

    public static Double parseDoubleLoose(Object o) {
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        if (o instanceof String s && !s.isBlank()) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static double parseSrtTimestampToSeconds(String raw) {
        if (raw == null) {
            return 0;
        }
        String ts = raw.trim().replace('.', ',');
        int comma = ts.lastIndexOf(',');
        if (comma < 0) {
            return 0;
        }
        String frac = ts.substring(comma + 1);
        String hms = ts.substring(0, comma);
        String[] p = hms.split(":");
        if (p.length != 3) {
            return 0;
        }
        try {
            int h = Integer.parseInt(p[0].trim());
            int m = Integer.parseInt(p[1].trim());
            int s = Integer.parseInt(p[2].trim());
            int ms = Integer.parseInt(frac.trim());
            return h * 3600.0 + m * 60.0 + s + ms / 1000.0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 将 SubRip 正文解析为字幕条目（宽松：跳过无法解析的块）。
     */
    public static List<VideoEditService.SubtitleItem> parseSrtDocumentToItems(String srt) {
        List<VideoEditService.SubtitleItem> out = new ArrayList<>();
        if (!StringUtils.hasText(srt)) {
            return out;
        }
        String normalized = srt.replace("\r\n", "\n").replace('\r', '\n');
        String[] blocks = normalized.split("\n{2,}");
        for (String block : blocks) {
            String b = block.trim();
            if (b.isEmpty()) {
                continue;
            }
            String[] lines = b.split("\n");
            int timeIdx = -1;
            Matcher tm = null;
            for (int i = 0; i < lines.length; i++) {
                Matcher m = SRT_TIME_LINE.matcher(lines[i].trim());
                if (m.find()) {
                    timeIdx = i;
                    tm = m;
                    break;
                }
            }
            if (timeIdx < 0 || tm == null) {
                continue;
            }
            double start = parseSrtTimestampToSeconds(tm.group(1));
            double end = parseSrtTimestampToSeconds(tm.group(2));
            if (end <= start) {
                end = start + 0.5;
            }
            StringBuilder text = new StringBuilder();
            for (int i = timeIdx + 1; i < lines.length; i++) {
                if (text.length() > 0) {
                    text.append('\n');
                }
                text.append(lines[i]);
            }
            String t = text.toString().trim();
            if (t.isEmpty()) {
                continue;
            }
            out.add(new VideoEditService.SubtitleItem(start, end, t));
        }
        return out;
    }

    public static String fetchSrtFromUrl(String url) throws Exception {
        if (!StringUtils.hasText(url) || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            return null;
        }
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url.trim()))
                .timeout(Duration.ofSeconds(25))
                .header("Accept", "text/plain,*/*")
                .GET()
                .build();
        HttpResponse<byte[]> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            return null;
        }
        byte[] body = resp.body();
        if (body == null || body.length == 0) {
            return null;
        }
        int len = Math.min(body.length, MAX_SRT_FETCH_BYTES);
        return new String(body, 0, len, StandardCharsets.UTF_8);
    }

    /**
     * @param materials 与剪辑 API {@code materials} 同结构（可来自工作流 {@code params} 或其 {@code materials} 子对象）
     */
    @SuppressWarnings("unchecked")
    public static List<VideoEditService.SubtitleItem> resolveSubtitleItems(Map<String, Object> materials) {
        List<VideoEditService.SubtitleItem> subtitleItems = new ArrayList<>();
        if (materials == null) {
            return subtitleItems;
        }
        boolean burnSubtitles = parseBooleanLoose(materials.get("burnSubtitles"), true);
        if (!burnSubtitles) {
            return subtitleItems;
        }
        List<Map<String, Object>> subtitlesList = (List<Map<String, Object>>) materials.get("subtitles");
        if (subtitlesList != null) {
            for (Map<String, Object> st : subtitlesList) {
                Double start = st.get("startTime") instanceof Number n ? n.doubleValue() : parseDoubleLoose(st.get("startTime"));
                Double end = st.get("endTime") instanceof Number n ? n.doubleValue() : parseDoubleLoose(st.get("endTime"));
                Double duration = st.get("duration") instanceof Number n ? n.doubleValue() : parseDoubleLoose(st.get("duration"));
                if (end == null && start != null && duration != null) {
                    end = start + duration;
                }
                String text = st.get("text") instanceof String s ? s : null;
                if (start != null && end != null && text != null) {
                    subtitleItems.add(new VideoEditService.SubtitleItem(start, end, text));
                }
            }
        }
        if (!subtitleItems.isEmpty()) {
            return subtitleItems;
        }
        String srtBody = materials.get("srt") instanceof String s ? s : null;
        if (StringUtils.hasText(srtBody)) {
            return new ArrayList<>(parseSrtDocumentToItems(srtBody));
        }
        if (materials.get("srtUrl") instanceof String surl && StringUtils.hasText(surl)) {
            try {
                String fetched = fetchSrtFromUrl(surl.trim());
                if (StringUtils.hasText(fetched)) {
                    return new ArrayList<>(parseSrtDocumentToItems(fetched));
                }
            } catch (Exception ignored) {
                // 拉取失败则保持空，后续可走 scriptText 生成
            }
        }
        if (materials.get("timelineJson") instanceof String tl && StringUtils.hasText(tl)) {
            var fromTl = ProjectTimelineJsonCodec.extractSubtitleSegments(tl.trim());
            if (!fromTl.isEmpty()) {
                return new ArrayList<>(fromTl);
            }
        }
        return subtitleItems;
    }

    /**
     * 与控制器一致：仅当烧录开启且 body 提供 {@code scriptText} 时返回该字符串，否则 null。
     */
    public static String resolveExplicitScriptTextForBurn(Map<String, Object> materials) {
        if (materials == null) {
            return null;
        }
        boolean burnSubtitles = parseBooleanLoose(materials.get("burnSubtitles"), true);
        if (!burnSubtitles) {
            return null;
        }
        return materials.get("scriptText") instanceof String s && StringUtils.hasText(s) ? s : null;
    }
}
