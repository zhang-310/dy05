package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.benchmark.service.DouyinCookieService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 用 Playwright 抓取抖音账号主页的全部视频列表。
 * 作为 Open API getVideoList 的兜底通道。
 */
@Slf4j
@Component
public class AccountVideoScraper {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String CHROME_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36";
    /** 排除 /user/self（登录态下「我的主页」占位，不是具体账号 sec_uid） */
    private static final Pattern SEC_UID_PATTERN = Pattern.compile("/user/([\\w-]+)");

    @Value("${app.video-analysis.yt-dlp-cookies-file:}")
    private String cookiesFile;

    @Value("${app.account-collect.playwright-timeout-ms:60000}")
    private int timeoutMs;

    @Value("${app.account-collect.browser-executable-path:}")
    private String browserExecutablePath;

    @Value("${app.account-collect.max-scroll-rounds:80}")
    private int maxScrollRounds;

    /** 至少滚动这么多轮后，才允许因「连续无新视频」而停止（避免首屏未加载完就停） */
    @Value("${app.account-collect.scroll-min-rounds:10}")
    private int scrollMinRounds;

    /** 连续多少轮 DOM 解析不到任何新 videoId 才认为到底（抖音懒加载可能中间停顿多轮） */
    @Value("${app.account-collect.scroll-idle-streak-limit:6}")
    private int scrollIdleStreakLimit;

    /** 每轮滚动后的等待（毫秒），给 XHR 与虚拟列表渲染时间 */
    @Value("${app.account-collect.scroll-pause-ms:2600}")
    private int scrollPauseMs;

    /** 账号主页 SPA 渲染与接口拉取作品列表的额外等待（毫秒） */
    @Value("${app.account-collect.user-page-settle-ms:5000}")
    private int userPageSettleMs;

    @Value("${app.shortvideo.account-collect.worker.cookie-id:}")
    private Long workerCookieId;

    private final boolean playwrightAvailable;

    @Autowired(required = false)
    private DouyinCookieService douyinCookieService;

    public AccountVideoScraper() {
        boolean ok;
        try {
            Class.forName("com.microsoft.playwright.Playwright");
            ok = true;
        } catch (ClassNotFoundException e) {
            ok = false;
        }
        this.playwrightAvailable = ok;
    }

    public boolean isAvailable() {
        return playwrightAvailable;
    }

    private Browser.NewContextOptions newBrowserContextOptions() {
        return new Browser.NewContextOptions()
                .setUserAgent(CHROME_UA)
                .setLocale("zh-CN")
                .setViewportSize(1365, 768);
    }

    private Browser launchBrowser(Playwright playwright) {
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(List.of("--no-sandbox", "--disable-dev-shm-usage"));
        if (StringUtils.hasText(browserExecutablePath)) {
            options.setExecutablePath(Path.of(browserExecutablePath.trim()));
        }
        return playwright.chromium().launch(options);
    }

