package cn.gaifan.douyinOperations.module.ai.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库文档列表查询参数（支持服务端分页）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KbDocumentSearchVO extends BasicQueryDto {

    /** 关键词（标题模糊搜索） */
    private String keyword;
    /** 来源类型（source_type / file_type） */
    private String sourceType;
}
