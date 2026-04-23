package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvComment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 爆款视频评论抓取器（Playwright 滚动评论区）。
 * <p>
 * 使用 Playwright 打开视频页，展开评论区，循环滚动收集评论。
 * 停止条件：连续 3 次滚动无新评论 OR 达到 maxComments 上限。
 * <p>
 * 需 Playwright 在 classpath 且已安装 Chromium。
 * 配置 {@code app.viral-analysis.comment-extraction-enabled=true} 启用。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.viral-analysis.comment-extraction-enabled", havingValue = "true", matchIfMissing = true)
public class ViralCommentExtractor {

    @Value("${app.viral-analysis.max-comments:200}")
    private int maxComments;

    @Value("${app.viral-analysis.comment-scroll-timeout-ms:60000}")
    private long scrollTimeoutMs;

    @Value("${app.video-analysis.yt-dlp-cookies-file:}")
    private String cookiesFile;

    private final boolean playwrightAvailable;

    public ViralCommentExtractor() {
        this.playwrightAvailable = checkPlaywrightAvailable();
        if (playwrightAvailable) {
            log.info("ViralCommentExtractor 已启用（Playwright classpath 可用）");
        } else {
            log.warn("ViralCommentExtractor 已配置但 Playwright 不在 classpath，评论抓取将不可用");
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

    /**
     * 抓取视频评论。
     *
     * @param videoUrl   视频页面 URL
     * @param viralVideoId 爆款视频 ID（用于关联评论）
     * @return 评论列表（videoSource="viral"）；不可用时返回空列表
     */
    public List<SvComment> extractComments(String videoUrl, Long viralVideoId) {
        if (!playwrightAvailable) {
            log.info("[评论抓取] Playwright 不可用，跳过评论抓取 viralId={}", viralVideoId);
            return List.of();
        }
        try {
            return doExtract(videoUrl, viralVideoId);
        } catch (Exception e) {
            log.warn("[评论抓取] 失败 viralId={}: {}", viralVideoId, e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<SvComment> doExtract(String videoUrl, Long viralVideoId) {
        // 使用反射调用 Playwright API，避免编译时对 Playwright 的硬依赖
        List<SvComment> comments = new ArrayList<>();

        try {
            var playwrightClass = Class.forName("com.microsoft.playwright.Playwright");
            var createMethod = playwrightClass.getMethod("create");
            var pw = createMethod.invoke(null);

            try {
                var chromium = playwrightClass.getMethod("chromium").invoke(pw);
                var browserTypeClass = Class.forName("com.microsoft.playwright.BrowserType");

                var launchOptionsClass = Class.forName("com.microsoft.playwright.BrowserType$LaunchOptions");
                var launchOptions = launchOptionsClass.getDeclaredConstructor().newInstance();
                launchOptionsClass.getMethod("setHeadless", boolean.class).invoke(launchOptions, true);

                var browser = browserTypeClass.getMethod("launch", launchOptionsClass).invoke(chromium, launchOptions);
                var browserClass = Class.forName("com.microsoft.playwright.Browser");

                try {
                    var contextOptionsClass = Class.forName("com.microsoft.playwright.Browser$NewContextOptions");
                    var ctxOpts = contextOptionsClass.getDeclaredConstructor().newInstance();
                    contextOptionsClass.getMethod("setUserAgent", String.class).invoke(ctxOpts,
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36");

                    var context = browserClass.getMethod("newContext", contextOptionsClass).invoke(browser, ctxOpts);
                    var contextClass = Class.forName("com.microsoft.playwright.BrowserContext");
                    var page = contextClass.getMethod("newPage").invoke(context);
                    var pageClass = Class.forName("com.microsoft.playwright.Page");

                    // 导航到视频页
                    var navOptionsClass = Class.forName("com.microsoft.playwright.Page$NavigateOptions");
                    var navOpts = navOptionsClass.getDeclaredConstructor().newInstance();
                    navOptionsClass.getMethod("setTimeout", double.class).invoke(navOpts, (double) scrollTimeoutMs);
                    pageClass.getMethod("navigate", String.class, navOptionsClass).invoke(page, videoUrl, navOpts);

                    // 等待页面加载
                    Thread.sleep(3000);

                    // 尝试点击评论区展开
                    try {
                        var locator = pageClass.getMethod("locator", String.class)
                                .invoke(page, "[data-e2e='comment-icon'], .comment-icon, [class*='comment']");
                        var locatorClass = Class.forName("com.microsoft.playwright.Locator");
                        var clickOptionsClass = Class.forName("com.microsoft.playwright.Locator$ClickOptions");
                        var clickOpts = clickOptionsClass.getDeclaredConstructor().newInstance();
                        clickOptionsClass.getMethod("setTimeout", double.class).invoke(clickOpts, 5000.0);
                        locatorClass.getMethod("first").invoke(locator);
                        locatorClass.getMethod("click", clickOptionsClass).invoke(locator, clickOpts);
                        Thread.sleep(2000);
                    } catch (Exception e) {
                        log.debug("[评论抓取] 未找到评论按钮，尝试直接滚动: {}", e.getMessage());
                    }

                    // 滚动收集评论
                    Set<String> seenContents = new HashSet<>();
                    int noNewRounds = 0;
                    long deadline = System.currentTimeMillis() + scrollTimeoutMs;

                    while (comments.size() < maxComments && noNewRounds < 3 && System.currentTimeMillis() < deadline) {
                        // 抓取当前可见的评论
                        var commentLocator = pageClass.getMethod("locator", String.class)
                                .invoke(page, "[data-e2e='comment-list-item'], .comment-item, [class*='CommentItem']");
                        var locatorClass = Class.forName("com.microsoft.playwright.Locator");
                        int count = (int) locatorClass.getMethod("count").invoke(commentLocator);

                        int newInThisRound = 0;
                        for (int i = 0; i < count && comments.size() < maxComments; i++) {
                            var item = locatorClass.getMethod("nth", int.class).invoke(commentLocator, i);
                            String text;
                            try {
                                text = (String) locatorClass.getMethod("innerText").invoke(item);
                            } catch (Exception e) {
                                continue;
                            }
                            if (text == null || text.isBlank() || seenContents.contains(text)) {
                                continue;
                            }
                            seenContents.add(text);
                            newInThisRound++;

                            SvComment comment = new SvComment();
                            comment.setVideoId(viralVideoId);
                            comment.setVideoSource("viral");
                            comment.setContent(text.length() > 2000 ? text.substring(0, 2000) : text);
                            comment.setDeleted(0);
                            comments.add(comment);
                        }

                        if (newInThisRound == 0) {
                            noNewRounds++;
                        } else {
                            noNewRounds = 0;
                        }

                        // 滚动评论区
                        pageClass.getMethod("evaluate", String.class).invoke(page,
                                "window.scrollBy(0, window.innerHeight)");
                        Thread.sleep(1500);
                    }

                    contextClass.getMethod("close").invoke(context);
                } finally {
                    browserClass.getMethod("close").invoke(browser);
                }
            } finally {
                playwrightClass.getMethod("close").invoke(pw);
            }
        } catch (Exception e) {
            log.warn("[评论抓取] Playwright 执行异常: {}", e.getMessage());
        }

        log.info("[评论抓取] viralId={} 抓取到 {} 条评论", viralVideoId, comments.size());
        return comments;
    }
}
