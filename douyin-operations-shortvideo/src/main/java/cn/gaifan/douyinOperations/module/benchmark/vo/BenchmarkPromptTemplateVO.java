package cn.gaifan.douyinOperations.module.benchmark.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Prompt 模板返回 VO
 */
@Data
public class BenchmarkPromptTemplateVO {

    /**
     * ID
     */
    private Long id;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板编码
     */
    private String templateCode;

    /**
     * 模板内容
     */
    private String templateContent;

    /**
     * 模板变量（JSON 格式）
     */
    private String templateVariables;

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
     * 版本号
     */
    private Integer version;

    /**
     * 描述
     */
    private String description;

    /**
     * 使用次数
     */
    private Integer usageCount;

    /**
     * 平均评分
     */
    private BigDecimal avgScore;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
