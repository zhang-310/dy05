package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.shortvideo.util.DouyinSharePasteParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 统一输入解析：将用户粘贴的任意文本（视频链接/账号主页/抖音号/分享文本）
 * 归一化为可用于采集的账号主页 URL。
 */
@Slf4j
@Component
public class DouyinUrlResolver {

    private static final Pattern USER_URL_PATTERN = Pattern.compile(
            "https?://(www\\.)?douyin\\.com/user/[\\w\\-]+", Pattern.CASE_INSENSITIVE);

    private static final Pattern HTTP_PATTERN = Pattern.compile("https?://", Pattern.CASE_INSENSITIVE);

    @Autowired(required = false)
    private AccountVideoScraper accountVideoScraper;

    public record ResolveResult(
            String inputType,
            String accountUrl,
            String accountName,
            String secUid,
            String singleVideoUrl
    ) {}

    /**
     * 智能识别输入文本，返回解析结果。
     * inputType: account_url / video_url / douyin_id / unknown
     *
     * @param cookieOwnerId 当前用户 id，用于 Playwright 拉取库内抖音 Cookie；可为 null
     */
    public ResolveResult resolve(String rawInput, Long cookieOwnerId) {
        if (!StringUtils.hasText(rawInput)) {
            return new ResolveResult("unknown", null, null, null, null);
        }
        String input = rawInput.trim();

        // 1. 检查是否含账号主页 URL
        Matcher userMatcher = USER_URL_PATTERN.matcher(input);
        if (userMatcher.find()) {
            String accountUrl = userMatcher.group();
            String secUid = extractSecUidFromUrl(accountUrl);
            log.info("识别为账号主页 URL: {}", accountUrl);
            return new ResolveResult("account_url", accountUrl, null, secUid, null);
        }

        // 2. 检查是否含视频 URL（主站长链或短链或分享文本）
        Optional<String> videoUrl = DouyinSharePasteParser.resolveCollectVideoUrl(input);
        if (videoUrl.isPresent()) {
            String url = videoUrl.get();
            log.info("识别为视频链接: {}", url);
            String accountUrl = tryExtractAuthorFromVideo(url, cookieOwnerId);
            String secUid = accountUrl != null ? extractSecUidFromUrl(accountUrl) : null;
            String accountName = null;
            if (accountUrl != null && accountVideoScraper != null) {
                accountName = tryExtractAccountNameFromVideo(url, cookieOwnerId);
            }
            return new ResolveResult("video_url", accountUrl, accountName, secUid, url);
        }

        // 3. 无 URL 特征 -> 视为抖音号
        if (!HTTP_PATTERN.matcher(input).find()) {
            String cleanId = input.replaceAll("[\\s@#]", "");
            if (StringUtils.hasText(cleanId) && cleanId.length() <= 64) {
                log.info("识别为抖音号: {}", cleanId);
                String accountUrl = trySearchDouyinAccount(cleanId, cookieOwnerId);
                String secUid = accountUrl != null ? extractSecUidFromUrl(accountUrl) : null;
                return new ResolveResult("douyin_id", accountUrl, null, secUid, null);
            }
        }

        return new ResolveResult("unknown", null, null, null, null);
    }

    /** 兼容旧调用：无库内 Cookie 上下文 */
    public ResolveResult resolve(String rawInput) {
        return resolve(rawInput, null);
    }

    private String extractSecUidFromUrl(String url) {
        if (accountVideoScraper != null) {
            return accountVideoScraper.extractSecUid(url);
        }
        Pattern p = Pattern.compile("/user/([\\w-]+)");
        Matcher m = p.matcher(url);
        return m.find() ? m.group(1) : null;
    }

    private String tryExtractAuthorFromVideo(String videoUrl, Long cookieOwnerId) {
        if (accountVideoScraper == null || !accountVideoScraper.isAvailable()) {
            log.debug("Playwright 不可用，无法从视频页提取作者");
            return null;
        }
        try {
            return accountVideoScraper.extractAuthorUrlFromVideoPage(videoUrl, cookieOwnerId);
        } catch (Exception e) {
            log.warn("从视频页提取作者失败: {}", e.getMessage());
            return null;
        }
    }

    private String tryExtractAccountNameFromVideo(String videoUrl, Long cookieOwnerId) {
        if (accountVideoScraper == null || !accountVideoScraper.isAvailable()) {
            return null;
        }
        try {
            return accountVideoScraper.extractAuthorNameFromVideoPage(videoUrl, cookieOwnerId);
        } catch (Exception e) {
            return null;
        }
    }

    private String trySearchDouyinAccount(String douyinId, Long cookieOwnerId) {
        if (accountVideoScraper == null || !accountVideoScraper.isAvailable()) {
            log.debug("Playwright 不可用，无法搜索抖音号");
            return null;
        }
        try {
            return accountVideoScraper.searchDouyinAccount(douyinId, cookieOwnerId);
        } catch (Exception e) {
            log.warn("搜索抖音号失败: {}", e.getMessage());
            return null;
        }
    }
}
