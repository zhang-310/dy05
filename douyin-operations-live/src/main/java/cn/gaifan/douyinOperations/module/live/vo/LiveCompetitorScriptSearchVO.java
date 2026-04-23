package cn.gaifan.douyinOperations.module.live.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LiveCompetitorScriptSearchVO extends BasicQueryDto {
    private String keyword;
    private String platform;
}
