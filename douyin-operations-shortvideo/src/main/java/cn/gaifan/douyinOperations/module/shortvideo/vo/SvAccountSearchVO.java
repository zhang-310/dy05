package cn.gaifan.douyinOperations.module.shortvideo.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 账号搜索查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SvAccountSearchVO extends BasicQueryDto {

    /** 关键词（昵称、抖音号、sec_uid） */
    private String keyword;

    /** 账号分类 */
    private String accountCategory;

    /** 来源类型：manual/keyword_search/recommend */
    private String sourceType;

    /** 来源关键词 */
    private String sourceKeyword;

    /** 账号状态：active/archived/blocked */
    private String status;

    /** 最小粉丝数 */
    private Long minFollowerCount;

    /** 最小爆款评分 */
    private Double minViralScore;
}
