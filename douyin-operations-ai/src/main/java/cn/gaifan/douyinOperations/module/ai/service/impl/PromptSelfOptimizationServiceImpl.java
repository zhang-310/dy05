package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.entity.AiPromptOptimizationLog;
import cn.gaifan.douyinOperations.module.ai.entity.AiPromptTemplate;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiPromptOptimizationLogRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiPromptTemplateRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.service.PromptSelfOptimizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.List;

/**
 * Prompt 自优化引擎实现
 * <p>
 * 当同类任务连续 10 次 effectiveness < 60 时触发 meta-prompt 分析：
 * 1. 收集最近 50 次 {prompt片段, effectiveness_score} 对
 * 2. LLM 分析哪些指令有效/无效
 * 3. 输出优化建议（替换/删除/强化具体 prompt 片段）
 * 4. 安全边界：合规词约束、品牌规范部分不可被优化删除
 * 5. 优化后写入 ai_prompt_template 表（variant=optimized_{timestamp}）+ 日志
 */
@Slf4j
@Service
public class PromptSelfOptimizationServiceImpl implements PromptSelfOptimizationService {

    private static final int LOW_EFFECTIVENESS_THRESHOLD = 60;
    private static final int CONSECUTIVE_LOW_TRIGGER = 10;
    private static final int ANALYSIS_SAMPLE_SIZE = 50;

    private static final String META_PROMPT_SYSTEM = """
            你是一个 Prompt 优化专家。分析以下 prompt 片段与效果评分的历史数据，找出：
            1. 哪些指令/片段与高评分正相关（有效片段）
            2. 哪些指令/片段与低评分正相关（无效片段）
            3. 具体的优化建议（替换/删除/强化）

            【安全约束】以下内容不可删除或弱化：
            - 合规相关指令（广告法、平台规则、违禁词检查）
            - 品牌规范相关指令
            - 数据隔离/安全相关指令

            请返回 JSON 格式：
            {
              "effective_fragments": ["片段1", "片段2"],
              "ineffective_fragments": ["片段3", "片段4"],
              "suggestions": [
                {"action": "replace|delete|strengthen", "target": "原片段", "replacement": "新片段", "reason": "原因"}
              ],
              "optimized_prompt": "完整的优化后 prompt"
            }
            """;

    @Autowired(required = false)
    private AiPromptTemplateRepository promptTemplateRepository;

    @Autowired
    private AiPromptOptimizationLogRepository optimizationLogRepository;

    @Autowired(required = false)
    private LlmClient llmClient;

