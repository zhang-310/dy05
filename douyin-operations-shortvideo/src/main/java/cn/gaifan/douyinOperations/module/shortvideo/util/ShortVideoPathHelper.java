package cn.gaifan.douyinOperations.module.shortvideo.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 短视频 BOS 存储路径生成工具
 * 路径规范：{userId}/{date}/{taskId}/{subFolder}/{filename}
 * 参考：docs/design/BAIDU-BOS-STORAGE-INTEGRATION.md
 */
public final class ShortVideoPathHelper {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private ShortVideoPathHelper() {
    }

    /**
     * 生成任务级路径前缀：{userId}/{date}/{taskId}/
     */
    public static String taskPrefix(Long userId, String date, Long taskId) {
        return userId + "/" + date + "/" + taskId + "/";
    }

    /**
     * 生成任务级路径前缀（使用当前日期）
     */
    public static String taskPrefixToday(Long userId, Long taskId) {
        return taskPrefix(userId, LocalDate.now().format(DATE_FMT), taskId);
    }

    /**
     * 关键帧路径：{userId}/{date}/{taskId}/keyframes/shot_001.jpg（首帧）
     */
    public static String keyframeKey(Long userId, String date, Long taskId, int shotNumber) {
        String padded = String.format("shot_%03d.jpg", shotNumber);
        return taskPrefix(userId, date, taskId) + "keyframes/" + padded;
    }

    /**
     * 尾帧路径：{userId}/{date}/{taskId}/keyframes/shot_001_end.jpg
     */
    public static String endFrameKey(Long userId, String date, Long taskId, int shotNumber) {
        String padded = String.format("shot_%03d_end.jpg", shotNumber);
        return taskPrefix(userId, date, taskId) + "keyframes/" + padded;
    }

    /**
     * 视频片段路径：{userId}/{date}/{taskId}/videos/shot_001.mp4
     */
    public static String videoClipKey(Long userId, String date, Long taskId, int shotNumber) {
        String padded = String.format("shot_%03d.mp4", shotNumber);
        return taskPrefix(userId, date, taskId) + "videos/" + padded;
    }

    /**
     * 成片路径：{userId}/{date}/{taskId}/videos/final.mp4
     */
    public static String finalVideoKey(Long userId, String date, Long taskId) {
        return taskPrefix(userId, date, taskId) + "videos/final.mp4";
    }

    /**
     * 配音路径：{userId}/{date}/{taskId}/audios/voice_001.mp3
     */
    public static String audioKey(Long userId, String date, Long taskId, int shotNumber) {
        String padded = String.format("voice_%03d.mp3", shotNumber);
        return taskPrefix(userId, date, taskId) + "audios/" + padded;
    }

    /**
     * 封面路径：{userId}/{date}/{taskId}/thumbnails/cover_1.jpg
     */
    public static String thumbnailKey(Long userId, String date, Long taskId, int index) {
        return taskPrefix(userId, date, taskId) + "thumbnails/cover_" + index + ".jpg";
    }

    /**
     * 用户级参考图路径：{userId}/references/characters/{characterId}/main.jpg
     */
    public static String characterReferenceKey(Long userId, String characterId, String ext) {
        return userId + "/references/characters/" + characterId + "/" + uniqueFilename(ext);
    }

    /**
     * 用户级场景参考图路径：{userId}/references/scenes/{sceneId}/main.jpg
     */
    public static String sceneReferenceKey(Long userId, String sceneId, String ext) {
        return userId + "/references/scenes/" + sceneId + "/" + uniqueFilename(ext);
    }

    /**
     * 音效路径：{userId}/sfx/{date}/sfx_{uuid}.mp3
     */
    public static String sfxKey(Long userId, String date, String filename) {
        long uid = userId != null ? userId : 0;
        return uid + "/sfx/" + date + "/" + (filename != null ? filename : "sfx_" + uniqueFilename(".mp3"));
    }

    /**
     * 生成唯一文件名（UUID + 扩展名）
     */
    public static String uniqueFilename(String ext) {
        if (ext == null || ext.isEmpty()) ext = "";
        if (!ext.isEmpty() && !ext.startsWith(".")) ext = "." + ext;
        return UUID.randomUUID().toString().replace("-", "") + ext;
    }
}
