package cn.gaifan.douyinOperations.module.ai.util;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;

import java.util.*;

/**
 * 进化/重排共用：模型降级顺序为 DeepSeek 思考 → DeepSeek Chat → Claude，
 * 避免重排使用全表模型导致 MiniMax/GLM 等刷屏。
 */
public final class EvolveModelOrderUtil {

    private EvolveModelOrderUtil() {}

    /**
     * 构建有序模型链：DeepSeek 思考(reasoner) → DeepSeek Chat → Claude → 火山方舟；
     * 若均未匹配则回退 ollama → deepseek → volcengine → 580ai。
     */
    public static List<AiModel> buildOrderedChain(List<AiModel> all) {
        if (all == null || all.isEmpty()) return Collections.emptyList();
        List<AiModel> deepseek = all.stream()
                .filter(m -> "deepseek".equalsIgnoreCase(safeProvider(m)))
                .toList();
        List<AiModel> result = new ArrayList<>();
        Set<Long> used = new HashSet<>();

        deepseek.stream().filter(EvolveModelOrderUtil::isDeepseekThinkingModel).findFirst().ifPresent(m -> {
            result.add(m);
            used.add(m.getId());
        });

        Optional<AiModel> chatDs = deepseek.stream()
                .filter(m -> !used.contains(m.getId()))
                .filter(m -> !isDeepseekThinkingModel(m))
                .filter(m -> modelVerLower(m).contains("chat"))
                .findFirst();
        if (chatDs.isPresent()) {
            result.add(chatDs.get());
            used.add(chatDs.get().getId());
        } else {
            deepseek.stream()
                    .filter(m -> !used.contains(m.getId()))
                    .filter(m -> !isDeepseekThinkingModel(m))
                    .findFirst()
                    .ifPresent(m -> {
                        result.add(m);
                        used.add(m.getId());
                    });
        }

        all.stream()
                .filter(m -> !used.contains(m.getId()))
                .filter(EvolveModelOrderUtil::isClaudeFamilyModel)
                .findFirst()
                .ifPresent(m -> {
                    result.add(m);
                    used.add(m.getId());
                });

        all.stream()
                .filter(m -> !used.contains(m.getId()))
                .filter(EvolveModelOrderUtil::isVolcengineArkModel)
                .findFirst()
                .ifPresent(m -> {
                    result.add(m);
                    used.add(m.getId());
                });

        if (!result.isEmpty()) return result;

        List<AiModel> ollama = all.stream().filter(m -> "ollama".equalsIgnoreCase(safeProvider(m))).toList();
        List<AiModel> ai580 = all.stream().filter(m -> "580ai".equalsIgnoreCase(safeProvider(m))).toList();
        List<AiModel> volc = all.stream().filter(EvolveModelOrderUtil::isVolcengineArkModel).toList();
        List<AiModel> legacy = new ArrayList<>();
        if (!ollama.isEmpty()) legacy.add(ollama.get(0));
        if (!deepseek.isEmpty()) legacy.add(deepseek.get(0));
        if (!volc.isEmpty()) legacy.add(volc.get(0));
        if (!ai580.isEmpty()) legacy.add(ai580.get(0));
        if (ollama.size() > 1) legacy.add(ollama.get(1));
        return legacy;
    }

    private static String safeProvider(AiModel m) {
        return m.getModelProvider() != null ? m.getModelProvider() : "";
    }

    private static String modelVerLower(AiModel m) {
        return m.getModelVersion() != null ? m.getModelVersion().toLowerCase(Locale.ROOT) : "";
    }

    public static boolean isDeepseekThinkingModel(AiModel m) {
        if (!"deepseek".equalsIgnoreCase(safeProvider(m))) return false;
        String v = modelVerLower(m);
        return v.contains("reasoner") || v.contains("deepseek-r1") || v.contains("-r1")
                || v.contains("thinking") || "r1".equals(v);
    }

    public static boolean isClaudeFamilyModel(AiModel m) {
        String p = safeProvider(m).toLowerCase(Locale.ROOT);
        if ("anthropic".equals(p)) return true;
        return modelVerLower(m).contains("claude");
    }

    /** 火山方舟 Chat（与 OpenAiCompatibleLlmClient provider 别名一致） */
    public static boolean isVolcengineArkModel(AiModel m) {
        String p = safeProvider(m).toLowerCase(Locale.ROOT);
        return "volcengine".equals(p) || "ark".equals(p) || "volcano-ark".equals(p) || "volcengine-ark".equals(p);
    }
}
