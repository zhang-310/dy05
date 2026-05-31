package cn.gaifan.douyinOperations.module.benchmark.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.benchmark.service.DouyinCookieQrLoginService;
import cn.gaifan.douyinOperations.module.benchmark.vo.DouyinQrLoginPollResultVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.DouyinQrLoginStartResultVO;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.LoadState;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端打开抖音网页并截图二维码；用户用抖音 App 扫码后轮询 Cookie。
 */
@Slf4j
@Service
public class DouyinCookieQrLoginServiceImpl implements DouyinCookieQrLoginService {

    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36";

    @Value("${app.douyin-cookie.qr-timeout-ms:300000}")
    private long qrTimeoutMs;

    @Value("${app.douyin-cookie.qr-headless:true}")
    private boolean qrHeadless;

    /** 打开首页后是否尝试点击「登录」以弹出扫码框（默认 true；若反爬导致点不到可设 false 并配合 qr-login-url） */
    @Value("${app.douyin-cookie.qr-click-login:true}")
    private boolean qrClickLogin;

    /**
     * 优先打开的页面：默认抖音首页（右上角有「登录」可弹出扫码）；若仅打开 sso.douyin.com 根路径，无头环境常看不到扫码区。
     * 可通过配置改为 {@code https://sso.douyin.com/} 等，并依赖 {@link #qrFallbackSso} 与强化的点击逻辑。
     */
    @Value("${app.douyin-cookie.qr-login-url:https://www.douyin.com/}")
    private String qrLoginUrl;

    /**
     * 在首页未点到「登录」时，是否再打开 https://sso.douyin.com/ 尝试露出扫码页（默认 true）。
     */
    @Value("${app.douyin-cookie.qr-fallback-sso:true}")
    private boolean qrFallbackSso;

    /** 可选步骤（点登录、点链接）的超时，避免 Playwright 对不存在节点等待 8s 刷 WARN */
    @Value("${app.douyin-cookie.qr-opt-click-timeout-ms:2500}")
    private int qrOptClickTimeoutMs;

    /** 截图是否整页（整页较慢）；登录页二维码多在首屏，默认 false 以缩短 /qr-login/start */
    @Value("${app.douyin-cookie.qr-screenshot-full-page:false}")
    private boolean qrScreenshotFullPage;

    @Value("${app.douyin-cookie.session-eviction-enabled:true}")
    private boolean sessionEvictionEnabled;

    private static final String SSO_LOGIN_URL = "https://sso.douyin.com/";
    /** 部分环境下根路径无登录 UI，带 login 的路径更易露出扫码/账号登录页 */
    private static final String SSO_LOGIN_PATH_URL = "https://sso.douyin.com/login/";

    private final Map<String, QrSession> sessionsById = new ConcurrentHashMap<>();
    private final Map<Long, String> sessionIdByOwner = new ConcurrentHashMap<>();

    private volatile boolean playwrightAvailable;

    @PostConstruct
    void detectPlaywright() {
        try {
            Class.forName("com.microsoft.playwright.Playwright");
            playwrightAvailable = true;
        } catch (ClassNotFoundException e) {
            playwrightAvailable = false;
            log.warn("Playwright 类未找到，扫码登录不可用");
        }
    }

    @Override
    public DouyinQrLoginStartResultVO start(Long ownerId) {
        if (ownerId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (!playwrightAvailable) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "Playwright 未就绪，请在服务器执行: mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args=\"install chromium\"（或参考项目 README）");
        }
        cancelExistingForOwner(ownerId);

