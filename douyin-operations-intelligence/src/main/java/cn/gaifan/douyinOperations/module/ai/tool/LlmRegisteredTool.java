package cn.gaifan.douyinOperations.module.ai.tool;

import java.util.Set;

/**
 * 注册到 LlmToolOrchestratorService 的 LLM 可调用工具（OpenAI/方舟 function calling）
 */
public interface LlmRegisteredTool {

    /** function.name */
    String name();

    /** function.description */
    String description();

    /**
     * function.parameters：JSON Schema 对象字符串（type=object, properties, required）
     */
    String parametersJsonSchema();

    /**
     * 执行工具；返回给模型的文本（建议 JSON 或简短自然语言）
     */
    String execute(String argumentsJson, LlmToolContext ctx) throws Exception;

    /**
     * 可选：对输出 JSON 做 Schema 校验（非空时由 {@code LlmToolOrchestratorService} 校验，失败触发重试）
     */
    default String outputJsonSchema() {
        return null;
    }

    /**
     * 是否对「同用户 + 同名 + 同参」的 Tool 结果做 Redis 缓存（T-3）。
     */
    default boolean resultCacheable() {
        return true;
    }

    /**
     * 覆盖全局 TTL（秒）；≤0 表示使用 {@code app.ai.llm-tools.result-cache.ttl-seconds}
     */
    default int resultCacheTtlSeconds() {
        return -1;
    }

    /**
     * 同轮次多 tool call 时：这些工具名必须先于本工具执行（静态 DAG，T-1）。
     * 仅当本轮实际包含对应调用时参与拓扑重排；与 docs 中「T-* 工具编号」不同名。
     */
    default Set<String> prerequisiteToolNames() {
        return Set.of();
    }
}
