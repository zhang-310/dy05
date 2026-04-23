package cn.gaifan.douyinOperations.module.storage.service.impl;

import cn.gaifan.douyinOperations.module.storage.entity.BosFileMetadata;
import cn.gaifan.douyinOperations.module.storage.repository.BosFileMetadataRepository;
import cn.gaifan.douyinOperations.module.storage.service.BosFileMetadataService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * BOS 文件元数据服务实现
 */
@Service
public class BosFileMetadataServiceImpl implements BosFileMetadataService {

    @Resource
    private BosFileMetadataRepository repository;

    @Override
    public void recordUpload(String bosKey, Long userId, Long taskId, String category, long fileSize, Long shotId, String sourceUrl) {
        if (!StringUtils.hasText(bosKey) || userId == null) return;
        try {
            BosFileMetadata m = new BosFileMetadata();
            m.setBosKey(bosKey.trim());
            m.setUserId(userId);
            m.setTaskId(taskId);
            m.setCategory(StringUtils.hasText(category) ? category : "unknown");
            m.setFileSize(fileSize > 0 ? fileSize : 0L);
            m.setStorageCostMonthly(estimateMonthlyCost(fileSize));
            m.setShotId(shotId);
            m.setSourceUrl(StringUtils.hasText(sourceUrl) ? sourceUrl : null);
            repository.save(m);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void incrementUsageCount(String bosKey) {
        if (!StringUtils.hasText(bosKey)) return;
        try {
            repository.findByBosKeyAndDeleted(bosKey.trim(), 0).ifPresent(m -> {
                m.setUsageCount((m.getUsageCount() != null ? m.getUsageCount() : 0) + 1);
                repository.save(m);
            });
        } catch (Exception ignored) {
        }
    }

    private static BigDecimal estimateMonthlyCost(long fileSizeBytes) {
        if (fileSizeBytes <= 0) return BigDecimal.ZERO;
        double gb = fileSizeBytes / (1024.0 * 1024.0 * 1024.0);
        return BigDecimal.valueOf(gb * 0.12); // ¥0.12/GB/月 标准存储
    }
}
