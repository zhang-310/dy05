package cn.gaifan.douyinOperations.module.benchmark.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 对标账号查询VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BenchmarkAccountSearchVO extends BasicQueryDto {

    /**
     * 关键词（账号名称）
     */
    private String keyword;

    /**
     * 平台
     */
    private String platform;

    /**
     * 分类
     */
    private String category;

    /**
     * 是否启用
     */
    private Boolean isActive;

    /**
     * 最小粉丝数
     */
    private Long minFanCount;

    /**
     * 最大粉丝数
     */
    private Long maxFanCount;
}
