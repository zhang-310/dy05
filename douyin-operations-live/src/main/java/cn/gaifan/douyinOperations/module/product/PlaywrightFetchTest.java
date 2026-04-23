package cn.gaifan.douyinOperations.module.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 独立测试 Playwright 渲染好货链接，无需启动 Spring。
 * 运行: mvn exec:java -Dexec.mainClass=cn.gaifan.douyinOperations.module.product.PlaywrightFetchTest
 */
public class PlaywrightFetchTest {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightFetchTest.class);

    private static final String URL = "https://haohuo.jinritemai.com/ecommerce/trade/detail/index.html?id=3805723723315675368&origin_type=604";

    public static void main(String[] args) {
        log.info("测试 Playwright 渲染: {}", URL);
        try {
            var playwright = com.microsoft.playwright.Playwright.create();
            var browser = playwright.chromium().launch();
            var context = browser.newContext(new com.microsoft.playwright.Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"));
            var page = context.newPage();
            page.navigate(URL, new com.microsoft.playwright.Page.NavigateOptions().setTimeout(20000));
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE, new com.microsoft.playwright.Page.WaitForLoadStateOptions().setTimeout(15000));
            String html = page.content();
            browser.close();
            playwright.close();

            log.info("HTML 长度: {}", html.length());
            // 提取标题
            Pattern ogTitle = Pattern.compile("<meta[^>]+property=\"og:title\"[^>]+content=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
            Pattern titleTag = Pattern.compile("<title[^>]*>([^<]+)</title>", Pattern.CASE_INSENSITIVE);
            Matcher m = ogTitle.matcher(html);
            if (m.find()) {
                log.info("og:title: {}", m.group(1));
            } else {
                m = titleTag.matcher(html);
                if (m.find()) log.info("<title>: {}", m.group(1));
            }
            // 检查是否有商品相关文本
            if (html.contains("productName") || html.contains("title") || html.contains("价格") || html.contains("¥")) {
                log.info("✓ 检测到商品相关内容");
            } else {
                log.warn("⚠ 未检测到明显商品字段，可能需更长等待");
            }
            log.info("渲染成功");
        } catch (Exception e) {
            log.error("渲染失败: {}", e.getMessage(), e);
        }
    }
}
