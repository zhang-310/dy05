package cn.gaifan.douyinOperations.module.ai.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 模型对话流式服务：将 LLM 流式响应转发为 SSE
 */
public interface ModelChatStreamService {

    /**
     * 流式对话：连接模型、转发 token，通过 SseEmitter 发送 status/chunk/done/error
     *
     * @param modelId  模型 ID
     * @param messages 消息列表
     * @param emitter  SSE 发射器
     */
    void streamChat(Long modelId, List<Map<String, String>> messages, SseEmitter emitter);

    /**
     * 流式对话（带内容回调）：完成时回调 accumulatedContent，用于落库
     */
    void streamChat(Long modelId, List<Map<String, String>> messages, SseEmitter emitter, Consumer<String> onCompleteWithContent);

    /**
     * 流式对话：直接写入 OutputStream，每写一条立即 flush，避免缓冲导致前端卡住
     *
     * @param modelId  模型 ID
     * @param messages 消息列表
     * @param out      响应输出流
     */
    void streamChatToOutputStream(Long modelId, List<Map<String, String>> messages, OutputStream out);

    /**
     * RAG 流式对话：可选接入知识库检索，将检索结果作为上下文注入，并打点 recordRetrieval/recordCitation
     *
     * @param modelId  模型 ID
     * @param messages 消息列表
     * @param out      响应输出流
     * @param kbId     知识库 ID，null 则不做 RAG
     * @param userId   用户 ID，RAG 时必填
     */
    void streamChatToOutputStream(Long modelId, List<Map<String, String>> messages, OutputStream out, Long kbId, Long userId);

    /**
     * 流式输出并在**未出现 error 事件**且累积正文非空时回调（用于槽位 SSE 落库与 prompt 指纹）
     *
     * @param onSuccessContent 成功时传入全文 trim 后内容；失败或未产出正文时不调用
     */
    void streamChatToOutputStream(Long modelId, List<Map<String, String>> messages, OutputStream out,
            Consumer<String> onSuccessContent);

    /**
     * 写入错误 SSE 事件到输出流（用于鉴权/参数校验失败等）
     */
    void writeErrorToStream(OutputStream out, String error);
}
