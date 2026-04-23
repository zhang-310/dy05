package cn.gaifan.douyinOperations.module.messaging.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class MsgPlatformConfigSearchVO extends BasicQueryDto {

    private Long ownerId;
    private String platform;
    private Integer status;
}
