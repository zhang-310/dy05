package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

/**
 * G-1 批量并行生成结果
 */
@Data
public class ParallelGenerateResult {
    /** 是否成功 */
    private boolean success;
    /** 生成的内容（成功时） */
    private String content;
    /** 错误消息（失败时） */
    private String error;
    /** 耗时（毫秒） */
    private long durationMs;
}
