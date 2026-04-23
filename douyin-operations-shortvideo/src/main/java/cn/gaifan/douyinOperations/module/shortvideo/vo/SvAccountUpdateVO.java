package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

/**
 * 账号更新参数
 */
@Data
public class SvAccountUpdateVO {

    private Long id;

    /** 账号分类 */
    private String accountCategory;

    /** 行业标签（JSON 数组字符串） */
    private String industryTags;

    /** 内容标签（JSON 数组字符串） */
    private String contentTags;

    /** 备注 */
    private String notes;

    /** 账号状态：active/archived/blocked */
    private String status;
}
