package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.*;
import cn.gaifan.douyinOperations.module.ai.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiServiceImpl implements cn.gaifan.douyinOperations.module.ai.service.AiService {

    @Resource private AiModelRepository aiModelRepository;
    @Resource private AiGenerationTaskRepository taskRepository;
    @Resource private AiPromptTemplateRepository promptTemplateRepository;
    @Resource private cn.gaifan.douyinOperations.module.ai.repository.AiKnowledgeBaseRepository knowledgeBaseRepository;

    // ==================== 模型管理 ====================

    public List<Map<String, Object>> listModels(Integer status) {
        List<AiModel> models = status != null
                ? aiModelRepository.findByStatusAndDeleted(status, 0)
                : aiModelRepository.findByStatusAndDeleted(1, 0);
        return models.stream().map(this::modelToMap).collect(Collectors.toList());
    }

    public Map<String, Object> getModelById(Long id) {
        return modelToMap(aiModelRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "AI 模型不存在")));
    }

    @Transactional(rollbackFor = Exception.class)
    public long saveModel(Long id, String modelName, String modelProvider, String modelVersion,
                          String apiKey, Integer maxTokens, Double temperature, Integer status) {
        AiModel entity;
        if (id != null && id > 0) {
            entity = aiModelRepository.findByIdAndDeleted(id, 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "AI 模型不存在"));
        } else {
            entity = new AiModel();
        }
        entity.setModelName(modelName);
        entity.setModelProvider(modelProvider);
        entity.setModelVersion(modelVersion);
        if (apiKey != null) entity.setApiKey(apiKey);
        if (maxTokens != null) entity.setMaxTokens(maxTokens);
        if (temperature != null) entity.setTemperature(new java.math.BigDecimal(temperature.toString()));
        if (status != null) entity.setStatus(status);
        return aiModelRepository.save(entity).getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteModel(Long id) {
        AiModel entity = aiModelRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "AI 模型不存在"));
        entity.setDeleted(1);
        aiModelRepository.save(entity);
    }

    // ==================== 生成任务 ====================

    public PageResultVO<Map<String, Object>> searchTasks(Long userId, String taskType, Integer taskStatus, int page, int rows) {
        Pageable pageable = PageRequest.of(page, rows, Sort.by(Sort.Direction.DESC, "createTime"));
        Specification<AiGenerationTask> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            predicates.add(cb.equal(root.get("userId"), userId));
            if (taskType != null && !taskType.isBlank()) predicates.add(cb.equal(root.get("taskType"), taskType));
            if (taskStatus != null) predicates.add(cb.equal(root.get("taskStatus"), taskStatus));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<AiGenerationTask> p = taskRepository.findAll(spec, pageable);
        return PageResultVO.of(p.getTotalElements(),
                p.getContent().stream().map(this::taskToMap).collect(Collectors.toList()), page, rows);
    }

    public Map<String, Object> getTaskById(Long id) {
        return taskToMap(taskRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "任务不存在")));
    }

    @Transactional(rollbackFor = Exception.class)
    public long createTask(Long userId, String taskType, String inputContent, String prompt, String modelUsed) {
        AiGenerationTask task = new AiGenerationTask();
        task.setUserId(userId);
        task.setTaskType(taskType);
        task.setInputContent(inputContent);
        task.setPrompt(prompt);
        task.setModelUsed(modelUsed);
        task.setTaskStatus(0);
        return taskRepository.save(task).getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long id, String output, Long tokens) {
        taskRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "任务不存在"));
        taskRepository.updateResult(id, 2, output, tokens != null ? tokens : 0L);
    }

    // ==================== Prompt 模板 ====================

    public List<Map<String, Object>> listPromptTemplates(Long userId, String category) {
        List<AiPromptTemplate> templates = promptTemplateRepository.findByUserIdAndStatusAndDeleted(userId, 1, 0);
        if (category != null && !category.isBlank()) {
            templates = templates.stream().filter(t -> category.equals(t.getCategory())).collect(Collectors.toList());
        }
        return templates.stream().map(this::templateToMap).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public long savePromptTemplate(Long userId, Long id, String templateName, String templateContent,
                                   String category, String variables, Integer status) {
        AiPromptTemplate entity;
        if (id != null && id > 0) {
            entity = promptTemplateRepository.findByIdAndDeleted(id, 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "模板不存在"));
        } else {
            entity = new AiPromptTemplate();
            entity.setUserId(userId);
        }
        entity.setTemplateName(templateName);
        entity.setTemplateContent(templateContent);
        if (category != null) entity.setCategory(category);
        if (variables != null) entity.setVariables(variables);
        if (status != null) entity.setStatus(status);
        return promptTemplateRepository.save(entity).getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deletePromptTemplate(Long id) {
        AiPromptTemplate entity = promptTemplateRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "模板不存在"));
        entity.setDeleted(1);
        promptTemplateRepository.save(entity);
    }

    // ==================== toMap ====================

    private Map<String, Object> modelToMap(AiModel e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId()); m.put("modelName", e.getModelName()); m.put("modelProvider", e.getModelProvider());
        m.put("modelVersion", e.getModelVersion()); m.put("maxTokens", e.getMaxTokens());
        m.put("temperature", e.getTemperature()); m.put("status", e.getStatus());
        m.put("costPer1kTokens", e.getCostPer1kTokens()); m.put("quotaLimit", e.getQuotaLimit());
        m.put("quotaUsed", e.getQuotaUsed()); m.put("createTime", e.getCreateTime());
        return m;
    }

    private Map<String, Object> taskToMap(AiGenerationTask e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId()); m.put("userId", e.getUserId()); m.put("taskType", e.getTaskType());
        m.put("inputContent", e.getInputContent()); m.put("prompt", e.getPrompt());
        m.put("modelUsed", e.getModelUsed()); m.put("outputContent", e.getOutputContent());
        m.put("tokensUsed", e.getTokensUsed()); m.put("taskStatus", e.getTaskStatus());
        m.put("errorMsg", e.getErrorMsg()); m.put("createTime", e.getCreateTime());
        return m;
    }

    private Map<String, Object> templateToMap(AiPromptTemplate e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId()); m.put("userId", e.getUserId()); m.put("templateName", e.getTemplateName());
        m.put("templateContent", e.getTemplateContent()); m.put("category", e.getCategory());
        m.put("variables", e.getVariables()); m.put("status", e.getStatus());
        m.put("createTime", e.getCreateTime());
        return m;
    }

    // ==================== 知识库 ====================

    public List<Map<String, Object>> listKnowledgeBases(Long userId) {
        return knowledgeBaseRepository.findByUserIdAndDeletedOrderByCreateTimeDesc(userId, 0)
                .stream().map(this::kbToMap).collect(Collectors.toList());
    }

    public Map<String, Object> getKnowledgeBaseById(Long id) {
        return kbToMap(knowledgeBaseRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "知识库不存在")));
    }

    @Transactional(rollbackFor = Exception.class)
    public long saveKnowledgeBase(Long userId, Long id, String kbName, String description, String embeddingModel) {
        cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase entity;
        if (id != null && id > 0) {
            entity = knowledgeBaseRepository.findByIdAndDeleted(id, 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "知识库不存在"));
        } else {
            entity = new cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase();
            entity.setUserId(userId);
        }
        if (kbName != null) entity.setKbName(kbName);
        if (description != null) entity.setDescription(description);
        if (embeddingModel != null) entity.setEmbeddingModel(embeddingModel);
        return knowledgeBaseRepository.save(entity).getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeBase(Long id) {
        cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase entity =
                knowledgeBaseRepository.findByIdAndDeleted(id, 0)
                        .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "知识库不存在"));
        entity.setDeleted(1);
        knowledgeBaseRepository.save(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateKnowledgeBaseStatus(Long id, Integer status) {
        knowledgeBaseRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "知识库不存在"));
        knowledgeBaseRepository.updateStatus(id, status);
    }

    private Map<String, Object> kbToMap(cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId()); m.put("userId", e.getUserId()); m.put("kbName", e.getKbName());
        m.put("description", e.getDescription()); m.put("totalDocuments", e.getTotalDocuments());
        m.put("totalTokens", e.getTotalTokens()); m.put("embeddingModel", e.getEmbeddingModel());
        m.put("status", e.getStatus()); m.put("createTime", e.getCreateTime());
        return m;
    }
}
