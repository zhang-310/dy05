package cn.gaifan.douyinOperations.module.auth.util;

import cn.gaifan.douyinOperations.module.auth.entity.AuthUser;
import cn.gaifan.douyinOperations.module.auth.repository.AuthUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.Optional;

/**
 * 开发/测试环境：若 admin 用户密码仍为占位哈希，则更新为 admin123 的 BCrypt 密文，便于本地登录
 * 仅当 spring.profiles.active 包含 dev 或 local 时执行
 */
@Component
public class DevDataInit implements ApplicationRunner {

    @Resource
    private AuthUserRepository authUserRepository;
    @Resource
    private PasswordEncoder passwordEncoder;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    @Override
    public void run(ApplicationArguments args) {
        if (activeProfiles == null || (!activeProfiles.contains("dev") && !activeProfiles.contains("local"))) {
            return;
        }
        Optional<AuthUser> adminOpt = authUserRepository.findByUsernameAndDeleted("admin", 0);
        if (!adminOpt.isPresent()) return;
        AuthUser admin = adminOpt.get();
        String hash = admin.getPasswordHash();
        if (hash == null || hash.length() < 50) return;
        try {
            if (passwordEncoder.matches("admin123", hash)) return;
        } catch (Exception ignored) {
            // 占位或非法 BCrypt，需要更新
        }
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        authUserRepository.save(admin);
    }
}
