package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.storage.service.BosStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 爆款视频 BOS 上传器：视频文件、场景关键帧、封面上传到百度 BOS。
 * <p>
 * 复用 {@link BosStorageService}，BOS Key 格式：{ownerId}/viral/{viralId}/...
 * <p>
 * BOS 未配置时所有方法安全跳过（返回 null / 空列表）。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.viral-analysis.bos-upload-enabled", havingValue = "true", matchIfMissing = true)
public class ViralBosUploader {

    @Autowired(required = false)
    private BosStorageService bosStorageService;

    /**
     * BOS 上传结果
     */
    public record BosUploadResult(String bosKey, String cdnUrl) {}

    /**
     * 上传视频文件到 BOS。
     *
     * @param ownerId  用户 ID
     * @param viralId  爆款视频 ID
     * @param localPath 本地视频文件路径
     * @return 上传结果；BOS 未配置或文件不存在返回 null
     */
    public BosUploadResult uploadVideo(Long ownerId, Long viralId, String localPath) {
        if (!isAvailable()) return null;

        Path path = Path.of(localPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            log.warn("[BOS上传] 视频文件不存在或不是普通文件: {}", path);
            return null;
        }

        try {
            String bosKey = String.format("%d/viral/%d/video.mp4", ownerId, viralId);
            byte[] data = Files.readAllBytes(path);
            String url = withUploadRetries("视频", viralId, () -> bosStorageService.uploadBytes(bosKey, data, "video/mp4"));
            if (url == null) {
                return null;
            }
            log.info("[BOS上传] 视频已上传 key={} size={}KB", bosKey, data.length / 1024);
            return new BosUploadResult(bosKey, url);
        } catch (Exception e) {
            log.warn("[BOS上传] 视频上传失败 viralId={}: {}", viralId, e.getMessage());
            return null;
        }
    }

    /**
     * 上传场景关键帧到 BOS。
     *
     * @param ownerId  用户 ID
     * @param viralId  爆款视频 ID
     * @param framePaths 本地关键帧文件路径列表
     * @return 上传结果列表
     */
    public List<BosUploadResult> uploadKeyframes(Long ownerId, Long viralId, List<String> framePaths) {
        if (!isAvailable() || framePaths == null || framePaths.isEmpty()) {
            return List.of();
        }

        List<BosUploadResult> results = new ArrayList<>();
        for (int i = 0; i < framePaths.size(); i++) {
            try {
                Path path = Path.of(framePaths.get(i));
                if (!Files.exists(path)) continue;

                String bosKey = String.format("%d/viral/%d/kf_%d.jpg", ownerId, viralId, i);
                byte[] data = Files.readAllBytes(path);
                String url = withUploadRetries("关键帧" + i, viralId, () -> bosStorageService.uploadBytes(bosKey, data, "image/jpeg"));
                if (url != null) {
                    results.add(new BosUploadResult(bosKey, url));
                }
            } catch (Exception e) {
                log.warn("[BOS上传] 关键帧 {} 上传失败: {}", i, e.getMessage());
            }
        }
        log.info("[BOS上传] 关键帧上传完成 viralId={} 成功={}/{}", viralId, results.size(), framePaths.size());
        return results;
    }

    /**
     * 从 URL 拉取封面并上传到 BOS。
     *
     * @param ownerId  用户 ID
     * @param viralId  爆款视频 ID
     * @param coverUrl 封面图片 URL
     * @return 上传结果；失败返回 null
     */
    public BosUploadResult uploadCover(Long ownerId, Long viralId, String coverUrl) {
        if (!isAvailable() || coverUrl == null || coverUrl.isBlank()) {
            return null;
        }

        try {
            String bosKey = String.format("%d/viral/%d/cover.jpg", ownerId, viralId);
            String url = withUploadRetries("封面", viralId, () -> bosStorageService.putObjectFromUrl(bosKey, coverUrl));
            if (url == null) {
                return null;
            }
            log.info("[BOS上传] 封面已上传 key={}", bosKey);
            return new BosUploadResult(bosKey, url);
        } catch (Exception e) {
            log.warn("[BOS上传] 封面上传失败 viralId={}: {}", viralId, e.getMessage());
            return null;
        }
    }

    /** 网络抖动时最多重试 3 次，间隔递增。 */
    private String withUploadRetries(String label, Long viralId, Supplier<String> upload) {
        int max = 3;
        long sleepMs = 400;
        Exception last = null;
        for (int attempt = 1; attempt <= max; attempt++) {
            try {
                String url = upload.get();
                if (url != null && !url.isBlank()) {
                    return url;
                }
                log.warn("[BOS上传] {} 第{}次返回空 viralId={}", label, attempt, viralId);
            } catch (Exception e) {
                last = e;
                log.warn("[BOS上传] {} 第{}次失败 viralId={}: {}", label, attempt, viralId, e.getMessage());
            }
            if (attempt < max) {
                try {
                    Thread.sleep(sleepMs * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        if (last != null) {
            log.debug("[BOS上传] {} 放弃 viralId={}: {}", label, viralId, last.getMessage());
        }
        return null;
    }

    private boolean isAvailable() {
        return bosStorageService != null && bosStorageService.isConfigured();
    }
}
