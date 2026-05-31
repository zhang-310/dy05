package cn.gaifan.douyinOperations.module.ai.service;

import java.util.Map;

/**
 * AI Prompt 配置服务：从 sys_config 读取 prompt 模板，支持占位符替换。
 * 管理后台可在线编辑，无需重启。
 */
public interface AiPromptConfigService {

    /**
     * 获取 prompt：优先从 sys_config 读取，无则返回默认值
     */
    String getPrompt(String configKey, String defaultPrompt);

    /**
     * 获取 prompt 并替换占位符。格式：{{varName}} 替换为 vars.get("varName")
     */
    String getPrompt(String configKey, Map<String, Object> vars, String defaultPrompt);
}
