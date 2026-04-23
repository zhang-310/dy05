package cn.gaifan.douyinOperations.module.benchmark.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 对标视频查询VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BenchmarkVideoSearchVO extends BasicQueryDto {

    /**
     * 账号ID
     */
    private Long benchmarkAccountId;

    /**
     * 关键词（标题）
     */
    private String keyword;

    /**
     * 分析状态
     */
    private String analysisStatus;

    /**
     * 是否符合条件
     */
    private Boolean isQualified;

    /**
     * 最小点赞数
     */
    private Integer minLikeCount;

    /**
     * 最大点赞数
     */
    private Integer maxLikeCount;
}
