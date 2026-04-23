package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.live.vo.DanmakuSentimentSnapshotVO;

import java.util.List;

/**
 * 弹幕情绪滚动窗口聚合（词典分类，非 LLM）
 */
public interface DanmakuSentimentService {

    /**
     *  ingest 一条弹幕文本（幂等按条计数）
     */
    void ingest(Long liveSessionId, String content);

    /**
     * 批量 ingest（顺序逐条进入同一滚动窗口；列表为空则 noop）
     */
    void ingestBatch(Long liveSessionId, List<String> contents);

    /**
     * 当前窗口内聚合快照（无数据时 counts 为 0，dominant=none）
     */
    DanmakuSentimentSnapshotVO getSnapshot(Long liveSessionId);

    /**
     * P2-1: 获取当前窗口内高频关键词（如"太贵了"），用于话术切换建议。
     * key = 关键词，value = 出现次数
     */
    default java.util.Map<String, Integer> getKeywordFrequency(Long liveSessionId) {
        return java.util.Collections.emptyMap();
    }
}
