package cn.gaifan.douyinOperations.module.live.service;

import java.io.OutputStream;

/**
 * 直播话术 SSE 流式生成服务：槽位话术流式输出、进度、超时处理。
 * 从 LiveScriptGenerationServiceImpl 拆分。
 */
public interface LiveScriptStreamService {

    /**
     * 按槽位需求生成单段话术（SSE 流式）；modelId 可选
     */
    void generateForSlotStream(Long scriptId, String requirementOverride, Integer durationSecOverride,
                               Long modelId, OutputStream out) throws java.io.IOException;
}
