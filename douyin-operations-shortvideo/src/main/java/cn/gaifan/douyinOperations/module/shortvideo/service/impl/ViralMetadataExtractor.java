package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 爆款视频元数据提取器。
 * <p>
 * 双源策略：
 * <ol>
 *   <li>yt-dlp --dump-json --no-download（快速、结构化）</li>
 *   <li>抖音链接：失败或互动字段缺失较多时，Playwright 打开页面解析内嵌 JSON / 正则兜底</li>
 * </ol>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.viral-analysis.metadata-extraction-enabled", havingValue = "true", matchIfMissing = true)
public class ViralMetadataExtractor {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Value("${app.video-analysis.yt-dlp-path:yt-dlp}")
    private String ytDlpPath;

    @Value("${app.video-analysis.yt-dlp-cookies-file:}")
    private String ytDlpCookiesFile;

    @Value("${app.video-analysis.yt-dlp-cookies-from-browser:}")
    private String ytDlpCookiesFromBrowser;

    @Value("${app.video-analysis.yt-dlp-force-direct-for-douyin:false}")
    private boolean ytDlpForceDirectForDouyin;

    @Value("${app.video-analysis.yt-dlp-user-agent:}")
    private String ytDlpUserAgent;

    @Value("${app.video-analysis.yt-dlp-impersonate-douyin:}")
    private String ytDlpImpersonateDouyin;

    /** 至少缺失几个互动字段（view/like/comment/favorite/share）才触发 Playwright；yt-dlp 完全失败时无视此阈值 */
    @Value("${app.viral-analysis.metadata-playwright-min-missing-stats:2}")
    private int metadataPlaywrightMinMissingStats;

    private static final String DEFAULT_DOUYIN_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36";

    @Autowired(required = false)
    private ViralMetadataPlaywrightExtractor metadataPlaywrightExtractor;

    /**
     * 从视频 URL 提取元数据。
     *
     * @param videoUrl 视频页面 URL
     * @return 元数据，提取失败返回 null
     */
    public ViralVideoMetadata extract(String videoUrl) {
        if (!StringUtils.hasText(videoUrl)) {
            return null;
        }
        ViralVideoMetadata ytdlp = null;
        try {
            ytdlp = extractViaYtDlp(videoUrl);
        } catch (Exception e) {
            log.warn("[元数据提取] yt-dlp --dump-json 失败: {}", e.getMessage());
        }

        if (!shouldTryPlaywright(ytdlp, videoUrl)) {
            return ytdlp;
        }
        try {
            ViralVideoMetadata pw = metadataPlaywrightExtractor.extract(videoUrl);
            ViralVideoMetadata merged = merge(ytdlp, pw);
            if (merged != null && pw != null) {
                log.info("[元数据提取] 已合并 Playwright 兜底（抖音） url={}", videoUrl);
            }
            return merged;
        } catch (Exception e) {
            log.warn("[元数据提取] Playwright 兜底异常: {}", e.getMessage());
            return ytdlp;
        }
    }

    private boolean shouldTryPlaywright(ViralVideoMetadata m, String url) {
        if (metadataPlaywrightExtractor == null || !metadataPlaywrightExtractor.isAvailable()) {
            return false;
        }
        if (!url.toLowerCase().contains("douyin")) {
            return false;
        }
        if (m == null) {
            return true;
        }
        return countNullInteractionFields(m) >= metadataPlaywrightMinMissingStats;
    }

    private static int countNullInteractionFields(ViralVideoMetadata m) {
        int n = 0;
        if (m.viewCount() == null) {
            n++;
        }
        if (m.likeCount() == null) {
            n++;
        }
        if (m.commentCount() == null) {
            n++;
        }
        if (m.favoriteCount() == null) {
            n++;
        }
        if (m.shareCount() == null) {
            n++;
        }
        return n;
    }

    private static ViralVideoMetadata merge(ViralVideoMetadata a, ViralVideoMetadata b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        List<String> tags;
        if (a.hashtags() != null && !a.hashtags().isEmpty()) {
            tags = a.hashtags();
        } else if (b.hashtags() != null && !b.hashtags().isEmpty()) {
            tags = b.hashtags();
        } else {
            tags = List.of();
        }
        String raw = mergeRaw(a.rawJson(), b.rawJson());
        return new ViralVideoMetadata(
                mergeLong(a.viewCount(), b.viewCount()),
                mergeLong(a.likeCount(), b.likeCount()),
                mergeLong(a.favoriteCount(), b.favoriteCount()),
                mergeLong(a.commentCount(), b.commentCount()),
                mergeLong(a.shareCount(), b.shareCount()),
                a.videoDuration() != null && a.videoDuration() > 0 ? a.videoDuration() : b.videoDuration(),
                firstNonBlank(a.description(), b.description()),
                firstNonBlank(a.authorId(), b.authorId()),
                firstNonBlank(a.authorName(), b.authorName()),
                a.authorFollowers() != null && a.authorFollowers() > 0 ? a.authorFollowers() : b.authorFollowers(),
                firstNonBlank(a.musicName(), b.musicName()),
                tags,
                firstNonBlank(a.coverUrl(), b.coverUrl()),
                raw
        );
    }

