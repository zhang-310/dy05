package cn.gaifan.douyinOperations.module.ai.util;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * LLM 调用链追踪上下文 — 贯穿 Controller → Service → LlmClient → 日志
 * <p>
 * 使用 MDC 传递 ai.traceId，与请求级 traceId 关联。
 */
public final class LlmTraceContext {

    private LlmTraceContext() {}

    public static final String MDC_KEY = "ai.traceId";
    public static final String MDC_PROVIDER = "ai.provider";
    public static final String MDC_MODEL = "ai.model";

    /**
     * 开始一次 LLM 追踪（在调用前设置）
     * @return 生成的 traceId
     */
    public static String begin(String provider, String model) {
        String traceId = generateTraceId();
        MDC.put(MDC_KEY, traceId);
        if (provider != null) MDC.put(MDC_PROVIDER, provider);
        if (model != null) MDC.put(MDC_MODEL, model);
        return traceId;
    }

    /** 结束追踪（清理 MDC） */
    public static void end() {
        MDC.remove(MDC_KEY);
        MDC.remove(MDC_PROVIDER);
        MDC.remove(MDC_MODEL);
    }

    /** 获取当前追踪 ID */
    public static String current() {
        return MDC.get(MDC_KEY);
    }

    private static String generateTraceId() {
        // 使用请求级 traceId 作为前缀（如果有），否则独立生成
        String reqTraceId = MDC.get("traceId");
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return reqTraceId != null ? reqTraceId + "-llm-" + suffix : "llm-" + suffix;
    }
}
