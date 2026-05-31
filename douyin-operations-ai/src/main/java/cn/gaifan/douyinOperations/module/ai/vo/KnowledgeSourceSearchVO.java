package cn.gaifan.douyinOperations.module.ai.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识源查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeSourceSearchVO extends BasicQueryDto {

    /** 关键词（前端 keyword，映射到 sourceName 模糊搜索） */
    private String keyword;
    private String sourceName;
    private String sourceType;
    private Integer status;
}
