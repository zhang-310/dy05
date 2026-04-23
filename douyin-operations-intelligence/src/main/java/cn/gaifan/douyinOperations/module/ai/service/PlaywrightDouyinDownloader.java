package cn.gaifan.douyinOperations.module.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Playwright 无头浏览器抖音视频下载器。
 * <p>
 * 打开抖音视频页，通过 {@code page.onResponse} 拦截含 {@code .mp4} 的 CDN 响应 URL，
 * 再用 Java HttpClient 下载到本地。
 * <p>
 * 需执行 {@code mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"} 安装浏览器。
 * 配置 {@code app.video-analysis.playwright-enabled=true} 启用。
 */
@Component
@ConditionalOnProperty(name = "app.video-analysis.playwright-enabled", havingValue = "true")
public class PlaywrightDouyinDownloader {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightDouyinDownloader.class);

    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile("/video/(\\d{15,25})");

    private static final String CHROME_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36";

    @Value("${app.video-analysis.playwright-timeout-ms:30000}")
    private int timeoutMs;

    @Value("${app.video-analysis.yt-dlp-cookies-file:}")
    private String cookiesFile;

    private final boolean playwrightAvailable;

    public PlaywrightDouyinDownloader() {
        this.playwrightAvailable = checkPlaywrightAvailable();
        if (playwrightAvailable) {
            log.info("PlaywrightDouyinDownloader 已启用（Playwright classpath 可用）");
        } else {
            log.warn("PlaywrightDouyinDownloader 已配置但 Playwright 不在 classpath，将不可用");
        }
    }

    private static boolean checkPlaywrightAvailable() {
        try {
            Class.forName("com.microsoft.playwright.Playwright");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean isAvailable() {
        return playwrightAvailable;
    }

    /**
     * 使用 Playwright 下载抖音视频。
     *
     * @param douyinUrl 抖音视频 URL（长链或短链）
     * @param videosDir 视频保存目录
     * @return 下载的本地视频文件路径
     * @throws Exception 下载失败时抛出
     */
    public String download(String douyinUrl, Path videosDir) throws Exception {
        if (!playwrightAvailable) {
            throw new IllegalStateException("Playwright 不可用");
        }
        return doDownload(douyinUrl, videosDir);
    }

    @SuppressWarnings("resource")
    private String doDownload(String douyinUrl, Path videosDir) throws Exception {
        String videoId = extractVideoId(douyinUrl);

        var playwright = com.microsoft.playwright.Playwright.create();
        try {
            var browser = playwright.chromium().launch(
                    new com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(true));
            try {
                var contextOptions = new com.microsoft.playwright.Browser.NewContextOptions()
                        .setUserAgent(CHROME_UA)
                        .setLocale("zh-CN");
                var context = browser.newContext(contextOptions);

                // 注入 cookies（从 Netscape cookies.txt 读取 .douyin.com 域的 cookie）
                injectCookies(context);

                var page = context.newPage();

                // 拦截 CDN 视频 URL
                AtomicReference<String> cdnUrl = new AtomicReference<>();
                CompletableFuture<String> cdnFuture = new CompletableFuture<>();

                page.onResponse(response -> {
                    String url = response.url();
                    // 匹配抖音 CDN 视频地址模式
                    if (cdnUrl.get() == null && isVideoCdnUrl(url)) {
                        log.info("[Playwright] 拦截到视频 CDN URL: {}...", url.substring(0, Math.min(120, url.length())));
                        if (cdnUrl.compareAndSet(null, url)) {
                            cdnFuture.complete(url);
                        }
                    }
                });

                // 导航到视频页
                String targetUrl = buildTargetUrl(douyinUrl, videoId);
                log.info("[Playwright] 打开: {}", targetUrl);
                page.navigate(targetUrl, new com.microsoft.playwright.Page.NavigateOptions()
                        .setTimeout(timeoutMs));

                // 等待 CDN URL 被拦截（超时由 timeoutMs 控制）
                String interceptedUrl;
                try {
                    interceptedUrl = cdnFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    // 尝试点击播放按钮触发视频加载
                    log.debug("[Playwright] 自动拦截超时，尝试点击播放按钮");
                    try {
                        page.click("xg-video-container, video, [data-e2e='video-player']",
                                new com.microsoft.playwright.Page.ClickOptions().setTimeout(3000));
                        interceptedUrl = cdnFuture.get(10, TimeUnit.SECONDS);
                    } catch (Exception e2) {
                        throw new RuntimeException("Playwright 未能拦截到视频 CDN URL（超时 " + timeoutMs + "ms）", e2);
                    }
                }

                page.close();

                // 用 Java HttpClient 下载视频
                String fileName = (videoId != null ? videoId : "pw_" + System.currentTimeMillis()) + ".mp4";
                Path outputFile = videosDir.resolve(fileName);
                downloadFromCdn(interceptedUrl, outputFile);

                log.info("[Playwright] 视频下载完成: {} ({}KB)", outputFile, Files.size(outputFile) / 1024);
                return outputFile.toString();

            } finally {
                browser.close();
            }
        } finally {
            playwright.close();
        }
    }

    /**
     * 判断 URL 是否为视频 CDN 地址
     */
    private static boolean isVideoCdnUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        // 抖音视频 CDN 常见模式
        return (lower.contains(".mp4") || lower.contains("/play/") || lower.contains("video/tos"))
                && (lower.contains("douyinvod") || lower.contains("bytevcloudcdn")
                || lower.contains("bytecdn") || lower.contains("douyincdn")
                || lower.contains("tiktokcdn") || lower.contains("amemv.com")
                || lower.contains("pstatp.com") || lower.contains("snssdk.com")
                || lower.contains("ixigua.com") || lower.contains("tos-cn-"))
                && !lower.contains(".m3u8");
    }

    private String buildTargetUrl(String originalUrl, String videoId) {
        // 如果已经是 www.douyin.com/video/xxx 格式，直接用
        if (originalUrl.contains("www.douyin.com/video/") && videoId != null) {
            return "https://www.douyin.com/video/" + videoId;
        }
        // 短链或其他格式，也尝试用 video ID 构建标准 URL
        if (videoId != null) {
            return "https://www.douyin.com/video/" + videoId;
        }
        // 回退：直接用原始 URL
        return originalUrl;
    }

    /**
     * 从 Netscape cookies.txt 读取 .douyin.com 域名的 Cookie 并注入到 BrowserContext
     */
    private void injectCookies(com.microsoft.playwright.BrowserContext context) {
        if (!StringUtils.hasText(cookiesFile)) return;
        Path cookiePath = Path.of(cookiesFile.trim());
        if (!Files.isRegularFile(cookiePath)) {
            log.debug("[Playwright] cookies 文件不存在: {}", cookiePath);
            return;
        }
        try {
            var lines = Files.readAllLines(cookiePath);
            var cookies = new java.util.ArrayList<com.microsoft.playwright.options.Cookie>();
            for (String line : lines) {
                if (line.startsWith("#") || line.isBlank()) continue;
                String[] parts = line.split("\t");
                if (parts.length < 7) continue;
                String domain = parts[0];
                if (!domain.contains("douyin")) continue;
                var cookie = new com.microsoft.playwright.options.Cookie(parts[5], parts[6]);
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
                log.info("[Playwright] 注入 {} 条抖音 Cookie", cookies.size());
            }
        } catch (Exception e) {
            log.warn("[Playwright] 读取 cookies.txt 失败: {}", e.getMessage());
        }
    }

    private void downloadFromCdn(String cdnUrl, Path outputFile) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(cdnUrl))
                .header("User-Agent", CHROME_UA)
                .header("Referer", "https://www.douyin.com/")
                .timeout(Duration.ofMinutes(5))
                .GET()
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new RuntimeException("CDN 下载失败: HTTP " + response.statusCode());
        }

        try (InputStream is = response.body()) {
            Files.copy(is, outputFile, StandardCopyOption.REPLACE_EXISTING);
        }

        long size = Files.size(outputFile);
        if (size < 10_000) {
            Files.deleteIfExists(outputFile);
            throw new RuntimeException("CDN 下载文件过小 (" + size + "B)，可能不是有效视频");
        }
    }

    /**
     * 从抖音 URL 中提取视频 ID（复用 VideoAnalysisService 逻辑）
     */
    private String extractVideoId(String url) {
        if (url == null) return null;
        Matcher m = VIDEO_ID_PATTERN.matcher(url);
        if (m.find()) return m.group(1);
        // 短链需先解析重定向
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
                if (m2.find()) return m2.group(1);
            } catch (Exception e) {
                log.debug("[Playwright] 解析短链失败: {}", e.getMessage());
            }
        }
        return null;
    }
}
