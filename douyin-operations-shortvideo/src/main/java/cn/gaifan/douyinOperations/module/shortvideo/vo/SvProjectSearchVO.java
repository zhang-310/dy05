package cn.gaifan.douyinOperations.module.shortvideo.vo;

import cn.gaifan.douyinOperations.module.shortvideo.vo.ShortVideoBasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 短视频项目查询 VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SvProjectSearchVO extends ShortVideoBasicQueryDto {

    private Long ownerId;
    private String status;
    private String projectType;
    private String title;
}
