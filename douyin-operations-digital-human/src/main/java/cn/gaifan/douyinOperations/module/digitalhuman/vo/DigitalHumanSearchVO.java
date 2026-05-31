package cn.gaifan.douyinOperations.module.digitalhuman.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DigitalHumanSearchVO extends BasicQueryDto {
    private String status;
    private String voiceType;
}