    /** 抖音页多为 SPA，默认 waitUntil=load 易长时间不触发导致 navigate 超时 */
    private void navigateDouyin(Page page, String url) {
        page.navigate(url, new Page.NavigateOptions()
                .setTimeout(timeoutMs)
                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
    }

    /**
     * 抖音号转主页：优先「精选」搜索页（与浏览器一致 {@code /jingxuan/search/...?type=general}），再试旧版 {@code /search?type=user}。
     */
    private static List<String> userSearchCandidateUrls(String douyinId) {
        String enc = URLEncoder.encode(douyinId.trim(), StandardCharsets.UTF_8);
        List<String> list = new ArrayList<>(4);
        list.add("https://www.douyin.com/jingxuan/search/" + enc + "?type=general");
        list.add("https://www.douyin.com/jingxuan/search/" + enc + "?type=user");
        list.add("https://www.douyin.com/search/" + enc + "?type=user");
        return list;
    }

    /** 从当前搜索页 DOM 取第一个非 self 的用户主页链接 */
    private static final String EXTRACT_USER_PROFILE_FROM_SEARCH_PAGE = ""
            + "() => {"
            + "function norm(href) {"
            + "  if (!href) return null;"
            + "  let h = (href.split('?')[0] || '').trim();"
            + "  if (h.startsWith('//')) h = 'https:' + h;"
            + "  if (!h.includes('/user/')) return null;"
            + "  if (h.includes('/user/self')) return null;"
            + "  return h.startsWith('http') ? h : ('https://www.douyin.com' + h);"
            + "}"
            + "const roots = ["
            + "  document.querySelector('[data-e2e=\"search-user-list\"]'),"
            + "  document.querySelector('[data-e2e=\"user-list\"]'),"
            + "  document.querySelector('[data-e2e=\"search-result\"]'),"
            + "  document.querySelector('div[class*=\"search-result\"]'),"
            + "  document.querySelector('#root'),"
            + "  document.querySelector('main')"
            + "].filter(Boolean);"
            + "for (const root of roots) {"
            + "  const as = root.querySelectorAll('a[href*=\"/user/\"]');"
            + "  for (const a of as) {"
            + "    const u = norm(a.getAttribute('href'));"
            + "    if (u) return u;"
            + "  }"
            + "}"
            + "const all = document.querySelectorAll('a[href*=\"/user/\"]');"
            + "for (const a of all) {"
            + "  const u = norm(a.getAttribute('href'));"
            + "  if (u) return u;"
            + "}"
            + "return null;"
            + "}";

    @Data
    public static class ScrapedVideo {
        private String videoUrl;
        private String videoId;
        private String title;
        private String coverUrl;
        /** 播放量（来自页面 XHR/JSON statistics.play_count） */
        private Long viewCount;
        /** 点赞 digg_count */
        private Long likeCount;
        private Long commentCount;
        private Long shareCount;
        /** 视频被收藏数 collect_count */
        private Long favoriteCount;
    }

    /** 从接口 JSON 的 statistics 节点合并出的互动数据 */
    private static final class VideoStatsAgg {
        Long play;
        Long like;
        Long comment;
        Long share;
        Long collect;

        void absorbStatistics(JsonNode st) {
            if (st == null || !st.isObject()) {
                return;
            }
            play = maxLong(play, readStat(st, "play_count"));
            like = maxLong(like, readStat(st, "digg_count"));
            comment = maxLong(comment, readStat(st, "comment_count"));
            share = maxLong(share, readStat(st, "share_count"));
            collect = maxLong(collect, readStat(st, "collect_count"));
        }

        void merge(VideoStatsAgg o) {
            if (o == null) {
                return;
            }
            play = maxLong(play, o.play);
            like = maxLong(like, o.like);
            comment = maxLong(comment, o.comment);
            share = maxLong(share, o.share);
            collect = maxLong(collect, o.collect);
        }

        private static Long readStat(JsonNode st, String key) {
            if (!st.has(key) || !st.get(key).isNumber()) {
                return null;
            }
            return st.get(key).asLong();
        }

        private static Long maxLong(Long a, Long b) {
            if (a == null) {
                return b;
            }
            if (b == null) {
                return a;
            }
            return Math.max(a, b);
        }
    }

    private static final class CookieInjectionResult {
        private final int cookieCount;
        private final Long dbCookieId;

        private CookieInjectionResult(int cookieCount, Long dbCookieId) {
            this.cookieCount = cookieCount;
            this.dbCookieId = dbCookieId;
        }

        private boolean hasCookies() {
            return cookieCount > 0;
        }
    }

    private static void applyAggToVideo(ScrapedVideo v, VideoStatsAgg a) {
        if (v == null || a == null) {
            return;
        }
        if (a.play != null) {
            v.setViewCount(a.play);
        }
        if (a.like != null) {
            v.setLikeCount(a.like);
        }
        if (a.comment != null) {
            v.setCommentCount(a.comment);
        }
        if (a.share != null) {
            v.setShareCount(a.share);
        }
        if (a.collect != null) {
            v.setFavoriteCount(a.collect);
        }
    }

    private static void enrichVideosFromStatsMap(List<ScrapedVideo> videos, Map<String, VideoStatsAgg> statsByVideoId) {
        if (videos == null || statsByVideoId == null || statsByVideoId.isEmpty()) {
            return;
        }
        int hit = 0;
        for (ScrapedVideo v : videos) {
            if (v.getVideoId() == null) {
                continue;
            }
            VideoStatsAgg a = statsByVideoId.get(v.getVideoId());
            if (a != null) {
                applyAggToVideo(v, a);
                hit++;
            }
        }
        if (hit > 0) {
            log.info("已从页面接口 JSON 为 {}/{} 条视频合并互动数据（播放/赞/评/转/藏）", hit, videos.size());
        }
    }

    /** 多容器滚到底，尽量触发虚拟列表与分页请求 */
    private void scrollFeedDown(Page page) {
        try {
            page.evaluate("() => {\n"
                    + "  const toEnd = (el) => {\n"
                    + "    try {\n"
                    + "      if (el && el.scrollHeight > el.clientHeight) { el.scrollTop = el.scrollHeight; }\n"
                    + "    } catch (e) {}\n"
                    + "  };\n"
                    + "  toEnd(document.scrollingElement);\n"
                    + "  toEnd(document.documentElement);\n"
                    + "  toEnd(document.body);\n"
                    + "  document.querySelectorAll("
                    + "'#douyin-right-container, main, [data-e2e=\"user-post-list\"]').forEach(toEnd);\n"
                    + "  const h = Math.max(\n"
                    + "    document.body ? document.body.scrollHeight : 0,\n"
                    + "    document.documentElement ? document.documentElement.scrollHeight : 0);\n"
                    + "  window.scrollTo(0, h);\n"
                    + "}");
        } catch (Exception e) {
            log.debug("scrollFeedDown: {}", e.getMessage());
        }
    }

    private void ingestAwemeJsonResponseBody(String body, Map<String, VideoStatsAgg> sink) {
        if (!StringUtils.hasText(body) || body.length() > 4_000_000) {
            return;
        }
        if (!body.contains("aweme_id") || !body.contains("statistics")) {
            return;
        }
        JsonNode root;
        try {
            root = JSON.readTree(body);
        } catch (Exception e) {
            return;
        }
        Map<String, VideoStatsAgg> batch = new HashMap<>();
        collectAwemeStatisticsNodes(root, batch, 0);
        for (Map.Entry<String, VideoStatsAgg> e : batch.entrySet()) {
            sink.merge(e.getKey(), e.getValue(), (a, b) -> {
                a.merge(b);
                return a;
            });
        }
    }

    /**
     * 递归查找同时含 aweme_id（或 item_id）与 statistics 的节点，合并互动数。
     */
    private static void collectAwemeStatisticsNodes(JsonNode node, Map<String, VideoStatsAgg> out, int depth) {
        if (node == null || depth > 80) {
            return;
        }
        if (node.isObject()) {
            JsonNode aid = node.get("aweme_id");
            if (aid == null) {
                aid = node.get("item_id");
            }
            String id = null;
            if (aid != null && aid.isTextual()) {
                id = aid.asText();
            } else if (aid != null && aid.isIntegralNumber()) {
                id = String.valueOf(aid.asLong());
            }
            JsonNode st = node.get("statistics");
            if (id != null && st != null && st.isObject()) {
                out.computeIfAbsent(id, k -> new VideoStatsAgg()).absorbStatistics(st);
            }
            Iterator<String> it = node.fieldNames();
            while (it.hasNext()) {
                collectAwemeStatisticsNodes(node.get(it.next()), out, depth + 1);
            }
        } else if (node.isArray()) {
            for (JsonNode c : node) {
                collectAwemeStatisticsNodes(c, out, depth + 1);
            }
        }
    }

    private void attachDouyinAwemeStatsListener(Page page, Map<String, VideoStatsAgg> statsSink) {
        page.onResponse(response -> {
            try {
                if (response.status() != 200) {
                    return;
                }
                String url = response.url();
                if (!url.contains("douyin")) {
                    return;
                }
                String ct = response.headerValue("content-type");
                String body = response.text();
                boolean jsonCt = ct != null && ct.toLowerCase(Locale.ROOT).contains("json");
                boolean looseAweme = body.length() < 2_000_000 && body.contains("\"aweme_id\"")
                        && body.contains("\"statistics\"")
                        && (body.trim().startsWith("{") || body.trim().startsWith("["));
                if (!jsonCt && !looseAweme) {
                    return;
                }
                ingestAwemeJsonResponseBody(body, statsSink);
            } catch (Exception ex) {
                log.trace("采集响应解析跳过: {}", ex.getMessage());
            }
        });
    }

    @Data
    public static class ScrapeResult {
        private String accountName;
        private String secUid;
        private List<ScrapedVideo> videos = new ArrayList<>();
    }

    /**
     * 从抖音账号主页提取 sec_uid
     */
    public String extractSecUid(String accountUrl) {
        if (!StringUtils.hasText(accountUrl)) return null;
        Matcher m = SEC_UID_PATTERN.matcher(accountUrl);
        if (m.find()) {
            String id = m.group(1);
            if (isReservedUserSegment(id)) {
                return null;
            }
            return id;
        }

        if (accountUrl.contains("v.douyin.com") || accountUrl.contains("iesdouyin.com")) {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(accountUrl))
                        .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X)")
                        .method("HEAD", HttpRequest.BodyPublishers.noBody())
                        .timeout(Duration.ofSeconds(15))
                        .build();
                HttpResponse<Void> resp = client.send(req, HttpResponse.BodyHandlers.discarding());
                Matcher m2 = SEC_UID_PATTERN.matcher(resp.uri().toString());
                if (m2.find()) {
                    String id = m2.group(1);
                    if (!isReservedUserSegment(id)) {
                        return id;
                    }
                }
            } catch (Exception e) {
                log.debug("短链解析 sec_uid 失败: {}", e.getMessage());
            }
        }
        return null;
    }

    private static boolean isReservedUserSegment(String segment) {
        return segment != null && "self".equalsIgnoreCase(segment.trim());
    }

    /** 浏览器复制的「我的主页」为 /user/self，不能用于采集指定抖音号作品 */
    private static boolean isSelfProfileUrl(String url) {
        return url != null && url.contains("/user/self");
    }

    /**
     * 打开视频页，提取作者主页 URL
     */
    public String extractAuthorUrlFromVideoPage(String videoUrl) {
        return extractAuthorUrlFromVideoPage(videoUrl, null);
    }

    public String extractAuthorUrlFromVideoPage(String videoUrl, Long cookieOwnerId) {
        if (!playwrightAvailable || !StringUtils.hasText(videoUrl)) return null;
        try (Playwright playwright = Playwright.create()) {
            try (Browser browser = launchBrowser(playwright)) {
                try (BrowserContext context = browser.newContext(newBrowserContextOptions())) {
                    injectCookies(context, cookieOwnerId);
                    Page page = context.newPage();
                    navigateDouyin(page, videoUrl);
                    Thread.sleep(3000);
                    Object result = page.evaluate("() => {"
                            + "const a = document.querySelector('a[href*=\"/user/\"][data-e2e], "
                            + "  a[href*=\"/user/\"].author, "
                            + "  div[data-e2e=\"video-player-container\"] a[href*=\"/user/\"], "
                            + "  a[href*=\"/user/\"]');"
                            + "if (a) {"
                            + "  const href = a.getAttribute('href') || '';"
                            + "  if (href.includes('/user/')) {"
                            + "    return href.startsWith('http') ? href : 'https://www.douyin.com' + href;"
                            + "  }"
                            + "}"
                            + "return null;"
                            + "}");
                    if (result != null) {
                        String url = result.toString().split("\\?")[0];
                        log.info("从视频页提取到作者主页: {}", url);
                        return url;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("从视频页提取作者失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 打开视频页，提取作者昵称
     */
    public String extractAuthorNameFromVideoPage(String videoUrl) {
        return extractAuthorNameFromVideoPage(videoUrl, null);
    }

    public String extractAuthorNameFromVideoPage(String videoUrl, Long cookieOwnerId) {
        if (!playwrightAvailable || !StringUtils.hasText(videoUrl)) return null;
        try (Playwright playwright = Playwright.create()) {
            try (Browser browser = launchBrowser(playwright)) {
                try (BrowserContext context = browser.newContext(newBrowserContextOptions())) {
                    injectCookies(context, cookieOwnerId);
                    Page page = context.newPage();
                    navigateDouyin(page, videoUrl);
                    Thread.sleep(3000);
                    Object result = page.evaluate("() => {"
                            + "const el = document.querySelector('[data-e2e=\"video-author-name\"], "
                            + "  span.author-name, "
                            + "  a[href*=\"/user/\"] span');"
                            + "return el ? el.textContent.trim() : null;"
                            + "}");
                    return result != null ? result.toString() : null;
                }
            }
        } catch (Exception e) {
            log.debug("从视频页提取作者昵称失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 搜索抖音号，返回第一个匹配的用户主页 URL
     */
    public String searchDouyinAccount(String douyinId) {
        return searchDouyinAccount(douyinId, null);
    }

    public String searchDouyinAccount(String douyinId, Long cookieOwnerId) {
        if (!playwrightAvailable || !StringUtils.hasText(douyinId)) return null;
        List<String> candidates = userSearchCandidateUrls(douyinId);
        try (Playwright playwright = Playwright.create()) {
            try (Browser browser = launchBrowser(playwright)) {
                try (BrowserContext context = browser.newContext(newBrowserContextOptions())) {
                    injectCookies(context, cookieOwnerId);
                    Page page = context.newPage();
                    for (String searchUrl : candidates) {
                        try {
                            log.debug("抖音号搜索尝试: {}", searchUrl);
                            navigateDouyin(page, searchUrl);
                            Thread.sleep(5200);
                            try {
                                page.waitForLoadState(LoadState.NETWORKIDLE,
                                        new Page.WaitForLoadStateOptions().setTimeout(12_000));
                            } catch (Exception ignored) {
                            }
                            Object result = page.evaluate(EXTRACT_USER_PROFILE_FROM_SEARCH_PAGE);
                            if (result == null) {
                                continue;
                            }
                            String url = result.toString().split("\\?")[0];
                            if (isSelfProfileUrl(url)) {
                                log.warn("搜索抖音号 [{}] 在 {} 仍仅命中 /user/self，尝试下一套 URL", douyinId, searchUrl);
                                continue;
                            }
                            log.info("搜索抖音号 [{}] 找到主页: {} (searchUrl={})", douyinId, url, searchUrl);
                            return url;
                        } catch (Exception one) {
                            log.debug("单次搜索 URL 失败: {} — {}", searchUrl, one.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("搜索抖音号失败: {}", e.getMessage());
        }
        log.info("搜索抖音号 [{}] 未找到结果（已尝试 jingxuan 与旧版 search）", douyinId);
        return null;
    }

    /**
     * 抓取抖音「视频」搜索结果页（关键词搜索）下的视频卡片列表。
     * searchUrl 须为完整 URL，例如 https://www.douyin.com/search/xxx?type=video
     */
    public ScrapeResult scrapeSearchVideos(String searchUrl) {
        return scrapeSearchVideos(searchUrl, null);
    }

    public ScrapeResult scrapeSearchVideos(String searchUrl, Long cookieOwnerId) {
        if (!playwrightAvailable || !StringUtils.hasText(searchUrl)) {
            log.warn("Playwright 不可用或搜索 URL 为空，无法抓取搜索结果");
            return null;
        }
        ScrapeResult result = new ScrapeResult();
        result.setAccountName("视频搜索");
        Long usedDbCookieId = null;
        try (Playwright playwright = Playwright.create()) {
            try (Browser browser = launchBrowser(playwright)) {
                try (BrowserContext context = browser.newContext(newBrowserContextOptions())) {
                    CookieInjectionResult cookieInjection = injectCookies(context, cookieOwnerId);
                    usedDbCookieId = cookieInjection.dbCookieId;
                    if (!cookieInjection.hasCookies()) {
                        throw new IllegalStateException("未找到有效抖音 Cookie，请在 Cookie 管理中扫码登录，或为采集 worker 配置有效 COLLECTOR_COOKIE_ID");
                    }
                    Map<String, VideoStatsAgg> statsSink = new ConcurrentHashMap<>();
                    Page page = context.newPage();
                    attachDouyinAwemeStatsListener(page, statsSink);
                    navigateDouyin(page, searchUrl);
                    Thread.sleep(4000);
                    result.setVideos(scrollAndCollectSearchVideos(page, statsSink));
                    if (result.getVideos().isEmpty()) {
                        String blockedReason = logSearchEmptyDiagnostics(page, searchUrl);
                        if (StringUtils.hasText(blockedReason)) {
                            throw new IllegalStateException(blockedReason);
                        }
                    }
                }
            }
        } catch (Exception e) {
            markCookieUnavailableIfBlocked(cookieOwnerId, usedDbCookieId, e.getMessage());
            log.error("抓取搜索视频列表失败: {}", e.getMessage(), e);
            throw new IllegalStateException("抓取搜索视频列表失败: " + e.getMessage(), e);
        }
        log.info("搜索页视频抓取完成，共 {} 条", result.getVideos().size());
        return result;
    }

    private String logSearchEmptyDiagnostics(Page page, String searchUrl) {
        try {
            Object raw = page.evaluate("() => {"
                    + "const text = (document.body && document.body.innerText || '').replace(/\\s+/g, ' ').trim();"
                    + "return {"
                    + "  title: document.title || '',"
                    + "  url: location.href || '',"
                    + "  videoAnchors: document.querySelectorAll('a[href*=\"/video/\"]').length,"
                    + "  userAnchors: document.querySelectorAll('a[href*=\"/user/\"]').length,"
                    + "  images: document.querySelectorAll('img').length,"
                    + "  mainText: text.substring(0, 500)"
                    + "};"
                    + "}");
            if (raw instanceof Map<?, ?> map) {
                String title = str(map, "title");
                String url = str(map, "url");
                String mainText = str(map, "mainText");
                String videoAnchors = str(map, "videoAnchors");
                String userAnchors = str(map, "userAnchors");
                String images = str(map, "images");
                log.warn("搜索页未解析到视频: targetUrl={}, currentUrl={}, title={}, videoAnchors={}, userAnchors={}, images={}, text={}",
                        searchUrl, url, title, videoAnchors, userAnchors, images, mainText);
                String combined = ((title == null ? "" : title) + " " + (url == null ? "" : url)
                        + " " + (mainText == null ? "" : mainText)).toLowerCase(Locale.ROOT);
                if (containsSecurityVerificationMarker(combined)) {
                    return "抖音搜索页疑似进入登录/安全验证/风控页，请更新有效 Cookie 或降低采集频率";
                }
            }
        } catch (Exception e) {
            log.warn("搜索页空结果诊断失败: {}", e.getMessage());
        }
        return null;
    }

    private static boolean containsAny(String text, String... needles) {
        if (text == null) {
            return false;
        }
        for (String needle : needles) {
            if (needle != null && text.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsSecurityVerificationMarker(String text) {
        return containsAny(text, "验证码", "安全验证", "请完成验证", "扫码登录", "请登录",
                "登录后", "访问频繁", "captcha", "verify", "verification", "robot");
    }

    private void markCookieUnavailableIfBlocked(Long cookieOwnerId, Long dbCookieId, String message) {
        if (cookieOwnerId == null || dbCookieId == null || douyinCookieService == null
                || !containsSecurityVerificationMarker(message)) {
            return;
        }
        try {
            douyinCookieService.markUnavailable(dbCookieId, cookieOwnerId, "douyin",
                    "采集时进入登录/安全验证/风控页: " + message);
        } catch (Exception markEx) {
            log.warn("标记抖音 Cookie 不可用失败: cookieId={}, err={}", dbCookieId, markEx.getMessage());
        }
    }

    private List<ScrapedVideo> scrollAndCollectSearchVideos(Page page, Map<String, VideoStatsAgg> statsSink) {
        Set<String> seenIds = new HashSet<>();
        List<ScrapedVideo> videos = new ArrayList<>();
        int idleStreak = 0;
        for (int round = 0; round < maxScrollRounds; round++) {
            List<ScrapedVideo> batch = extractVideoCardsFromSearch(page);
            int newCount = 0;
            for (ScrapedVideo v : batch) {
                if (v.getVideoId() != null && seenIds.add(v.getVideoId())) {
                    videos.add(v);
                    newCount++;
                }
            }
            if (newCount == 0) {
                idleStreak++;
            } else {
                idleStreak = 0;
            }
            if (round >= scrollMinRounds && idleStreak >= scrollIdleStreakLimit) {
                log.debug("搜索页滚动停止：已连续 {} 轮无新视频，已滚 {} 轮", idleStreak, round + 1);
                break;
            }
            scrollFeedDown(page);
            try {
                Thread.sleep(scrollPauseMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        enrichVideosFromStatsMap(videos, statsSink);
        return videos;
    }

    private List<ScrapedVideo> extractVideoCardsFromSearch(Page page) {
        List<ScrapedVideo> result = new ArrayList<>();
        try {
            Object raw = page.evaluate("() => {"
                    + "const as = document.querySelectorAll('a[href*=\"/video/\"]');"
                    + "const out = [];"
                    + "const seen = new Set();"
                    + "for (const a of as) {"
                    + "  const href = a.getAttribute('href') || '';"
                    + "  const m = href.match(/\\/video\\/(\\d{15,25})/);"
                    + "  if (!m || seen.has(m[1])) continue;"
                    + "  seen.add(m[1]);"
                    + "  const box = a.closest('li') || a.parentElement;"
                    + "  const img = box ? box.querySelector('img') : a.querySelector('img');"
                    + "  const title = (a.getAttribute('title') || a.textContent || '').trim();"
                    + "  out.push({"
                    + "    videoId: m[1],"
                    + "    href: 'https://www.douyin.com/video/' + m[1],"
                    + "    cover: img ? img.src : '',"
                    + "    title: title"
                    + "  });"
                    + "}"
                    + "return out;"
                    + "}");
            if (raw instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        ScrapedVideo v = new ScrapedVideo();
                        v.setVideoId(str(map, "videoId"));
                        v.setVideoUrl(str(map, "href"));
                        v.setCoverUrl(str(map, "cover"));
                        v.setTitle(str(map, "title"));
                        result.add(v);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("extractVideoCardsFromSearch 失败: {}", e.getMessage());
        }
        return result;
    }

    /**
     * 抓取账号主页全部视频列表
     */
    public ScrapeResult scrapeAccountVideos(String accountUrl) {
        return scrapeAccountVideos(accountUrl, null);
    }

    public ScrapeResult scrapeAccountVideos(String accountUrl, Long cookieOwnerId) {
        if (!playwrightAvailable) {
            log.warn("Playwright 不可用，无法抓取账号视频列表");
            return null;
        }
        if (isSelfProfileUrl(accountUrl)) {
            log.warn("账号链接为 /user/self（当前登录用户占位页），无法用于采集他人作品；请使用抖音号、他人主页链接或带 MS4wLjABAAAA… 的 sec_uid 地址");
            ScrapeResult bad = new ScrapeResult();
            bad.setSecUid(null);
            return bad;
        }

        String secUid = extractSecUid(accountUrl);
        String pageUrl = StringUtils.hasText(secUid)
                ? "https://www.douyin.com/user/" + secUid
                : accountUrl;

        ScrapeResult result = new ScrapeResult();
        result.setSecUid(secUid);

        Long usedDbCookieId = null;
        try (Playwright playwright = Playwright.create()) {
            try (Browser browser = launchBrowser(playwright)) {
                try (BrowserContext context = browser.newContext(newBrowserContextOptions())) {
                    CookieInjectionResult cookieInjection = injectCookies(context, cookieOwnerId);
                    usedDbCookieId = cookieInjection.dbCookieId;
                    if (!cookieInjection.hasCookies()) {
                        throw new IllegalStateException("未找到有效抖音 Cookie，请在 Cookie 管理中扫码登录，或为采集 worker 配置有效 COLLECTOR_COOKIE_ID");
                    }
                    Map<String, VideoStatsAgg> statsSink = new ConcurrentHashMap<>();
                    Page page = context.newPage();
                    attachDouyinAwemeStatsListener(page, statsSink);
                    navigateDouyin(page, pageUrl);
                    try {
                        page.waitForLoadState(LoadState.DOMCONTENTLOADED,
                                new Page.WaitForLoadStateOptions().setTimeout(Math.min(timeoutMs, 60_000)));
                    } catch (Exception ignored) {
                    }
                    page.waitForTimeout(Math.max(1200, userPageSettleMs));
                    tryActivateUserWorksTab(page);
                    try {
                        page.locator("a[href*='/video/']").first()
                                .waitFor(new Locator.WaitForOptions().setTimeout(20_000));
                    } catch (Exception e) {
                        log.debug("等待视频链接出现超时或未出现: {}", e.getMessage());
                    }
                    result.setAccountName(extractAccountName(page));
                    result.setVideos(scrollAndCollectVideos(page, statsSink));
                    if (result.getVideos().isEmpty()) {
                        String blockedReason = logScrapeEmptyDiagnostics(page, pageUrl);
                        if (StringUtils.hasText(blockedReason)) {
                            throw new IllegalStateException(blockedReason);
                        }
                    }
                }
            }
        } catch (Exception e) {
            markCookieUnavailableIfBlocked(cookieOwnerId, usedDbCookieId, e.getMessage());
            log.error("抓取账号视频列表失败: {}", e.getMessage(), e);
            throw new IllegalStateException("抓取账号视频列表失败: " + e.getMessage(), e);
        }
        return result;
    }

    private String extractAccountName(Page page) {
        try {
            Object name = page.evaluate("() => {"
                    + "const el = document.querySelector('span.J76h2pSC, h1, [data-e2e=\"user-info\"] span');"
                    + "return el ? el.textContent.trim() : null;"
                    + "}");
            return name != null ? name.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private List<ScrapedVideo> scrollAndCollectVideos(Page page, Map<String, VideoStatsAgg> statsSink) {
        Set<String> seenIds = new HashSet<>();
        List<ScrapedVideo> videos = new ArrayList<>();
        int idleStreak = 0;

        for (int round = 0; round < maxScrollRounds; round++) {
            List<ScrapedVideo> batch = extractVideoCards(page);
            int newCount = 0;
            for (ScrapedVideo v : batch) {
                if (v.getVideoId() != null && seenIds.add(v.getVideoId())) {
                    videos.add(v);
                    newCount++;
                }
            }
            if (newCount == 0) {
                idleStreak++;
            } else {
                idleStreak = 0;
            }
            // 避免「单轮懒加载未返回新节点」就停；需满足最少轮数 + 连续多轮无增量
            if (round >= scrollMinRounds && idleStreak >= scrollIdleStreakLimit) {
                log.debug("账号主页滚动停止：已连续 {} 轮无新视频，已滚 {} 轮", idleStreak, round + 1);
                break;
            }
            scrollFeedDown(page);
            try {
                Thread.sleep(scrollPauseMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        enrichVideosFromStatsMap(videos, statsSink);
        log.info("账号视频抓取完成，共 {} 条", videos.size());
        return videos;
    }

    /** 个人主页常见 Tab：确保在「作品」而非「喜欢」等 */
    private static void tryActivateUserWorksTab(Page page) {
        try {
            String[] labels = {"作品", "视频"};
            for (String label : labels) {
                Locator t = page.getByText(label, new Page.GetByTextOptions().setExact(true));
                int n = t.count();
                for (int i = 0; i < n; i++) {
                    Locator el = t.nth(i);
                    if (el.isVisible()) {
                        el.click(new Locator.ClickOptions().setTimeout(2000).setForce(true));
                        page.waitForTimeout(1200);
                        log.debug("已尝试点击「{}」Tab", label);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("切换作品 Tab 跳过: {}", e.getMessage());
        }
    }

    private String logScrapeEmptyDiagnostics(Page page, String pageUrl) {
        try {
            String title = page.title();
            Object vcount = page.evaluate("() => document.querySelectorAll('a[href*=\"/video/\"]').length");
            Object hasPost = page.evaluate("() => !!document.querySelector('[data-e2e=\"user-post-list\"]')");
            Object text = page.evaluate("() => (document.body && document.body.innerText || '').replace(/\\s+/g, ' ').trim().substring(0, 500)");
            log.warn("账号主页未解析到视频卡片: url={}, currentUrl={}, title={}, user-post-list={}, a[video]={}, text={}",
                    pageUrl, page.url(), title, hasPost, vcount, text);
            String combined = ((title == null ? "" : title) + " " + page.url()
                    + " " + (text == null ? "" : text.toString())).toLowerCase(Locale.ROOT);
            if (containsSecurityVerificationMarker(combined)) {
                return "抖音账号页疑似进入登录/安全验证/风控页，请更新有效 Cookie 或降低采集频率";
            }
        } catch (Exception e) {
            log.warn("账号主页采集为空，且诊断失败: {}", e.getMessage());
        }
        return null;
    }

    private List<ScrapedVideo> extractVideoCards(Page page) {
        List<ScrapedVideo> result = new ArrayList<>();
        try {
            Object raw = page.evaluate("() => {"
                    + "function collectFrom(root) {"
                    + "  const out = [];"
                    + "  const seen = new Set();"
                    + "  const nodes = root.querySelectorAll('a[href*=\"/video/\"]');"
                    + "  for (const a of nodes) {"
                    + "    const href = (a.getAttribute('href') || '').split('?')[0];"
                    + "    const m = href.match(/\\/video\\/(\\d{15,25})/);"
                    + "    if (!m || seen.has(m[1])) continue;"
                    + "    seen.add(m[1]);"
                    + "    const box = a.closest('li') || a.closest('[data-e2e]') || a.parentElement;"
                    + "    const img = box ? box.querySelector('img') : a.querySelector('img');"
                    + "    let title = (a.getAttribute('title') || '').trim();"
                    + "    if (!title && box) {"
                    + "      const te = box.querySelector('p, span[class*=\"title\"], [class*=\"title\"]');"
                    + "      title = te ? te.textContent.trim() : '';"
                    + "    }"
                    + "    out.push({"
                    + "      videoId: m[1],"
                    + "      href: 'https://www.douyin.com/video/' + m[1],"
                    + "      cover: img ? img.src : '',"
                    + "      title: title"
                    + "    });"
                    + "  }"
                    + "  return out;"
                    + "}"
                    + "const postList = document.querySelector('[data-e2e=\"user-post-list\"]');"
                    + "if (postList) {"
                    + "  const v = collectFrom(postList);"
                    + "  if (v.length) return v;"
                    + "}"
                    + "const main = document.querySelector('main') || document.querySelector('#douyin-right-container') || document.body;"
                    + "return collectFrom(main);"
                    + "}");
            if (raw instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        ScrapedVideo v = new ScrapedVideo();
                        v.setVideoId(str(map, "videoId"));
                        v.setVideoUrl(str(map, "href"));
                        v.setCoverUrl(str(map, "cover"));
                        v.setTitle(str(map, "title"));
                        result.add(v);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("extractVideoCards 失败: {}", e.getMessage());
        }
        return result;
    }

    private static String str(Map<?, ?> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    /**
     * 合并 Netscape 文件（可选）与后台「抖音 Cookie 管理」中保存的整串 Cookie，供采集使用。
     *
     * @param cookieOwnerId 当前登录用户 id，用于拉取库内 Cookie；为 null 时仅使用文件
     */
    private CookieInjectionResult injectCookies(BrowserContext context, Long cookieOwnerId) {
        List<Cookie> merged = new ArrayList<>();
        merged.addAll(loadCookiesFromNetscapeFile());
        Long usedDbCookieId = null;
        if (cookieOwnerId != null && douyinCookieService != null) {
            try {
                DouyinCookieService.AvailableCookie selected =
                        douyinCookieService.getAvailableCookieForUse(cookieOwnerId, "douyin", workerCookieId);
                String header = selected != null ? selected.cookieValue() : null;
                if (StringUtils.hasText(header)) {
                    usedDbCookieId = selected.id();
                    List<Cookie> fromDb = parseCookieHeaderToPlaywright(header);
                    merged.addAll(fromDb);
                    log.info("AccountVideoScraper 从库解析并合并 {} 条抖音 Cookie（键），cookieId={}, preferredCookieId={}",
                            fromDb.size(), usedDbCookieId, workerCookieId);
                }
            } catch (Exception e) {
                log.warn("读取库内抖音 Cookie 失败: {}", e.getMessage());
            }
        }
        if (merged.isEmpty()) {
            log.debug("AccountVideoScraper 未注入任何 Cookie（无文件且无库内记录）");
            return new CookieInjectionResult(0, usedDbCookieId);
        }
        try {
            context.addCookies(merged);
            log.info("AccountVideoScraper 合计向浏览器上下文注入 {} 条 Cookie", merged.size());
        } catch (Exception e) {
            log.warn("注入 Cookie 失败: {}", e.getMessage());
            return new CookieInjectionResult(0, usedDbCookieId);
        }
        return new CookieInjectionResult(merged.size(), usedDbCookieId);
    }

    private List<Cookie> loadCookiesFromNetscapeFile() {
        List<Cookie> cookies = new ArrayList<>();
        if (!StringUtils.hasText(cookiesFile)) {
            return cookies;
        }
        Path cookiePath = Path.of(cookiesFile.trim());
        if (!Files.isRegularFile(cookiePath)) {
            return cookies;
        }
        try {
            List<String> lines = Files.readAllLines(cookiePath);
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
                log.info("AccountVideoScraper 从文件加载 {} 条 Cookie", cookies.size());
            }
        } catch (Exception e) {
            log.warn("读取 cookies 文件失败: {}", e.getMessage());
        }
        return cookies;
    }

    /** 将浏览器请求头形式的 Cookie 串转为 Playwright {@link Cookie}（域统一为 .douyin.com） */
    private static List<Cookie> parseCookieHeaderToPlaywright(String cookieHeader) {
        List<Cookie> out = new ArrayList<>();
        if (!StringUtils.hasText(cookieHeader)) {
            return out;
        }
        for (String part : cookieHeader.split(";")) {
            String p = part.trim();
            if (p.isEmpty()) {
                continue;
            }
            int eq = p.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String name = p.substring(0, eq).trim();
            String value = eq + 1 < p.length() ? p.substring(eq + 1).trim() : "";
            if (!StringUtils.hasText(name)) {
                continue;
            }
            Cookie c = new Cookie(name, value);
            c.setDomain(".douyin.com");
            c.setPath("/");
            c.setSecure(true);
            out.add(c);
        }
        return out;
    }
}
