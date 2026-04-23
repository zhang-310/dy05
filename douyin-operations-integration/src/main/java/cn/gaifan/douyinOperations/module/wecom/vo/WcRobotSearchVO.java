package cn.gaifan.douyinOperations.module.wecom.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WcRobotSearchVO extends BasicQueryDto {
    private String robotType;
    private Integer status;
    private Long ownerId;
}
