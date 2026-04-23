package cn.gaifan.douyinOperations.module.shortvideo.service;

/**
 * 热点话题同步服务（鬼鬼鸭 / TianAPI 抖音热搜 → sv_hot_topic）
 */
public interface SvHotTopicSyncService {

    /** 执行一次同步 */
    int syncFromDouyinHot();
}
