package cn.gaifan.douyinOperations.module.ai.service;

/**
 * ComfyUI 本地服务：关键帧生成（支持人物/场景参考图）
 * 配置 COMFYUI_URL 后可用，默认 http://localhost:8188
 */
public interface ComfyUIService {

    /**
     * 生成关键帧图片
     *
     * @param prompt                场景描述提示词
     * @param characterReferenceUrl 人物参考图 URL（可选，用于 img2img 风格迁移）
     * @param sceneReferenceUrl     场景参考图 URL（可选，用于 img2img 风格迁移）
     * @param width                 输出宽度
     * @param height                输出高度
     * @param steps                 采样步数
     * @param cfg                   CFG Scale
     * @return 生成图片的本地路径或可访问 URL
     */
    String generateKeyframe(
            String prompt,
            String characterReferenceUrl,
            String sceneReferenceUrl,
            int width,
            int height,
            int steps,
            double cfg
    );

    /**
     * 是否已配置且可连接
     */
    boolean isAvailable();
}
