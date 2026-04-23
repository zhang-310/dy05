package cn.gaifan.douyinOperations.module.shortvideo.service;

import java.util.Map;

/**
 * 快速生成服务：3 步一键生成（主题 + 内容 + 风格）
 */
public interface ShortVideoQuickService {

    /**
     * 快速生成：创建项目 → 生成脚本 → 生成分镜，返回 projectId
     *
     * @param theme    主题（美食/旅行/情感等）
     * @param keywords 关键词或爆款链接
     * @param style    风格（温馨/搞笑/高端等）
     * @param ownerId  用户 ID
     * @return projectId, scriptId, shotListId, scriptContent
     */
    Map<String, Object> quickGenerate(String theme, String keywords, String style, Long ownerId);
}
