package cn.gaifan.douyinOperations.module.ai.service;

import java.util.Map;

/**
 * 文档导入前置依赖检查
 */
public interface ImportRequirementsService {

    /**
     * 检查文档导入所需服务状态。
     * key: milvus | ollama | elasticsearch
     * value: null 表示正常，非 null 表示错误提示
     */
    Map<String, String> checkImportRequirements();
}
