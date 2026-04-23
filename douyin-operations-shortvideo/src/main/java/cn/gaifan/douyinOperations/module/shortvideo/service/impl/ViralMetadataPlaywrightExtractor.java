package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 抖音视频页元数据：yt-dlp 失败或字段不全时，用 Playwright 打开页面解析内嵌 JSON / 正文中的互动数字。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.viral-analysis.metadata-playwright-fallback-enabled", havingValue = "true", matchIfMissing = true)
public class ViralMetadataPlaywrightExtractor {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile("/video/(\\d{15,25})");

    private static final String CHROME_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36";

    private static final Set<String> STAT_KEYS = Set.of(
            "digg_count", "comment_count", "share_count", "collect_count",
            "play_count", "download_count", "forward_count");

    @Value("${app.viral-analysis.metadata-playwright-timeout-ms:45000}")
    private int timeoutMs;

    @Value("${app.video-analysis.yt-dlp-cookies-file:}")
    private String cookiesFile;

    private final boolean playwrightAvailable;

    public ViralMetadataPlaywrightExtractor() {
        boolean ok;
        try {
            Class.forName("com.microsoft.playwright.Playwright");
            ok = true;
        } catch (ClassNotFoundException e) {
            ok = false;
        }
        this.playwrightAvailable = ok;
        if (ok) {
            log.info("ViralMetadataPlaywrightExtractor 已就绪（Playwright classpath 可用）");
        } else {
            log.warn("ViralMetadataPlaywrightExtractor：Playwright 不在 classpath，兜底不可用");
        }
    }

    public boolean isAvailable() {
        return playwrightAvailable;
    }

