package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

/**
 * 账号视频查询参数（分页 + 可选筛选/排序）
 */
@Data
public class AccountVideosQueryVO {

    private Long accountId;
    private Integer page = 0;
    private Integer rows = 20;

    /** 标题关键词（模糊） */
    private String keyword;

    /** 深度分析状态：pending/processing/completed/failed 等，空表示不限 */
    private String deepAnalyzeStatus;

    /**
     * 排序字段白名单：createTime、viewCount、viralScore、updateTime、id
     */
    private String sortName = "createTime";

    /** asc 或 desc */
    private String sortOrder = "desc";
}
