package cn.gaifan.douyinOperations.module.live.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 直播话术导航查询 VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LiveScriptNavigationSearchVO extends BasicQueryDto {
    private Long sessionId;
    private String keyword;
}
