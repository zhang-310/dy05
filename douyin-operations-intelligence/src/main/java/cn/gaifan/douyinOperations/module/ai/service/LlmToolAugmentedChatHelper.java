package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.tool.LlmToolContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 在「支持 tools 的模型」上优先走 {@link LlmToolOrchestratorService}，否则或失败时回退 {@link LlmClient#chatWithFallback}。
 * 用于商品话术精修、直播成篇优化等单轮 system+user 场景。
 */
@Component
public class LlmToolAugmentedChatHelper {

    private static final Logger log = LoggerFactory.getLogger(LlmToolAugmentedChatHelper.class);

    private static final String SCENE_SUFFIX = """

（你可调用工具 kb_rag_search 检索用户知识库；需要事实、话术范例或资料时先检索再作答。）
""";

    /** Agent / 外部在写入 system 后拼接，与单轮场景 suffix 一致 */
    public static String appendKnowledgeToolHint(String systemContent) {
        return (systemContent != null ? systemContent : "") + SCENE_SUFFIX;
    }

    @Resource
    private LlmToolOrchestratorService orchestrator;
    @Resource
    private LlmClient llmClient;

    @Value("${app.ai.llm-tools.enabled:true}")
    private boolean toolsEnabled;

    @Value("${app.ai.llm-tools.product-script:true}")
    private boolean productScriptScene;

    @Value("${app.ai.llm-tools.live-script-quality:true}")
    private boolean liveScriptQualityScene;

    public boolean isProductScriptToolsEnabled() {
        return toolsEnabled && productScriptScene && orchestrator.hasRegisteredTools();
    }

    public boolean isLiveScriptQualityToolsEnabled() {
        return toolsEnabled && liveScriptQualityScene && orchestrator.hasRegisteredTools();
    }

    /**
     * 单轮对话：system + user，带模型降级链
     */
    public LlmClient.LlmResponse chatWithFallbackAndOptionalTools(
            List<AiModel> models,
            String system,
            String user,
            LlmToolContext ctx,
            boolean sceneAllowsTools
    ) {
        if (models == null || models.isEmpty()) {
            return new LlmClient.LlmResponse(null, 0, false, "无可用模型");
        }
        if (!sceneAllowsTools || !toolsEnabled || !orchestrator.hasRegisteredTools()) {
            return llmClient.chatWithFallback(models, system, user);
        }

        String systemAugmented = system + SCENE_SUFFIX;
        List<Map<String, Object>> base = new ArrayList<>();
        LinkedHashMap<String, Object> sys = new LinkedHashMap<>();
        sys.put("role", "system");
        sys.put("content", systemAugmented);
        base.add(sys);
        LinkedHashMap<String, Object> usr = new LinkedHashMap<>();
        usr.put("role", "user");
        usr.put("content", user != null ? user : "");
        base.add(usr);

        for (AiModel m : models) {
            if (m == null) continue;
            if (!LlmToolOrchestratorService.modelSupportsTools(m)) {
                LlmClient.LlmResponse r = llmClient.chat(m, system, user);
                if (r.success()) return r;
                continue;
            }
            LlmToolOrchestratorService.ToolOrchestrationResult tr = orchestrator.run(m, new ArrayList<>(base), ctx);
            if (tr.success() && tr.finalContent() != null && !tr.finalContent().isBlank()) {
                return new LlmClient.LlmResponse(tr.finalContent(), tr.totalTokens(), true, null);
            }
            if (!tr.success()) {
                log.debug("工具编排未成功 model={} err={}，尝试下一模型或降级", m.getModelVersion(), tr.errorMsg());
            }
        }
        return llmClient.chatWithFallback(models, system, user);
    }
}
