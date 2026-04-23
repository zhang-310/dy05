package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 拍摄任务分页查询
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SvShootingTaskSearchVO extends ShortVideoBasicQueryDto {

    private String title;
    /** 状态 0-5，null 不限 */
    private Integer status;
    private String shootDateFrom;
    private String shootDateTo;
    private Long personaId;
    private Long photographerId;
    private Long anchorUserId;
}
