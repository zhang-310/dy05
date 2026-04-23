package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.config.service.ConfigService;
import cn.gaifan.douyinOperations.module.shortvideo.service.ContentAuditService;
import cn.gaifan.douyinOperations.module.shortvideo.service.client.BaiduContentAuditClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 内容审核服务实现。
 * 未配置或未接入第三方时统一返回通过，不阻断发布流程。
 * 接入方式：在系统配置中配置 ai.content-audit.app-id / api-key / api-secret 后，
 * 可在此处调用百度云内容安全 API（图像/视频/文本审核），isConfigured() 为 true 时走真实审核。
 *
 * 配置项（可配置在 sys_config 或环境变量）：
 * - ai.content-audit.app-id: 百度云应用 ID (或环境变量 BAIDU_CONTENT_AUDIT_APP_ID)
 * - ai.content-audit.api-key: API 密钥 (或环境变量 BAIDU_CONTENT_AUDIT_API_KEY)
 * - ai.content-audit.secret-key: Secret 密钥 (或环境变量 BAIDU_CONTENT_AUDIT_SECRET_KEY)
 */
@Slf4j
@Service
public class ContentAuditServiceImpl implements ContentAuditService {

    @Resource
    private ConfigService configService;

    private BaiduContentAuditClient client;

    /**
     * 获取或初始化百度云审核客户端
     */
    private BaiduContentAuditClient getClient() {
        if (!isConfigured()) {
            return null;
        }
        if (client != null) {
            return client;
        }

        // 初始化客户端
        String appId = getConfigValue("ai.content-audit.app-id");
        String apiKey = getConfigValue("ai.content-audit.api-key");
        String secretKey = getConfigValue("ai.content-audit.secret-key");

        if (StringUtils.hasText(appId) && StringUtils.hasText(apiKey) && StringUtils.hasText(secretKey)) {
            client = new BaiduContentAuditClient(appId, apiKey, secretKey);
            log.info("百度云内容审核客户端初始化成功");
        }

        return client;
    }

    /**
     * 读取配置值（优先读配置表，再读环境变量）
     */
    private String getConfigValue(String key) {
        String value = null;
        if (configService != null) {
            value = configService.getRawValueByKey(key);
        }
        if (!StringUtils.hasText(value)) {
            // 转换配置键为环境变量名（例如 ai.content-audit.app-id -> BAIDU_CONTENT_AUDIT_APP_ID）
            String envKey = key.replaceAll("^ai\\.content-audit\\.", "BAIDU_CONTENT_AUDIT_")
                    .replaceAll("\\.", "_")
                    .toUpperCase();
            value = System.getenv(envKey);
        }
        return value;
    }

    @Override
    public AuditResult auditImage(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return AuditResult.pass();
        }

        BaiduContentAuditClient auditClient = getClient();
        if (auditClient == null) {
            return AuditResult.pass();
        }

        try {
            BaiduContentAuditClient.ContentAuditResult result = auditClient.auditImage(imageUrl);
            return new AuditResult(result.passed, result.conclusion, result.issues, result.suggestions);
        } catch (Exception e) {
            log.error("图像审核异常", e);
            return AuditResult.pass();
        }
    }

    @Override
    public AuditResult auditVideo(String videoUrl) {
        if (!StringUtils.hasText(videoUrl)) {
            return AuditResult.pass();
        }

        BaiduContentAuditClient auditClient = getClient();
        if (auditClient == null) {
            return AuditResult.pass();
        }

        try {
            BaiduContentAuditClient.ContentAuditResult result = auditClient.auditVideo(videoUrl);
            return new AuditResult(result.passed, result.conclusion, result.issues, result.suggestions);
        } catch (Exception e) {
            log.error("视频审核异常", e);
            return AuditResult.pass();
        }
    }

    @Override
    public AuditResult auditText(String text) {
        if (!StringUtils.hasText(text)) {
            return AuditResult.pass();
        }

        BaiduContentAuditClient auditClient = getClient();
        if (auditClient == null) {
            return AuditResult.pass();
        }

        try {
            BaiduContentAuditClient.ContentAuditResult result = auditClient.auditText(text);
            return new AuditResult(result.passed, result.conclusion, result.issues, result.suggestions);
        } catch (Exception e) {
            log.error("文本审核异常", e);
            return AuditResult.pass();
        }
    }

    @Override
    public boolean isConfigured() {
        String appId = getConfigValue("ai.content-audit.app-id");
        String apiKey = getConfigValue("ai.content-audit.api-key");
        String secretKey = getConfigValue("ai.content-audit.secret-key");
        return StringUtils.hasText(appId) && StringUtils.hasText(apiKey) && StringUtils.hasText(secretKey);
    }
}