        String sessionId = UUID.randomUUID().toString();
        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        Page page = null;
        try {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(qrHeadless));
            context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent(UA)
                    .setLocale("zh-CN")
                    .setViewportSize(1365, 768));
            page = context.newPage();
            openDouyinLoginSurface(page);
            byte[] png = page.screenshot(new Page.ScreenshotOptions().setFullPage(qrScreenshotFullPage));

            QrSession session = new QrSession(ownerId, playwright, browser, context, page, System.currentTimeMillis());
            sessionsById.put(sessionId, session);
            sessionIdByOwner.put(ownerId, sessionId);

            String b64 = Base64.getEncoder().encodeToString(png);
            log.info("抖音扫码会话已创建: ownerId={}, sessionId={}", ownerId, sessionId);
            return new DouyinQrLoginStartResultVO(sessionId, b64,
                    "请使用抖音 App 扫描图中「登录」后的二维码（服务器已尝试打开登录入口）。若仍无法识别，可将 app.douyin-cookie.qr-headless=false 调试，或手动添加 Cookie。");
        } catch (Exception e) {
            closeQuietly(page, context, browser, playwright);
            log.error("启动抖音扫码页失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "打开抖音页面失败: " + e.getMessage());
        }
    }

    @Override
    public DouyinQrLoginPollResultVO poll(String sessionId, Long ownerId) {
        if (!hasText(sessionId)) {
            return new DouyinQrLoginPollResultVO("error", null, "sessionId 为空");
        }
        QrSession session = sessionsById.get(sessionId);
        if (session == null) {
            return new DouyinQrLoginPollResultVO("expired", null, "会话已结束或不存在，请重新获取二维码");
        }
        if (!session.ownerId.equals(ownerId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作此会话");
        }
        synchronized (session) {
            if (System.currentTimeMillis() - session.createdAt > qrTimeoutMs) {
                closeSession(sessionId);
                return new DouyinQrLoginPollResultVO("expired", null, "扫码超时，请重试");
            }
            try {
                long ageMs = System.currentTimeMillis() - session.createdAt;
                // 手机端已确认后，网页有时需一次刷新才能把 HttpOnly Cookie 同步到当前上下文
                if (!session.reloadAttempted && ageMs > 10_000 && session.page != null) {
                    session.reloadAttempted = true;
                    try {
                        log.info("抖音扫码：超过 10s 仍未识别登录态，尝试 page.reload 同步 Cookie");
                        session.page.reload(new Page.ReloadOptions().setTimeout(25_000));
                        session.page.waitForTimeout(1200);
                    } catch (Exception ex) {
                        log.warn("page.reload 失败: {}", ex.getMessage());
                    }
                }
                if (session.page != null) {
                    session.page.waitForTimeout(200);
                }
                List<Cookie> cookies = session.context.cookies();
                if (looksLoggedIn(cookies)) {
                    return buildPollSuccess(sessionId, cookies, false);
                }
                if (looksLoggedInWeak(cookies, ageMs)) {
                    log.info("抖音扫码：弱校验通过（SSO/passport 特征 + 足够 Cookie），Cookie 数={}", cookies.size());
                    return buildPollSuccess(sessionId, cookies, true);
                }
            } catch (Exception e) {
                log.warn("轮询 Cookie 失败: {}", e.getMessage());
                return new DouyinQrLoginPollResultVO("waiting", null, "等待扫码…");
            }
            List<Cookie> snap = session.context.cookies();
            Set<String> nameSet = cookieNameSet(snap);
            if (log.isDebugEnabled()) {
                log.debug("抖音扫码轮询：尚未识别为登录态，Cookie 名: {}", nameSet);
            }
            if (!session.diagnosticLogged && snap.size() >= 3) {
                session.diagnosticLogged = true;
                log.info("抖音扫码：仍未判定为已登录（规则未命中），Cookie 数={}，名={}", snap.size(), nameSet);
            }
            String hint = buildWaitingHint(snap.size(), nameSet);
            return new DouyinQrLoginPollResultVO("waiting", null, hint);
        }
    }

    @Override
    public void cancel(String sessionId, Long ownerId) {
        if (!hasText(sessionId)) {
            return;
        }
        QrSession session = sessionsById.get(sessionId);
        if (session != null && session.ownerId.equals(ownerId)) {
            closeSession(sessionId);
        }
    }

    @Scheduled(fixedRateString = "${app.douyin-cookie.session-eviction-fixed-rate-ms:60000}")
    public void evictExpiredSessions() {
        if (!sessionEvictionEnabled) {
            return;
        }
        long now = System.currentTimeMillis();
        for (String sid : Set.copyOf(sessionsById.keySet())) {
            QrSession s = sessionsById.get(sid);
            if (s != null && now - s.createdAt > qrTimeoutMs) {
                log.info("清理超时抖音扫码会话: sessionId={}", sid);
                closeSession(sid);
            }
        }
    }

    private void cancelExistingForOwner(Long ownerId) {
        String old = sessionIdByOwner.remove(ownerId);
        if (old != null) {
            closeSession(old);
        }
    }

    private void closeSession(String sessionId) {
        QrSession s = sessionsById.remove(sessionId);
        if (s == null) {
            return;
        }
        sessionIdByOwner.entrySet().removeIf(e -> sessionId.equals(e.getValue()));
        synchronized (s) {
            closeQuietly(s.page, s.context, s.browser, s.playwright);
        }
    }

    private static void closeQuietly(Page page, BrowserContext context, Browser browser, Playwright playwright) {
        try {
            if (page != null) {
                page.close();
            }
        } catch (Exception ignored) {
        }
        try {
            if (context != null) {
                context.close();
            }
        } catch (Exception ignored) {
        }
        try {
            if (browser != null) {
                browser.close();
            }
        } catch (Exception ignored) {
        }
        try {
            if (playwright != null) {
                playwright.close();
            }
        } catch (Exception ignored) {
        }
    }

    private DouyinQrLoginPollResultVO buildPollSuccess(String sessionId, List<Cookie> cookies, boolean weak) {
        String header = buildCookieHeaderAll(cookies);
        if (!hasText(header)) {
            header = buildCookieHeader(cookies);
        }
        closeSession(sessionId);
        String msg = weak
                ? "已获取 Cookie（弱校验：未识别到典型 sessionid/sid_guard，但检测到 SSO/passport 流程与足够条目；请先保存，再在列表中点「验证」或自行试爬）"
                : "已获取登录 Cookie（整串含多键值），请填写名称后保存";
        return new DouyinQrLoginPollResultVO("success", header, msg);
    }

    /** 按 Cookie 值判断：部分环境下键名与 Set 精确匹配不一致 */
    private static boolean hasExplicitSessionCookie(List<Cookie> cookies) {
        for (Cookie c : cookies) {
            if (c == null || c.name == null) {
                continue;
            }
            if (!hasText(c.value)) {
                continue;
            }
            String n = c.name.toLowerCase(Locale.ROOT);
            if ("sessionid".equals(n) || "sessionid_ss".equals(n)) {
                return true;
            }
            if ("sid_tt".equals(n) || "sid_guard".equals(n)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 抖音 SSO/无头环境下，手机已确认后仍可能长时间只有 passport_auth_mix_state、ttwid、设备类等，
     * 典型 session 键名延迟或落在其它域。轮询超过一定时间且条目足够时允许落库。
     */
    private boolean looksLoggedInWeak(List<Cookie> cookies, long ageMs) {
        if (cookies.size() < 18) {
            return false;
        }
        if (ageMs < 28_000) {
            return false;
        }
        Set<String> names = new HashSet<>();
        for (Cookie c : cookies) {
            if (c.name != null) {
                names.add(c.name);
            }
        }
        if (!names.contains("ttwid")) {
            return false;
        }
        boolean hasMixState = names.contains("passport_auth_mix_state") || names.contains("passport_auth_status");
        if (!hasMixState) {
            return false;
        }
        boolean anyPassport = names.stream().anyMatch(n -> n != null && n.startsWith("passport"));
        return anyPassport;
    }

    private boolean looksLoggedIn(List<Cookie> cookies) {
        if (cookies == null || cookies.isEmpty()) {
            return false;
        }
        if (hasExplicitSessionCookie(cookies)) {
            return true;
        }
        Set<String> names = new HashSet<>();
        for (Cookie c : cookies) {
            names.add(c.name);
        }
        if (names.contains("sid_guard") || names.contains("sid_tt")) {
            return true;
        }
        if (names.contains("sessionid") && names.contains("uid_tt")) {
            return true;
        }
        // 网页版常见变体：sessionid_ss、或 odin_tt 与会话类 cookie 并存
        if (names.contains("sessionid_ss") && (names.contains("uid_tt") || names.contains("sid_guard"))) {
            return true;
        }
        if (names.contains("odin_tt")
                && (names.contains("sessionid") || names.contains("sessionid_ss") || names.contains("sid_guard"))) {
            return true;
        }
        // passport_assist_user 一般在登录完成后出现
        if (names.contains("passport_assist_user") && names.contains("sessionid")) {
            return true;
        }
        // 新版/变体：csrf + session、或仅 uid_tt + sessionid_ss
        if (names.contains("passport_csrf_token") && (names.contains("sessionid") || names.contains("sessionid_ss"))) {
            return true;
        }
        if (names.contains("uid_tt") && names.contains("sessionid_ss")) {
            return true;
        }
        // 登录态常见组合：bd_ticket_guard 系列
        if (names.contains("bd_ticket_guard_client_data") && names.contains("sessionid")) {
            return true;
        }
        // SSO / passport 常见
        if (names.contains("passport_auth_status") && (names.contains("sessionid") || names.contains("sessionid_ss"))) {
            return true;
        }
        // 网页版变体：odin_tt 与 sessionid 同现即视为已建立会话
        if (names.contains("odin_tt") && (names.contains("sessionid") || names.contains("sessionid_ss"))) {
            return true;
        }
        // sessionid_ss + uid_tt 无 sid_guard 时的组合
        if (names.contains("sessionid_ss") && names.contains("uid_tt")) {
            return true;
        }
        // 宽松兜底：会话类 + 用户类同时存在且 Cookie 数量较多（排除纯游客少量跟踪 Cookie）
        if (cookies.size() >= 8) {
            boolean hasSessionToken = names.contains("sessionid") || names.contains("sessionid_ss")
                    || names.contains("sid_tt") || names.contains("sid_guard");
            boolean hasUserToken = names.contains("uid_tt") || names.contains("odin_tt");
            if (hasSessionToken && hasUserToken) {
                return true;
            }
        }
        return false;
    }

    /** 轮询等待文案：带 Cookie 数量与名称预览，便于确认手机已授权但识别规则未命中时排查 */
    private static String buildWaitingHint(int cookieCount, Set<String> names) {
        String preview = cookieNamesPreview(names, 10);
        String base = "等待识别登录态… 请在手机抖音上确认授权。若已确认仍停留此处，可查看服务端日志。";
        if (cookieCount <= 0) {
            return base + " 当前浏览器上下文尚无 Cookie。";
        }
        return base + " 当前已捕获 " + cookieCount + " 枚 Cookie" + (preview.isEmpty() ? "。" : ("（含 " + preview + "）。"));
    }

    private static String cookieNamesPreview(Set<String> names, int maxNames) {
        if (names == null || names.isEmpty()) {
            return "";
        }
        Iterator<String> it = names.iterator();
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (it.hasNext() && i < maxNames) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(it.next());
            i++;
        }
        if (names.size() > maxNames) {
            sb.append(" 等共 ").append(names.size()).append(" 个");
        }
        return sb.toString();
    }

    /**
     * 打开抖音页并尽量露出扫码登录：仅首页截图往往没有登录二维码，需点「登录」。
     */
    private void openDouyinLoginSurface(Page page) {
        String url = qrLoginUrl != null && !qrLoginUrl.isBlank() ? qrLoginUrl.trim() : SSO_LOGIN_URL;
        page.navigate(url, new Page.NavigateOptions().setTimeout(120_000));
        waitDomSettle(page);
        if (!qrClickLogin) {
            return;
        }
        boolean opened = tryClickLoginEntry(page);
        page.waitForTimeout(opened ? 2200 : 800);
        if (!opened && qrFallbackSso) {
            log.info("当前页未点到「登录」，将依次尝试 {} 与 {}", SSO_LOGIN_PATH_URL, SSO_LOGIN_URL);
            try {
                page.navigate(SSO_LOGIN_PATH_URL, new Page.NavigateOptions().setTimeout(120_000));
                waitDomSettle(page);
                opened = tryClickLoginEntry(page);
                page.waitForTimeout(opened ? 2200 : 800);
            } catch (Exception e) {
                log.debug("打开 SSO /login 失败: {}", e.getMessage());
            }
            if (!opened) {
                try {
                    page.navigate(SSO_LOGIN_URL, new Page.NavigateOptions().setTimeout(120_000));
                    waitDomSettle(page);
                    opened = tryClickLoginEntry(page);
                    page.waitForTimeout(opened ? 2200 : 1200);
                } catch (Exception e) {
                    log.debug("打开 SSO 根路径失败，仍使用当前页截图: {}", e.getMessage());
                }
            }
        }
    }

    /** 导航后等待 DOM，避免脚本未完成时定位不到「登录」 */
    private static void waitDomSettle(Page page) {
        try {
            page.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(45_000));
        } catch (Exception ignored) {
        }
        page.waitForTimeout(900);
    }

    /**
     * 尝试点击「登录」以弹出二维码。必须先 {@link Locator#count()} 再点，避免 Playwright 对「零匹配」定位器等待直至超时并打 WARN。
     *
     * @return 是否成功点到任一入口
     */
    private boolean tryClickLoginEntry(Page page) {
        int optMs = Math.max(800, qrOptClickTimeoutMs);
        Locator.ClickOptions clickOpt = new Locator.ClickOptions().setTimeout(optMs).setForce(true);

        String[] texts = {"登录", "登 录"};
        for (String t : texts) {
            try {
                Locator byText = page.getByText(t, new Page.GetByTextOptions().setExact(true));
                int n = byText.count();
                for (int i = 0; i < n; i++) {
                    Locator el = byText.nth(i);
                    try {
                        if (el.isVisible()) {
                            el.click(clickOpt);
                            log.info("已点击可见节点「{}」以打开登录/扫码区域 (第 {} 个匹配)", t, i);
                            return true;
                        }
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception e) {
                log.debug("点击「{}」失败: {}", t, e.getMessage());
            }
        }
        try {
            Locator loose = page.getByText("登录");
            int n = loose.count();
            for (int i = 0; i < n; i++) {
                Locator el = loose.nth(i);
                try {
                    if (el.isVisible()) {
                        el.click(clickOpt);
                        log.info("已点击非精确匹配「登录」(第 {} 个可见节点)", i);
                        return true;
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            log.debug("非精确「登录」失败: {}", e.getMessage());
        }
        for (AriaRole role : new AriaRole[]{AriaRole.LINK, AriaRole.BUTTON}) {
            try {
                Locator byRole = page.getByRole(role, new Page.GetByRoleOptions().setName("登录"));
                int n = byRole.count();
                for (int i = 0; i < n; i++) {
                    Locator el = byRole.nth(i);
                    try {
                        if (el.isVisible()) {
                            el.click(clickOpt);
                            log.info("已通过 getByRole({}) 点击可见「登录」", role);
                            return true;
                        }
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception e) {
                log.debug("getByRole 登录失败: {}", e.getMessage());
            }
        }
        try {
            Locator headerLogin =
                    page.locator("header").getByText("登录", new Locator.GetByTextOptions().setExact(true));
            if (headerLogin.count() >= 1 && headerLogin.first().isVisible()) {
                headerLogin.first().click(clickOpt);
                log.info("已通过 header 内精确「登录」打开登录区域");
                return true;
            }
        } catch (Exception e) {
            log.debug("header 登录点击失败: {}", e.getMessage());
        }
        Locator links = page.locator("a[href*='login'], a[href*='passport'], a[href*='Login'], [data-e2e='nav-login']");
        try {
            if (links.count() < 1) {
                log.debug("当前页无匹配登录链接选择器，将依赖后续 SSO 回退或整页截图");
                return false;
            }
            int n = links.count();
            for (int i = 0; i < n; i++) {
                Locator el = links.nth(i);
                try {
                    if (el.isVisible()) {
                        el.click(clickOpt);
                        log.info("已通过登录链接打开登录区域 (第 {} 个)", i);
                        return true;
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            log.debug("登录链接点击未成功: {}", e.getMessage());
        }
        return false;
    }

    private static Set<String> cookieNameSet(List<Cookie> cookies) {
        Set<String> names = new HashSet<>();
        for (Cookie c : cookies) {
            names.add(c.name);
        }
        return names;
    }

    private String buildCookieHeader(List<Cookie> cookies) {
        StringBuilder sb = new StringBuilder();
        for (Cookie c : cookies) {
            if (c.domain == null) {
                continue;
            }
            if (!c.domain.contains("douyin") && !c.domain.contains("iesdouyin")) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(c.name).append("=").append(c.value);
        }
        return sb.toString();
    }

    /** 域名过滤过严时的兜底：拼接当前上下文下全部 Cookie */
    private String buildCookieHeaderAll(List<Cookie> cookies) {
        StringBuilder sb = new StringBuilder();
        for (Cookie c : cookies) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(c.name).append("=").append(c.value);
        }
        return sb.toString();
    }

    /** 与 Spring StringUtils 区分，避免依赖 org.springframework 在本类仅用于 hasText 时混用 */
    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static final class QrSession {
        final Long ownerId;
        final Playwright playwright;
        final Browser browser;
        final BrowserContext context;
        final Page page;
        final long createdAt;
        /** 是否已对页面做过一次 reload，用于同步扫码后的 Cookie */
        boolean reloadAttempted;
        /** 是否已打过一次「仍未登录」诊断日志，避免刷屏 */
        boolean diagnosticLogged;

        QrSession(Long ownerId, Playwright playwright, Browser browser, BrowserContext context, Page page, long createdAt) {
            this.ownerId = ownerId;
            this.playwright = playwright;
            this.browser = browser;
            this.context = context;
            this.page = page;
            this.createdAt = createdAt;
        }
    }
}
