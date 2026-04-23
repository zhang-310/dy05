package cn.gaifan.douyinOperations.module.ai.service;

import java.util.List;

/**
 * 进化主题库导入服务（从老系统 JSON/TXT 导入）
 */
public interface EvolveTopicImportService {

    /**
     * 从本地文件导入主题到进化主题池
     *
     * @param sourcePath 本地文件路径（.json 或 .txt）
     * @param kbId       知识库 ID，null 表示全局主题
     * @return 导入结果
     */
    TopicImportResult importFromFile(String sourcePath, Long kbId);

    record TopicImportResult(
            int total,
            int success,
            int skipped,
            List<String> errors,
            String hint  // 成功0条时的提示：如 "均已存在" 或 "解析后为空"
    ) {
        public TopicImportResult(int total, int success, int skipped, List<String> errors) {
            this(total, success, skipped, errors, null);
        }
    }
}
