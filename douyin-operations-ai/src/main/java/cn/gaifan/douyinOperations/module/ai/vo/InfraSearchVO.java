package cn.gaifan.douyinOperations.module.ai.vo;

import lombok.Data;

/**
 * AI 运维管理 - 分页查询参数
 */
@Data
public class InfraSearchVO {

    /** 知识库 ID */
    private Long kbId;

    /** 关键词 */
    private String keyword;

    /** 状态（仅索引队列） */
    private String status;

    /** 页码，从 0 开始 */
    private Integer page = 0;

    /** 每页条数 */
    private Integer rows = 30;
}
