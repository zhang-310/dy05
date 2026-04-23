package cn.gaifan.douyinOperations.module.tianapi.service;

import java.util.Map;

/**
 * TianAPI 文案素材自动入库服务。
 * 默认按 {@code material-import-calls-per-category} 控制规模；若配置 {@code material-import-api-calls-per-category}（如 10000），则每类按 HTTP 请求次数拉满，对齐天行单接口日配额。
 */
public interface TianApiMaterialImportService {

    /**
     * 执行素材入库：多轮随机拉取 + 去重 + 写入 copy_library
     *
     * @return 统计信息：totalImported, totalSkipped, totalCalls, byCategory
     */
    Map<String, Object> runImport();
}
