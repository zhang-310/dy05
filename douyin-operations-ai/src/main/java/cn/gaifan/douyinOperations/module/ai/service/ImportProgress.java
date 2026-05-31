package cn.gaifan.douyinOperations.module.ai.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 导入任务进度，供异步导入时轮询
 */
public class ImportProgress {
    private final AtomicInteger processed = new AtomicInteger(0);
    private final AtomicInteger success = new AtomicInteger(0);
    private final AtomicInteger failed = new AtomicInteger(0);
    private volatile int total;
    private volatile String phase = "running"; // running | done | error
    private volatile KnowledgeBaseImportService.ImportResult result;
    private final List<String> logs = Collections.synchronizedList(new ArrayList<>());

    public void setTotal(int total) {
        this.total = total;
    }

    public void addLog(String line) {
        logs.add(line);
    }

    public void incrementProcessed() {
        processed.incrementAndGet();
    }

    public void incrementSuccess() {
        success.incrementAndGet();
    }

    public void incrementFailed() {
        failed.incrementAndGet();
    }

    public void done(KnowledgeBaseImportService.ImportResult result) {
        this.phase = "done";
        this.result = result;
    }

    public void error(String message) {
        this.phase = "error";
        this.addLog("错误: " + message);
    }

    public int getProcessed() {
        return processed.get();
    }

    public int getSuccess() {
        return success.get();
    }

    public int getFailed() {
        return failed.get();
    }

    public int getTotal() {
        return total;
    }

    public String getPhase() {
        return phase;
    }

    public KnowledgeBaseImportService.ImportResult getResult() {
        return result;
    }

    public List<String> getLogs() {
        return new ArrayList<>(logs);
    }
}
