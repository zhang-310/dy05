package cn.gaifan.douyinOperations.module.shortvideo.vo;

import cn.gaifan.douyinOperations.module.shortvideo.vo.ShortVideoBasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SvScriptSearchVO extends ShortVideoBasicQueryDto {

    private Long ownerId;
    private String scriptType;
    private String style;
    private String title;
}
