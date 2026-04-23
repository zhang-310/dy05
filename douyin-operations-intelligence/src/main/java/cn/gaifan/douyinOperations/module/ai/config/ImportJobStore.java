package cn.gaifan.douyinOperations.module.ai.config;

import cn.gaifan.douyinOperations.module.ai.service.ImportProgress;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 导入任务进度存储，供轮询
 */
@Component
public class ImportJobStore {
    private final Map<String, ImportProgress> store = new ConcurrentHashMap<>();

    public String createJob() {
        String jobId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        ImportProgress progress = new ImportProgress();
        progress.addLog("任务已创建，后台开始处理...");
        store.put(jobId, progress);
        return jobId;
    }

    public ImportProgress get(String jobId) {
        return store.get(jobId);
    }

    public void remove(String jobId) {
        store.remove(jobId);
    }

    /** 返回 phase=running 的 jobId 列表，供前端恢复进度 */
    public List<String> listActiveJobIds() {
        return store.entrySet().stream()
                .filter(e -> "running".equals(e.getValue().getPhase()))
                .map(Map.Entry::getKey)
                .toList();
    }
}
