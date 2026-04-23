package cn.gaifan.douyinOperations.module.shortvideo.vo;

import cn.gaifan.douyinOperations.module.shortvideo.vo.ShortVideoBasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SvScriptTemplateSearchVO extends ShortVideoBasicQueryDto {
    private String keyword;
    private String templateType;
    private String scene;
    private Long categoryId;
    private Integer status;
    private Long ownerId;
}
