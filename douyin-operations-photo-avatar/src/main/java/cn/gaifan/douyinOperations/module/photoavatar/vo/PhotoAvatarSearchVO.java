package cn.gaifan.douyinOperations.module.photoavatar.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PhotoAvatarSearchVO extends BasicQueryDto {

    private String status;
    private String outfitStyle;
    private String background;
}
