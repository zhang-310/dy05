package cn.gaifan.douyinOperations.module.script.service;

import java.util.List;
import java.util.Map;

/**
 * 合规词库服务：加载绝对化用语、医疗功效词
 * 供 ComplianceService 使用
 */
public interface ComplianceWordService {

    /**
     * 获取绝对化用语 → 替换建议（可自动修复）
     */
    Map<String, String> getAbsoluteReplacements();

    /**
     * 获取医疗功效词列表（不可自动修复）
     */
    List<String> getMedicalViolations();

    /**
     * 刷新内存缓存（供定时任务或管理接口调用）
     */
    void refresh();

    /**
     * 获取词库是否已从 DB 加载（用于 fallback 判断）
     */
    boolean isLoadedFromDb();
}
