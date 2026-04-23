package cn.gaifan.douyinOperations.module.script.service;

import java.util.List;

public interface AiService {
    /**
     * 调用 AI 生成话术
     * @param prompt 提示词
     * @return 生成的话术内容
     */
    String generateScript(String prompt);

    /**
     * 批量生成话术
     * @param prompts 提示词列表
     * @return 生成的话术列表
     */
    List<String> generateScriptBatch(List<String> prompts);

    /**
     * 优化话术
     * @param content 原始话术
     * @param style 优化风格
     * @return 优化后的话术
     */
    String optimizeScript(String content, String style);

    /**
     * 评分话术
     * @param content 话术内容
     * @return 评分 (0-10)
     */
    Double scoreScript(String content);
}
