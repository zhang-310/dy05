package cn.gaifan.douyinOperations.module.benchmark.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Prompt 模板查询 VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BenchmarkPromptTemplateSearchVO extends BasicQueryDto {

    /**
     * 模板名称（模糊查询）
     */
    private String templateName;

    /**
     * 模板编码
     */
    private String templateCode;

    /**
     * 场景类型
     */
    private String sceneType;

    /**
     * 行业分类
     */
    private String industry;

    /**
     * 是否激活
     */
    private Boolean isActive;

    /**
     * 最小平均评分
     */
    private Double minAvgScore;
}
