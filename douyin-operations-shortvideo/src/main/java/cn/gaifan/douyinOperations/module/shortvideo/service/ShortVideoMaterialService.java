package cn.gaifan.douyinOperations.module.shortvideo.service;

import java.util.List;
import java.util.function.Consumer;

/**
 * 短视频素材生产 Service
 */
public interface ShortVideoMaterialService {

    /**
     * 批量生成关键帧（文生图 + 可选 BOS 上传）
     */
    List<KeyframeResult> generateKeyframes(Long projectId, Long shotListId, List<KeyframeInput> shots, Long ownerId);

    /**
     * 批量生成关键帧（带进度回调，用于 SSE 实时推送）
     */
    List<KeyframeResult> generateKeyframesWithProgress(Long projectId, Long shotListId, List<KeyframeInput> shots,
                                                      Long ownerId, Consumer<ProgressEvent> progressCallback);

    /**
     * 批量生成配音（TTS + 可选 BOS 上传）
     */
    List<VoiceResult> generateVoiceBatch(Long projectId, Long shotListId, List<VoiceInput> shots, Long ownerId);

    /**
     * 图生视频批量
     */
    List<VideoResult> img2videoBatch(Long projectId, Long shotListId, List<Img2VideoInput> keyframes, Long ownerId);

    /**
     * 图生视频批量（带进度回调，用于 SSE 实时推送）
     */
    List<VideoResult> img2videoBatchWithProgress(Long projectId, Long shotListId, List<Img2VideoInput> keyframes,
                                                 Long ownerId, Consumer<ProgressEvent> progressCallback);

    /** ComfyUI 人物/场景参考：有 ComfyUI 时用于 IP-Adapter，否则忽略 */
    record KeyframeInput(Long shotId, Integer shotNumber, String sceneDescription, String style,
                        String characterReferenceUrl, String sceneReferenceUrl) {}
    record KeyframeResult(Long shotId, Integer shotNumber, String imageUrl, String bosKey, String prompt,
                         String endFrameUrl, String endFrameBosKey) {}

    record VoiceInput(Long shotId, Integer shotNumber, String dialogue, String voice, Double speed) {}
    record VoiceResult(Long shotId, Integer shotNumber, String audioUrl, String bosKey, Double duration) {}

    record Img2VideoInput(Long shotId, Integer shotNumber, String imageUrl, String endFrameUrl,
                         Integer duration, String motion, String sceneDescription,
                         String quality, String aspectRatio, String cameraType, String mood, String action) {
        /** 兼容旧版构造 (7参数) */
        public Img2VideoInput(Long shotId, Integer shotNumber, String imageUrl, String endFrameUrl,
                             Integer duration, String motion, String sceneDescription) {
            this(shotId, shotNumber, imageUrl, endFrameUrl, duration, motion, sceneDescription,
                 null, null, null, null, null);
        }
    }
    record VideoResult(Long shotId, Integer shotNumber, String videoUrl, String bosKey, Integer duration) {}

    /** 进度事件（用于 SSE 推送） */
    record ProgressEvent(String step, int current, int total, int percent, String message) {}
}
