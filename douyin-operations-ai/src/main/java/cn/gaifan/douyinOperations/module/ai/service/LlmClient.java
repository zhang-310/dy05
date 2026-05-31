package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;

import java.util.List;

/**
 * 通用 LLM 调用客户端（支持 OpenAI 兼容 API：Ollama / DeepSeek / Qwen / OpenAI）
 * 支持 Vision：图片 URL 输入，需模型支持多模态（Claude、GPT-4V 等）
 */
public interface LlmClient {

    /**
     * 发送聊天请求并返回生成内容
     *
     * @param model  AI 模型配置
     * @param system 系统提示词
     * @param prompt 用户提示词
     * @return 生成的文本内容
     */
    LlmResponse chat(AiModel model, String system, String prompt);

    /**
     * 带图片的聊天请求（Vision），需模型支持多模态
     *
     * @param model    AI 模型配置（Claude、GPT-4V 等）
     * @param system   系统提示词
     * @param prompt   用户提示词
     * @param imageUrls 图片 URL 列表（公网可访问），可为空
     * @return 生成的文本内容，不支持 Vision 时返回失败
     */
    LlmResponse chatWithImage(AiModel model, String system, String prompt, List<String> imageUrls);

    /**
     * 三层回退：依次尝试 models，首个成功即返回
     *
     * @param models 模型列表（主→备1→备2）
     * @param system 系统提示词
     * @param prompt 用户提示词
     * @return 首个成功的响应，或最后一个失败响应
     */
    default LlmResponse chatWithFallback(List<AiModel> models, String system, String prompt) {
        if (models == null || models.isEmpty()) {
            return new LlmResponse(null, 0, false, "无可用模型");
        }
        LlmResponse last = null;
        for (AiModel m : models) {
            if (m == null) continue;
            last = chat(m, system, prompt);
            if (last.success()) return last;
        }
        return last != null ? last : new LlmResponse(null, 0, false, "无可用模型");
    }

    /**
     * 带图片的三层回退，优先使用支持 Vision 的模型
     */
    default LlmResponse chatWithImageFallback(List<AiModel> models, String system, String prompt, List<String> imageUrls) {
        if (models == null || models.isEmpty()) {
            return new LlmResponse(null, 0, false, "无可用模型");
        }
        if (imageUrls == null || imageUrls.isEmpty()) {
            return chatWithFallback(models, system, prompt);
        }
        LlmResponse last = null;
        for (AiModel m : models) {
            if (m == null) continue;
            last = chatWithImage(m, system, prompt, imageUrls);
            if (last.success()) return last;
        }
        return last != null ? last : new LlmResponse(null, 0, false, "无可用模型");
    }

    record LlmResponse(String content, long tokensUsed, boolean success, String errorMsg) {}

    /**
     * Function-calling / Tool-use：带工具声明发起请求
     * 返回包含 toolCallsJson（function_call 或 tool_calls JSON 字符串，为空则表示纯文本响应）
     */
    default LlmToolResponse chatWithToolsStructured(AiModel model, java.util.List<java.util.Map<String, Object>> messages, String toolsJson) {
        return new LlmToolResponse(null, 0, false, "当前实现不支持 tools 调用", null, null);
    }

    record LlmToolResponse(String content, long tokensUsed, boolean success, String errorMsg,
                           String toolCallsJson, String rawJson) {
        public String reasoningContent() {
            if (rawJson == null || rawJson.isBlank()) {
                return null;
            }
            try {
                com.fasterxml.jackson.databind.JsonNode root =
                        new com.fasterxml.jackson.databind.ObjectMapper().readTree(rawJson);
                com.fasterxml.jackson.databind.JsonNode node =
                        root.path("choices").path(0).path("message").path("reasoning_content");
                if (node.isMissingNode() || node.isNull()) {
                    return null;
                }
                String value = node.asText("");
                return value.isBlank() ? null : value;
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
