package cn.gaifan.douyinOperations.module.live.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 弹幕情绪快照（词典快路径，滚动窗口聚合）
 * 供面板初始化、实时建议 SSE 推送。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DanmakuSentimentSnapshotVO {

    /** 窗口内正向条数 */
    private int positiveCount;

    /** 窗口内中性条数 */
    private int neutralCount;

    /** 窗口内负向条数 */
    private int negativeCount;

    /** 窗口内总条数 */
    private int totalInWindow;

    /** 聚合窗口秒数 */
    private int windowSeconds;

    /** 主导情绪：positive / neutral / negative / none（无样本） */
    private String dominant;

    /** 快照时间戳（毫秒） */
    private long updatedAt;
}
