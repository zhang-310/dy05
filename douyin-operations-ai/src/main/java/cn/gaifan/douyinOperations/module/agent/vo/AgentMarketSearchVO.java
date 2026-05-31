package cn.gaifan.douyinOperations.module.agent.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 智能体市场查询 VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentMarketSearchVO extends BasicQueryDto {
    private String keyword;
    private String category;
}
