package cn.gaifan.douyinOperations.module.ai.vo;

import lombok.Data;

import java.util.List;

/**
 * 视频对比分析请求 VO
 */
@Data
public class VideoCompareRequestVO {

    /** 主视频 ID */
    private Long videoId;

    /** 对比类型：similar（同类视频对比）、history（历史视频对比） */
    private String compareType;

    /** 对比视频 ID 列表（最多 5 个） */
    private List<Long> compareVideoIds;

    /** 分析维度（可选，默认全部） */
    private List<String> dimensions;
}
