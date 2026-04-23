package cn.gaifan.douyinOperations.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 安全配置验证器
 * 在应用启动时验证关键安全配置
 */
@Component
public class SecurityConfigValidator implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfigValidator.class);

    @Value("${app.token.secret:default_secret_key_change_in_production}")
    private String tokenSecret;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("开始验证安全配置...");

        boolean hasErrors = false;

        // 验证 Token 密钥
        if (!validateTokenSecret()) {
            hasErrors = true;
        }

        // 验证数据库密码（生产环境）
        if ("prod".equals(activeProfile) || "production".equals(activeProfile)) {
            if (!validateProductionConfig()) {
                hasErrors = true;
            }
        }

        if (hasErrors) {
            log.error("=".repeat(80));
            log.error("安全配置验证失败！请修复上述问题后重启应用。");
            log.error("=".repeat(80));
            if ("prod".equals(activeProfile) || "production".equals(activeProfile)) {
                log.error("生产环境检测到安全问题，应用将退出！");
                System.exit(1);
            }
        } else {
            log.info("安全配置验证通过 ✓");
        }
    }

    /**
     * 验证 Token 密钥
     */
    private boolean validateTokenSecret() {
        // 检查是否使用默认密钥
        if (tokenSecret == null || "default_secret_key_change_in_production".equals(tokenSecret)) {
            log.error("❌ 检测到使用默认 Token 密钥！");
            log.error("   请设置环境变量: APP_TOKEN_SECRET=<your_strong_secret_key>");
            log.error("   密钥要求: 至少32个字符，包含大小写字母、数字和特殊字符");
            return false;
        }

        // 检查密钥长度
        if (tokenSecret.length() < 32) {
            log.error("❌ Token 密钥长度不足！当前长度: {}, 要求: 至少32个字符", tokenSecret.length());
            log.error("   请设置更强的密钥: APP_TOKEN_SECRET=<your_strong_secret_key>");
            return false;
        }

        // 检查密钥复杂度
        boolean hasUpper = tokenSecret.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = tokenSecret.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = tokenSecret.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = tokenSecret.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));

        if (!hasUpper || !hasLower || !hasDigit) {
            log.warn("⚠️  Token 密钥复杂度不足！建议包含大小写字母、数字和特殊字符");
            log.warn("   当前: 大写={}, 小写={}, 数字={}, 特殊字符={}", hasUpper, hasLower, hasDigit, hasSpecial);
        }

        log.info("✓ Token 密钥配置正确（长度: {}）", tokenSecret.length());
        return true;
    }

    /**
     * 验证生产环境配置
     */
    private boolean validateProductionConfig() {
        boolean valid = true;

        // 检查是否配置了数据库密码
        String dbPassword = System.getenv("DB_PASSWORD");
        if (dbPassword == null || dbPassword.isBlank() || "postgres".equals(dbPassword)) {
            log.error("❌ 生产环境未配置安全的数据库密码！");
            log.error("   请设置环境变量: DB_PASSWORD=<your_secure_password>");
            valid = false;
        }

        // 检查是否配置了 CORS 白名单
        String corsOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
        if (corsOrigins == null || corsOrigins.contains("*")) {
            log.error("❌ 生产环境 CORS 配置不安全！");
            log.error("   请设置环境变量: CORS_ALLOWED_ORIGINS=https://your-domain.com");
            valid = false;
        }

        return valid;
    }
}
