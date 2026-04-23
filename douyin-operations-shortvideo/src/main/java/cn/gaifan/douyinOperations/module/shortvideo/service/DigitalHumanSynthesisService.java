package cn.gaifan.douyinOperations.module.shortvideo.service;

import java.util.Map;

/**
 * 工作流 digitalHuman 步骤：可配置占位/测试 URL，便于后续对接 HeyGen 等供应商。
 */
public interface DigitalHumanSynthesisService {

    boolean isConfigured();

    /**
     * 当前阶段：若开启 stub，返回配置的成片 URL；真实厂商接入时在此扩展。
     */
    String synthesizePlaceholder(Map<String, Object> params, Long userId, Long projectId);
}
