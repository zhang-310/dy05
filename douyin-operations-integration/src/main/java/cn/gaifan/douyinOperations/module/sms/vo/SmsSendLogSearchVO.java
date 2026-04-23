package cn.gaifan.douyinOperations.module.sms.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SmsSendLogSearchVO extends BasicQueryDto {
    private Long ownerId;
    private String phoneNumber;
    private String status;
    private String bizType;
}