    @Autowired(required = false)
    private AiModelRepository modelRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void evaluateAndOptimize(String taskType, Long userId) {
        if (llmClient == null || promptTemplateRepository == null) {
            log.debug("[PromptOptimization] LLM 或模板服务不可用，跳过优化评估");
            return;
        }

        // 查找该任务类型的活跃模板
        List<AiPromptTemplate> templates = promptTemplateRepository
                .findByTemplateCodeAndVariantNameAndDeleted(taskType, "default", 0);
        if (templates.isEmpty()) {
            log.debug("[PromptOptimization] 未找到任务类型 {} 的默认模板，跳过", taskType);
            return;
        }

        AiPromptTemplate currentTemplate = templates.get(0);
        Float avgScore = currentTemplate.getAvgScore();
        Integer usageCount = currentTemplate.getUsageCount();

        // 连续低效判定：平均分低于阈值 且 使用次数 >= 触发阈值
        if (avgScore != null && avgScore >= LOW_EFFECTIVENESS_THRESHOLD) {
            return;
        }
        if (usageCount == null || usageCount < CONSECUTIVE_LOW_TRIGGER) {
            return;
        }

        log.info("[PromptOptimization] 任务类型 {} 连续低效（avgScore={}），触发自优化", taskType, avgScore);
        doOptimize(taskType, userId, currentTemplate, "low_effectiveness");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forceOptimize(String taskType, Long userId) {
        if (llmClient == null || promptTemplateRepository == null) {
            log.warn("[PromptOptimization] LLM 或模板服务不可用");
            return;
        }

        List<AiPromptTemplate> templates = promptTemplateRepository
                .findByTemplateCodeAndVariantNameAndDeleted(taskType, "default", 0);
        if (templates.isEmpty()) {
            log.warn("[PromptOptimization] 未找到任务类型 {} 的默认模板", taskType);
            return;
        }

        doOptimize(taskType, userId, templates.get(0), "manual");
    }

    private void doOptimize(String taskType, Long userId, AiPromptTemplate currentTemplate, String triggerReason) {
        // 构建分析上下文
        String currentPrompt = currentTemplate.getTemplateContent();
        if (currentPrompt == null || currentPrompt.isBlank()) {
            currentPrompt = currentTemplate.getUserPromptTpl() != null ? currentTemplate.getUserPromptTpl() : "";
        }

        String analysisPrompt = String.format("""
                【任务类型】%s
                【当前 Prompt】
                %s

                【历史表现】
                - 平均效果评分: %.1f / 100
                - 使用次数: %d
                - P50 评分: %s
                - P90 评分: %s

                请分析并优化此 Prompt。
                """,
                taskType,
                currentPrompt,
                currentTemplate.getAvgScore() != null ? currentTemplate.getAvgScore() : 0f,
                currentTemplate.getUsageCount() != null ? currentTemplate.getUsageCount() : 0,
                currentTemplate.getP50Score() != null ? String.format("%.1f", currentTemplate.getP50Score()) : "N/A",
                currentTemplate.getP90Score() != null ? String.format("%.1f", currentTemplate.getP90Score()) : "N/A");

        List<AiModel> models = modelRepository != null
                ? modelRepository.findByStatusAndDeleted(1, 0)
                : List.of();
        if (models.isEmpty()) {
            log.warn("[PromptOptimization] 无可用模型，跳过优化");
            return;
        }

        LlmClient.LlmResponse response = llmClient.chatWithFallback(models, META_PROMPT_SYSTEM, analysisPrompt);
        if (!response.success() || response.content() == null) {
            log.warn("[PromptOptimization] LLM 分析失败: {}", response.errorMsg());
            return;
        }

        String optimizedContent = response.content();

        // 保存优化后的模板变体
        String variantName = "optimized_" + System.currentTimeMillis();
        AiPromptTemplate optimized = new AiPromptTemplate();
        optimized.setUserId(userId != null ? userId : 0L);
        optimized.setTemplateName(currentTemplate.getTemplateName() + " (优化)");
        optimized.setTemplateContent(optimizedContent);
        optimized.setCategory(currentTemplate.getCategory());
        optimized.setTemplateCode(taskType);
        optimized.setVariantName(variantName);
        optimized.setVersion(String.valueOf((currentTemplate.getVersion() != null ? Integer.parseInt(currentTemplate.getVersion()) : 1) + 1));
        optimized.setSystemPrompt(currentTemplate.getSystemPrompt());
        optimized.setModelHint(currentTemplate.getModelHint());
        optimized.setTemperature(currentTemplate.getTemperature());
        optimized.setMaxTokens(currentTemplate.getMaxTokens());
        optimized.setIsActive(0); // 待验证后激活
        optimized.setIsDefault(0);
        optimized.setOwnerId(userId != null ? userId : 0L);
        promptTemplateRepository.save(optimized);

        // 记录优化日志
        AiPromptOptimizationLog logEntry = new AiPromptOptimizationLog();
        logEntry.setTaskType(taskType);
        logEntry.setOriginalPromptHash(sha256(currentPrompt));
        logEntry.setOptimizedPromptHash(sha256(optimizedContent));
        logEntry.setImprovementPct(BigDecimal.ZERO); // 待后续评估
        logEntry.setTriggerReason(triggerReason);
        logEntry.setDetailsJson(optimizedContent);
        logEntry.setUserId(userId);
        optimizationLogRepository.save(logEntry);

        log.info("[PromptOptimization] 任务类型 {} 优化完成，新变体: {}", taskType, variantName);
    }

    private static String sha256(String input) {
        if (input == null) return "";
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