    /**
     * 打开抖音视频页，解析内嵌 JSON 与正则兜底，返回可合并的元数据（rawJson 带 source=playwright）。
     */
    public ViralVideoMetadata extract(String videoUrl) {
        if (!playwrightAvailable || !StringUtils.hasText(videoUrl)) {
            return null;
        }
        String lower = videoUrl.toLowerCase();
        if (!lower.contains("douyin")) {
            log.debug("[元数据 Playwright] 非抖音 URL，跳过: {}", videoUrl);
            return null;
        }
        String canonical = buildCanonicalVideoUrl(videoUrl);
        if (!StringUtils.hasText(canonical)) {
            log.warn("[元数据 Playwright] 无法解析为可打开的视频 URL: {}", videoUrl);
            return null;
        }

        try (Playwright playwright = Playwright.create()) {
            try (Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
                try (BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                        .setUserAgent(CHROME_UA)
                        .setLocale("zh-CN"))) {
                    injectCookies(context);
                    Page page = context.newPage();
                    page.navigate(canonical, new Page.NavigateOptions().setTimeout(timeoutMs));
                    Thread.sleep(4000);

                    String html = page.content();
                    String embedded = extractEmbeddedJson(page);
                    ViralVideoMetadata fromJson = parseEmbeddedStats(embedded);
                    if (fromJson != null) {
                        return fromJson;
                    }
                    return parseStatsFromHtmlRegex(html);
                }
            }
        } catch (Exception e) {
            log.warn("[元数据 Playwright] 提取失败: {}", e.getMessage());
            return null;
        }
    }

    private String extractEmbeddedJson(Page page) {
        try {
            Object r = page.evaluate("() => {\n"
                    + "  const pick = (sel) => { const el = document.querySelector(sel); return el ? el.textContent : null; };\n"
                    + "  let t = pick('script#RENDER_DATA');\n"
                    + "  if (!t) t = pick('script#__NEXT_DATA__');\n"
                    + "  if (!t) return null;\n"
                    + "  try { return decodeURIComponent(t.trim()); } catch (e) { return t.trim(); }\n"
                    + "}");
            return r != null ? r.toString() : null;
        } catch (Exception e) {
            log.debug("[元数据 Playwright] 读取内嵌 script 失败: {}", e.getMessage());
            return null;
        }
    }

    private ViralVideoMetadata parseEmbeddedStats(String embeddedJson) {
        if (!StringUtils.hasText(embeddedJson)) {
            return null;
        }
        JsonNode root;
        try {
            String text = embeddedJson.trim();
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start < 0 || end <= start) {
                return null;
            }
            root = JSON.readTree(text.substring(start, end + 1));
        } catch (Exception e) {
            log.debug("[元数据 Playwright] 内嵌 JSON 解析失败: {}", e.getMessage());
            return null;
        }

        Map<String, Long> stats = new HashMap<>();
        collectStatNodes(root, stats);
        if (stats.isEmpty()) {
            return null;
        }

        Long digg = stats.get("digg_count");
        Long play = stats.get("play_count");
        if (play == null) {
            play = stats.get("download_count");
        }
        Long comment = stats.get("comment_count");
        Long share = stats.get("share_count");
        if (share == null) {
            share = stats.get("forward_count");
        }
        Long collect = stats.get("collect_count");

        String summary = "{\"source\":\"playwright\",\"stats\":" + JSON.valueToTree(stats).toString() + "}";
        return new ViralVideoMetadata(
                play, digg, collect, comment, share,
                null, null, null, null, null,
                null, List.of(), null, summary);
    }

    private void collectStatNodes(JsonNode node, Map<String, Long> out) {
        if (node == null || out.size() >= 8) {
            return;
        }
        if (node.isObject()) {
            var it = node.fields();
            while (it.hasNext()) {
                var e = it.next();
                String k = e.getKey();
                JsonNode v = e.getValue();
                if (STAT_KEYS.contains(k) && v != null && v.isNumber()) {
                    out.putIfAbsent(k, v.asLong());
                }
                collectStatNodes(v, out);
            }
        } else if (node.isArray()) {
            for (JsonNode c : node) {
                collectStatNodes(c, out);
            }
        }
    }

    private ViralVideoMetadata parseStatsFromHtmlRegex(String html) {
        if (!StringUtils.hasText(html)) {
            return null;
        }
        Map<String, Long> stats = new HashMap<>();
        applyPattern(html, "\"digg_count\"\\s*:\\s*(\\d+)", "digg_count", stats);
        applyPattern(html, "\"comment_count\"\\s*:\\s*(\\d+)", "comment_count", stats);
        applyPattern(html, "\"share_count\"\\s*:\\s*(\\d+)", "share_count", stats);
        applyPattern(html, "\"collect_count\"\\s*:\\s*(\\d+)", "collect_count", stats);
        applyPattern(html, "\"play_count\"\\s*:\\s*(\\d+)", "play_count", stats);
        applyPattern(html, "\"download_count\"\\s*:\\s*(\\d+)", "download_count", stats);

        if (stats.isEmpty()) {
            return null;
        }
        String summary = "{\"source\":\"playwright_regex\",\"stats\":" + JSON.valueToTree(stats).toString() + "}";
        return new ViralVideoMetadata(
                stats.get("play_count"),
                stats.get("digg_count"),
                stats.get("collect_count"),
                stats.get("comment_count"),
                stats.get("share_count"),
                null, null, null, null, null,
                null, List.of(), null, summary);
    }

    private static void applyPattern(String html, String regex, String key, Map<String, Long> stats) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(html);
        if (m.find()) {
            stats.putIfAbsent(key, Long.parseLong(m.group(1)));
        }
    }

    private void injectCookies(BrowserContext context) {
        if (!StringUtils.hasText(cookiesFile)) {
            return;
        }
        Path cookiePath = Path.of(cookiesFile.trim());
        if (!Files.isRegularFile(cookiePath)) {
            log.debug("[元数据 Playwright] cookies 文件不存在: {}", cookiePath);
            return;
        }
        try {
            List<String> lines = Files.readAllLines(cookiePath);
            List<Cookie> cookies = new ArrayList<>();
            for (String line : lines) {
                if (line.startsWith("#") || line.isBlank()) {
                    continue;
                }
                String[] parts = line.split("\t");
                if (parts.length < 7) {
                    continue;
                }
                String domain = parts[0];
                if (!domain.contains("douyin")) {
                    continue;
                }
                Cookie cookie = new Cookie(parts[5], parts[6]);
                cookie.setDomain(domain);
                cookie.setPath(parts[2]);
                cookie.setSecure("TRUE".equalsIgnoreCase(parts[3]));
                try {
                    long expires = Long.parseLong(parts[4]);
                    if (expires > 0) {
                        cookie.setExpires(expires);
                    }
                } catch (NumberFormatException ignored) {
                }
                cookies.add(cookie);
            }
            if (!cookies.isEmpty()) {
                context.addCookies(cookies);
                log.info("[元数据 Playwright] 注入 {} 条 Cookie", cookies.size());
            }
        } catch (Exception e) {
            log.warn("[元数据 Playwright] 读取 cookies 失败: {}", e.getMessage());
        }
    }

    private String buildCanonicalVideoUrl(String originalUrl) {
        String vid = extractVideoId(originalUrl);
        if (vid != null) {
            return "https://www.douyin.com/video/" + vid;
        }
        if (originalUrl.contains("www.douyin.com/video/")) {
            return originalUrl.split("\\?")[0];
        }
        return originalUrl;
    }

    private String extractVideoId(String url) {
        if (url == null) {
            return null;
        }
        Matcher m = VIDEO_ID_PATTERN.matcher(url);
        if (m.find()) {
            return m.group(1);
        }
        if (url.contains("v.douyin.com") || url.contains("iesdouyin.com")) {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X)")
                        .method("HEAD", HttpRequest.BodyPublishers.noBody())
                        .timeout(Duration.ofSeconds(15))
                        .build();
                HttpResponse<Void> resp = client.send(req, HttpResponse.BodyHandlers.discarding());
                Matcher m2 = VIDEO_ID_PATTERN.matcher(resp.uri().toString());
                if (m2.find()) {
                    return m2.group(1);
                }
            } catch (Exception e) {
                log.debug("[元数据 Playwright] 短链解析失败: {}", e.getMessage());
            }
        }
        return null;
    }
}
