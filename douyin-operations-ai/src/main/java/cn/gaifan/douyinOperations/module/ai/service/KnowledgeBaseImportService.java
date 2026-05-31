package cn.gaifan.douyinOperations.module.ai.service;

import java.util.List;
import java.util.Map;

/**
 * 知识库本地路径导入服务
 */
public interface KnowledgeBaseImportService {

    /**
     * 从本地路径导入文档到知识库
     *
     * @param sourcePath  本地目录路径
     * @param kbId        知识库 ID，null 时按 kbName 查找
     * @param kbName      知识库名称
     * @param autoClassify 自动识别抖音/技术
     * @param userId      操作用户 ID
     * @param progress    可选，用于异步导入时报告进度
     * @return 导入结果摘要
     */
    ImportResult importFromPath(String sourcePath, Long kbId, String kbName, boolean autoClassify, Long userId, ImportProgress progress);

    /**
     * 增量导入：仅处理 sourcePath 下 lastModified 晚于上次导入时间的文件，完成后更新 checkpoint
     */
    ImportResult importIncremental(String sourcePath, Long kbId, Long userId, ImportProgress progress);

    /** 兼容旧调用 */
    default ImportResult importFromPath(String sourcePath, Long kbId, String kbName, boolean autoClassify, Long userId) {
        return importFromPath(sourcePath, kbId, kbName, autoClassify, userId, null);
    }

    record ImportResult(
            int total, int success, int failed, int skipped,
            Map<String, Integer> byKb,
            List<String> errors,
            String hint
    ) {
        public ImportResult(int total, int success, int failed, Map<String, Integer> byKb, List<String> errors) {
            this(total, success, failed, 0, byKb, errors, null);
        }
    }
}
