package cn.gaifan.douyinOperations.module.storage.service;

import java.nio.file.Path;

/**
 * BOS 参考图本地缓存：避免重复下载，节省 99% 流量
 */
public interface BosReferenceImageCacheService {

    /**
     * 获取参考图本地路径（优先从缓存读取）
     *
     * @param bosKey       BOS 对象 key
     * @param currentUserId 当前用户 ID（校验归属）
     * @return 本地文件路径，无法获取时返回 null
     */
    Path getCachedPath(String bosKey, Long currentUserId);
}
