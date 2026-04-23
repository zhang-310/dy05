package cn.gaifan.douyinOperations.module.benchmark.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 质量脚本查询 VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BenchmarkQualityScriptSearchVO extends BasicQueryDto {

    /**
     * 视频 ID
     */
    private Long videoId;

    /**
     * 脚本类型
     */
    private String scriptType;

    /**
     * 行业分类
     */
    private String industry;

    /**
     * 场景类型
     */
    private String sceneType;

    /**
     * 最小质量评分
     */
    private Double minQualityScore;

    /**
     * 最小互动率
     */
    private Double minEngagementRate;

    /**
     * 最小传播力评分
     */
    private Double minViralScore;

    /**
     * 关键词（脚本内容模糊查询）
     */
    private String keyword;
}
