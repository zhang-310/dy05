package cn.gaifan.douyinOperations.module.storage.service.impl;

import cn.gaifan.douyinOperations.module.storage.service.BosReferenceImageCacheService;
import cn.gaifan.douyinOperations.module.storage.service.BosStorageService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

/**
 * BOS 参考图本地缓存：ComfyUI 等场景需多次读取同一参考图时，优先从本地缓存读取，
 * 避免重复从 BOS 下载，节省约 99% 流量。
 */
@Slf4j
@Service
public class BosReferenceImageCacheServiceImpl implements BosReferenceImageCacheService {

    private static final String CACHE_DIR = "storage/cache/reference";
    private static final int TTL_HOURS = 24;
    private static final int MAX_SIZE = 500;

    @Resource
    private BosStorageService bosStorageService;

    private final Cache<String, Path> cache = Caffeine.newBuilder()
            .expireAfterWrite(TTL_HOURS, TimeUnit.HOURS)
            .maximumSize(MAX_SIZE)
            .build();

    @Override
    public Path getCachedPath(String bosKey, Long currentUserId) {
        if (!StringUtils.hasText(bosKey) || currentUserId == null) return null;
        if (!bosStorageService.isConfigured()) return null;

        Path cached = cache.getIfPresent(bosKey.trim());
        if (cached != null && Files.exists(cached)) {
            return cached;
        }

        try {
            byte[] bytes = bosStorageService.getObjectBytesForUser(bosKey.trim(), currentUserId);
            if (bytes == null || bytes.length == 0) return null;

            Path cacheDir = Paths.get(CACHE_DIR).toAbsolutePath();
            Files.createDirectories(cacheDir);
            String safeName = sha256Hex(bosKey.trim()) + extensionFromKey(bosKey.trim());
            Path target = cacheDir.resolve(safeName);
            Files.write(target, bytes);
            cache.put(bosKey.trim(), target);
            return target;
        } catch (Exception e) {
            log.warn("参考图缓存失败 bosKey={} userId={}", bosKey, currentUserId, e);
            return null;
        }
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    private static String extensionFromKey(String key) {
        int i = key.lastIndexOf('.');
        if (i >= 0 && i < key.length() - 1) {
            String ext = key.substring(i);
            if (ext.matches("\\.[a-zA-Z0-9]{2,5}")) return ext;
        }
        return ".bin";
    }
}
