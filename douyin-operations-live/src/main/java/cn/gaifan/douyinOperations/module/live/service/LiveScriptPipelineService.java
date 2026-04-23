package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.live.entity.LiveScriptPipeline;

/**
 * 直播话术全自动流水线服务
 * Generate → QC → Auto-Refine → Save
 */
public interface LiveScriptPipelineService {

    /**
     * 启动一键全自动流水线
     *
     * @param sessionId 场次 ID
     * @param modelId   模型 ID（可选）
     * @param style     话术风格（可选）
     * @param useKbRef  是否引用知识库
     * @param ownerId   所属用户 ID
     * @return 创建的流水线记录
     */
    LiveScriptPipeline startPipeline(Long sessionId, Long modelId, String style, Boolean useKbRef, Long ownerId);

    /**
     * 查询流水线状态
     *
     * @param pipelineId 流水线 ID
     * @param ownerId    所属用户 ID
     * @return 流水线记录
     */
    LiveScriptPipeline getPipelineStatus(Long pipelineId, Long ownerId);

    /**
     * 取消流水线
     *
     * @param pipelineId 流水线 ID
     * @param ownerId    所属用户 ID
     */
    void cancelPipeline(Long pipelineId, Long ownerId);
}