    private static Long mergeLong(Long a, Long b) {
        if (a != null && a > 0) {
            return a;
        }
        if (b != null && b > 0) {
            return b;
        }
        if (a != null) {
            return a;
        }
        return b;
    }

    private static String firstNonBlank(String a, String b) {
        if (StringUtils.hasText(a)) {
            return a;
        }
        return StringUtils.hasText(b) ? b : null;
    }

    private static String mergeRaw(String a, String b) {
        if (!StringUtils.hasText(a)) {
            return b;
        }
        if (!StringUtils.hasText(b)) {
            return a;
        }
        return a + "\n---playwright_metadata---\n" + b;
    }

    private ViralVideoMetadata extractViaYtDlp(String videoUrl) throws Exception {
        List<String> cmd = buildDumpJsonCommand(videoUrl);
        log.info("[元数据提取] 执行: {} (url={})", ytDlpPath, videoUrl);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(false);

        Process process = pb.start();

        String stdout;
        try (InputStream is = process.getInputStream()) {
            stdout = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        String stderr;
        try (InputStream es = process.getErrorStream()) {
            stderr = new String(es.readAllBytes(), StandardCharsets.UTF_8);
        }

        boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("yt-dlp --dump-json 超时 60s");
        }

        if (process.exitValue() != 0) {
            String errSnippet = stderr.length() > 500 ? stderr.substring(stderr.length() - 500) : stderr;
            throw new RuntimeException("yt-dlp exit=" + process.exitValue() + ": " + errSnippet);
        }

        if (!StringUtils.hasText(stdout) || !stdout.trim().startsWith("{")) {
            throw new RuntimeException("yt-dlp 输出为空或非 JSON");
        }

        return parseYtDlpJson(stdout.trim());
    }

    private List<String> buildDumpJsonCommand(String videoUrl) {
        List<String> cmd = new ArrayList<>();
        cmd.add(ytDlpPath);
        cmd.add("--dump-json");
        cmd.add("--no-download");

        boolean douyin = videoUrl.toLowerCase().contains("douyin");

        if (StringUtils.hasText(ytDlpCookiesFile)) {
            cmd.add("--cookies");
            cmd.add(ytDlpCookiesFile.trim());
        } else if (StringUtils.hasText(ytDlpCookiesFromBrowser)) {
            cmd.add("--cookies-from-browser");
            cmd.add(ytDlpCookiesFromBrowser.trim());
        }

        if (douyin && ytDlpForceDirectForDouyin) {
            cmd.add("--proxy");
            cmd.add("");
        }

        if (douyin) {
            cmd.add("--add-header");
            cmd.add("Referer:https://www.douyin.com/");
            cmd.add("--add-header");
            cmd.add("Accept-Language:zh-CN,zh;q=0.9,en;q=0.8");
        }

        String ua = StringUtils.hasText(ytDlpUserAgent) ? ytDlpUserAgent.trim() : (douyin ? DEFAULT_DOUYIN_UA : null);
        if (ua != null) {
            cmd.add("--user-agent");
            cmd.add(ua);
        }

        if (douyin && StringUtils.hasText(ytDlpImpersonateDouyin)) {
            cmd.add("--impersonate");
            cmd.add(ytDlpImpersonateDouyin.trim());
        }

        cmd.add("--no-playlist");
        cmd.add(videoUrl);
        return cmd;
    }

    private ViralVideoMetadata parseYtDlpJson(String json) throws Exception {
        JsonNode root = JSON.readTree(json);

        Long viewCount = longOrNull(root, "view_count");
        Long likeCount = longOrNull(root, "like_count");
        Long favoriteCount = longOrNull(root, "favorite_count");
        Long commentCount = longOrNull(root, "comment_count");
        Long shareCount = longOrNull(root, "repost_count");
        if (shareCount == null) {
            shareCount = longOrNull(root, "share_count");
        }

        Integer duration = root.has("duration") && !root.get("duration").isNull()
                ? root.get("duration").asInt(0) : null;

        String description = textOrNull(root, "description");
        String authorId = textOrNull(root, "uploader_id");
        String authorName = textOrNull(root, "uploader");
        if (authorName == null) {
            authorName = textOrNull(root, "channel");
        }
        Long authorFollowers = longOrNull(root, "channel_follower_count");

        String musicName = textOrNull(root, "track");
        if (musicName == null) {
            musicName = textOrNull(root, "artist");
        }

        List<String> hashtags = new ArrayList<>();
        JsonNode tags = root.get("tags");
        if (tags != null && tags.isArray()) {
            for (JsonNode t : tags) {
                if (t.isTextual() && !t.asText().isBlank()) {
                    hashtags.add(t.asText());
                }
            }
        }

        String coverUrl = textOrNull(root, "thumbnail");

        return new ViralVideoMetadata(
                viewCount, likeCount, favoriteCount, commentCount, shareCount,
                duration, description, authorId, authorName, authorFollowers,
                musicName, hashtags, coverUrl, json
        );
    }

    private static Long longOrNull(JsonNode root, String field) {
        JsonNode n = root.get(field);
        if (n == null || n.isNull()) {
            return null;
        }
        return n.asLong(0);
    }

    private static String textOrNull(JsonNode root, String field) {
        JsonNode n = root.get(field);
        if (n == null || n.isNull()) {
            return null;
        }
        String t = n.asText();
        return t.isBlank() ? null : t;
    }
}
