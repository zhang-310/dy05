package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.module.ai.config.ImportJobStore;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 异步执行导入任务
 */
@Component
public class ImportAsyncRunner {

    @Resource
    private KnowledgeBaseImportService knowledgeBaseImportService;

    @Resource
    private ImportJobStore importJobStore;

    @Async
    public void runImport(String jobId, String sourcePath, Long kbId, String kbName, boolean autoClassify, Long userId) {
        ImportProgress progress = importJobStore.get(jobId);
        if (progress == null) return;
        try {
            knowledgeBaseImportService.importFromPath(sourcePath, kbId, kbName, autoClassify, userId, progress);
        } catch (Exception e) {
            progress.error(e.getMessage());
        }
    }
}
