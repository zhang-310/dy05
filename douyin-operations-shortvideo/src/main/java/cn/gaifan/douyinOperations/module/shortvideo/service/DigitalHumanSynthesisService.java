package cn.gaifan.douyinOperations.module.shortvideo.service;

import java.util.Map;

/**
 * 工作流 digitalHuman 步骤：调用真实供应商，未配置时由工作流明确失败。
 */
public interface DigitalHumanSynthesisService {

    boolean isConfigured();

    String synthesize(Map<String, Object> params, Long userId, Long projectId);
}
