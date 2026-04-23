package cn.gaifan.douyinOperations.module.product.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 使用 Playwright 无头浏览器渲染 SPA 页面，获取完整 HTML。
 * 需执行 {@code mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install"} 安装 Chromium。
 * 配置 app.product.use-playwright=true 启用。
 */
@Component
@ConditionalOnProperty(name = "app.product.use-playwright", havingValue = "true")
public class PlaywrightHtmlFetcher {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightHtmlFetcher.class);
    private static final int DEFAULT_TIMEOUT_MS = 15_000;

    @Value("${app.product.playwright-timeout-ms:" + DEFAULT_TIMEOUT_MS + "}")
    private int timeoutMs;

    private volatile boolean available;

    public PlaywrightHtmlFetcher() {
        this.available = checkPlaywrightAvailable();
    }

    private static boolean checkPlaywrightAvailable() {
        try {
            Class.forName("com.microsoft.playwright.Playwright");
            return true;
        } catch (ClassNotFoundException e) {
            log.warn("Playwright 未在 classpath，SPA 渲染不可用。添加 playwright 依赖并执行 playwright install");
            return false;
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * 使用无头浏览器渲染页面并返回 HTML
     *
     * @param url 页面 URL
     * @return 渲染后的 HTML，失败返回 null
     */
    public String fetch(String url) {
        if (!available) return null;
        try {
            return doFetch(url);
        } catch (Exception e) {
            log.warn("Playwright 抓取失败: {} - {}", url, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("resource")
    private String doFetch(String url) throws Exception {
        var playwright = com.microsoft.playwright.Playwright.create();
        try {
            var browser = playwright.chromium().launch();
            var context = browser.newContext(new com.microsoft.playwright.Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"));
            var page = context.newPage();
            page.navigate(url, new com.microsoft.playwright.Page.NavigateOptions().setTimeout(timeoutMs));
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE, new com.microsoft.playwright.Page.WaitForLoadStateOptions().setTimeout(timeoutMs));
            String html = page.content();
            browser.close();
            return html;
        } finally {
            playwright.close();
        }
    }
}
