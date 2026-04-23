package cn.gaifan.douyinOperations.module.shortvideo.service;

import cn.gaifan.douyinOperations.module.shortvideo.vo.*;

import java.util.List;

/**
 * 短视频 AI 创作服务
 */
public interface ShortVideoAiService {

    /**
     * AI 生成文案（使用 short_video_script 任务配置）
     */
    String generateCopy(AiCopyGenerateVO vo, Long userId);

    /**
     * AI 生成文案（使用指定任务配置，如 copy_processing 供文案模块独立配置）
     *
     * @param taskCode 任务编码，null 时使用 short_video_script
     */
    String generateCopy(AiCopyGenerateVO vo, Long userId, String taskCode);

    /**
     * AI 生成脚本
     */
    String generateScript(AiScriptGenerateVO vo, Long userId);

    /**
     * AI 生成标题
     */
    List<String> generateTitles(AiTitleGenerateVO vo, Long userId);

    /**
     * AI 生成视频方案
     */
    String generateVideoPlan(AiVideoPlanGenerateVO vo, Long userId);
}
