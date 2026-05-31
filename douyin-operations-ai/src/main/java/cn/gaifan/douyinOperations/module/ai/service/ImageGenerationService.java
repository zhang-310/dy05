package cn.gaifan.douyinOperations.module.ai.service;

import java.util.List;
import java.util.Map;

/**
 * 图像生成服务
 */
public interface ImageGenerationService {

    /**
     * 文生图
     */
    ImageResult textToImage(TextToImageRequest request, Long userId);

    /**
     * 图生图
     */
    ImageResult imageToImage(ImageToImageRequest request, Long userId);

    /**
     * 图像编辑
     */
    ImageResult editImage(ImageEditRequest request, Long userId);

    /**
     * 获取生成历史
     */
    List<ImageGenerationHistory> getHistory(Long userId, int page, int size);

    /**
     * 文生图请求
     */
    record TextToImageRequest(
            String prompt,
            String negativePrompt,
            String style,
            Integer width,
            Integer height,
            Integer steps,
            Double cfgScale,
            Long seed
    ) {}

    /**
     * 图生图请求
     */
    record ImageToImageRequest(
            String imageUrl,
            String prompt,
            String negativePrompt,
            Double strength,
            Integer steps,
            Double cfgScale
    ) {}

    /**
     * 图像编辑请求
     */
    record ImageEditRequest(
            String imageUrl,
            String maskUrl,
            String prompt,
            Integer steps
    ) {}

    /**
     * 图像生成结果
     */
    record ImageResult(
            String imageUrl,
            String prompt,
            Map<String, Object> parameters,
            Long generationTime
    ) {}

    /**
     * 图像生成历史
     */
    record ImageGenerationHistory(
            Long id,
            String imageUrl,
            String prompt,
            String type,
            Map<String, Object> parameters,
            Long createTime
    ) {}
}
