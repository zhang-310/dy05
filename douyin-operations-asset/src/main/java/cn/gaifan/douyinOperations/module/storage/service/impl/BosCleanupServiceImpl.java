package cn.gaifan.douyinOperations.module.storage.service.impl;

import cn.gaifan.douyinOperations.contract.asset.SvProjectBosCleanupSource;
import cn.gaifan.douyinOperations.contract.asset.SvProjectStorageCleanupRow;
import cn.gaifan.douyinOperations.module.storage.service.BosCleanupService;
import cn.gaifan.douyinOperations.module.storage.service.BosStorageService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.util.List; 

/**
 * BOS 僵尸文件清理：删除超过 N 天未完成项目的素材
 */
@Slf4j
@Service
public class BosCleanupServiceImpl implements BosCleanupService {

    @Resource
    private SvProjectBosCleanupSource svProjectBosCleanupSource;
    @Resource
    private BosStorageService bosStorageService;

    @Override
    public int cleanupAbandonedProjectFiles(int daysAbandoned) {
        if (!bosStorageService.isConfigured()) return 0;
        List<SvProjectStorageCleanupRow> abandoned = svProjectBosCleanupSource.findAbandonedProjects(daysAbandoned);
        int deleted = 0;
        for (SvProjectStorageCleanupRow p : abandoned) {
            try {
                String dateStr = p.createTime().atZone(ZoneId.systemDefault()).toLocalDate().toString();
                String prefix = p.ownerId() + "/" + dateStr + "/" + p.projectId() + "/";
                var keys = bosStorageService.listAllObjectKeys(prefix);
                for (String key : (Iterable<String>) keys) {
                    if (StringUtils.hasText(key)) {
                        try {
                            bosStorageService.deleteObject(key, p.ownerId());
                            deleted++;
                        } catch (Exception e) {
                            log.warn("删除 BOS 文件失败 key={}", key, e);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("清理项目 {} 素材失败", p.projectId(), e);
            }
        }
        if (deleted > 0) {
            log.info("BOS 僵尸文件清理完成: 删除 {} 个文件, 涉及 {} 个项目", deleted, abandoned.size());
        }
        return deleted;
    }
}
