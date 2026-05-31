package cn.gaifan.douyinOperations.module.ai.service.brain;

import java.util.Map;

/**
 * 多模态风格一致性服务（P1）
 * 五位主播：文/图/音/视频统一风格向量
 */
public interface HostStyleConsistencyService {

    /**
     * 获取主播的风格约束（用于文本生成 prompt）
     */
    String getTextStylePrompt(String hostCode);

    /**
     * 获取主播的多模态风格向量（tone/visual/pacing 等）
     */
    Map<String, String> getStyleVector(String hostCode);

    /**
     * 校验内容是否与主播风格一致（简单关键词匹配）
     */
    double getStyleConsistencyScore(String hostCode, String content);

    boolean isAvailable();
}
