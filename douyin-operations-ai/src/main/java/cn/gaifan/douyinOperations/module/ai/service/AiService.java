package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;

import java.util.List;
import java.util.Map;

public interface AiService {

    List<Map<String, Object>> listModels(Integer status);

    Map<String, Object> getModelById(Long id);

    long saveModel(Long id, String modelName, String modelProvider, String modelVersion,
                   String apiKey, Integer maxTokens, Double temperature, Integer status);

    void deleteModel(Long id);

    PageResultVO<Map<String, Object>> searchTasks(Long userId, String taskType, Integer taskStatus, int page, int rows);

    Map<String, Object> getTaskById(Long id);

    long createTask(Long userId, String taskType, String inputContent, String prompt, String modelUsed);

    void completeTask(Long id, String output, Long tokens);

    List<Map<String, Object>> listPromptTemplates(Long userId, String category);

    long savePromptTemplate(Long userId, Long id, String templateName, String templateContent,
                            String category, String variables, Integer status);

    void deletePromptTemplate(Long id);

    // ==================== 知识库 ====================

    List<Map<String, Object>> listKnowledgeBases(Long userId);

    Map<String, Object> getKnowledgeBaseById(Long id);

    long saveKnowledgeBase(Long userId, Long id, String kbName, String description, String embeddingModel);

    void deleteKnowledgeBase(Long id);

    void updateKnowledgeBaseStatus(Long id, Integer status);
}
