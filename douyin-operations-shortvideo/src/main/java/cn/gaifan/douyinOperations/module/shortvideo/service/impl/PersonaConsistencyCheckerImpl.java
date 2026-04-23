package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.douyin.entity.DyPersona;
import cn.gaifan.douyinOperations.module.douyin.repository.DyPersonaRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.PersonaConsistencyChecker;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PersonaConsistencyCheckerImpl implements PersonaConsistencyChecker {

    private static final Logger log = LoggerFactory.getLogger(PersonaConsistencyCheckerImpl.class);

    @Resource
    private DyPersonaRepository dyPersonaRepository;

    @Resource
    private LlmClient llmClient;

    @Resource
    private AiModelRepository aiModelRepository;

    /** D-2：追加到 system 提示的可运营片段（如行业禁忌短语） */
    @Value("${app.shortvideo.persona-consistency.system-extra:}")
    private String personaConsistencySystemExtra;

    /** D-2：追加到 user 任务提示的可运营片段（如本账号禁区词、称呼表） */
    @Value("${app.shortvideo.persona-consistency.user-extra:}")
    private String personaConsistencyUserExtra;

    /** D-2：送入人设审核的生成正文最大字符数（0=不截断，与 effect-predict 类似） */
    @Value("${app.shortvideo.persona-consistency.max-generated-chars:12000}")
    private int personaConsistencyMaxGeneratedChars;

    @Override
    public Map<String, Object> checkConsistency(Long personaId, String generatedContent, Long userId) {
        DyPersona persona = dyPersonaRepository.findByIdAndDeleted(personaId, 0).orElse(null);
        if (persona == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "人设不存在");
        }
        if (!persona.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该人设");
        }

        String contentForPrompt = generatedContent != null ? generatedContent : "";
        int cap = personaConsistencyMaxGeneratedChars;
        if (cap > 0 && contentForPrompt.length() > cap) {
            int total = generatedContent != null ? generatedContent.length() : 0;
            contentForPrompt = contentForPrompt.substring(0, cap) + "\n…（已截断，共 " + total + " 字）";
        }

        StringBuilder personaDesc = new StringBuilder();
        personaDesc.append("人设名称: ").append(persona.getPersonaName()).append("\n");
        if (persona.getPersonaType() != null) {
            personaDesc.append("人设类型: ").append(persona.getPersonaType()).append("\n");
        }
        if (persona.getTone() != null) {
            personaDesc.append("语气风格: ").append(persona.getTone()).append("\n");
        }
        if (persona.getTargetAudience() != null) {
            personaDesc.append("目标受众: ").append(persona.getTargetAudience()).append("\n");
        }
        if (persona.getContentStyle() != null) {
            personaDesc.append("内容风格: ").append(persona.getContentStyle()).append("\n");
        }
        if (persona.getDescription() != null) {
            personaDesc.append("描述: ").append(persona.getDescription()).append("\n");
        }

        String systemPrompt = "你是护肤品/彩妆类短视频与直播话术的人设一致性审核专家。请严格按 JSON 输出，勿自动改写正文。\n"
                + "重点检查：称呼与受众是否与人设一致；外观/人设标签是否在正文中被矛盾描述；禁忌词或夸大承诺是否与人设「语气/风格」冲突；"
                + "同一短文本内是否重复堆砌人设关键词而无信息量。"
                + (StringUtils.hasText(personaConsistencySystemExtra) ? "\n" + personaConsistencySystemExtra.trim() : "");
        String prompt = "请评估以下生成内容与人设的一致性：\n\n" +
                "【人设信息】\n" + personaDesc + "\n" +
                "【生成内容】\n" + contentForPrompt + "\n\n" +
                "请输出 JSON（不要包含 markdown 代码块标记），字段说明：\n" +
                "- score：0～100 整数，综合「称呼/受众/语气是否与人设一致」与「外观或禁忌类表述风险」；若称呼或受众明显跑偏、或出现与人设冲突的夸大承诺，应显著低于 60；\n" +
                "- isConsistent：score>=60 为 true，否则 false；\n" +
                "- suggestions：1～5 条可执行修改建议（点出具体用语或句式即可，**不要**输出整篇替稿或自动改写正文）。\n" +
                "示例：{\"score\":72,\"isConsistent\":true,\"suggestions\":[\"…\"]}";
        if (StringUtils.hasText(personaConsistencyUserExtra)) {
            prompt = prompt + "\n\n【运营补充约束】\n" + personaConsistencyUserExtra.trim();
        }

        List<AiModel> models = aiModelRepository.findByStatusAndDeleted(1, 0);
        if (models.isEmpty()) {
            log.warn("[人设一致性] 无可用模型");
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("score", 0);
            fallback.put("isConsistent", false);
            fallback.put("suggestions", List.of("无可用 AI 模型，无法评估"));
            return fallback;
        }

        LlmClient.LlmResponse response = llmClient.chatWithFallback(models, systemPrompt, prompt);
        if (!response.success() || response.content() == null) {
            log.error("[人设一致性] LLM 评估失败: {}", response.errorMsg());
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("score", 0);
            fallback.put("isConsistent", false);
            fallback.put("suggestions", List.of("AI 评估失败: " + response.errorMsg()));
            return fallback;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(response.content().trim()
                            .replaceAll("^```json\\s*", "")
                            .replaceAll("```\\s*$", ""), Map.class);
            log.info("[人设一致性] personaId={} score={}", personaId, result.get("score"));
            return result;
        } catch (Exception e) {
            log.warn("[人设一致性] JSON 解析失败，返回原始内容: {}", e.getMessage());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("score", 50);
            result.put("isConsistent", false);
            result.put("suggestions", List.of(response.content()));
            return result;
        }
    }
}
