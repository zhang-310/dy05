package cn.gaifan.douyinOperations.module.sms.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SmsProviderConfigSearchVO extends BasicQueryDto {
    private Long ownerId;
    private String providerCode;
    private Integer status;
    private Integer isDefault;
}
