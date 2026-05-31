package cn.gaifan.douyinOperations.module.agent.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 智能体查询 VO
 * 继承 BasicQueryDto，包含分页、排序等基础查询参数
 *
 * @author gaifan
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentSearchVO extends BasicQueryDto {
    /** 智能体 ID（可选） */
    private Long agentId;

    /** 智能体名称关键词（可选） */
    private String agentName;

    /** 智能体类型：0=自定义 1=话术生成 2=违规检测 3=商品分析 4=场次规划 5=数据分析 6=客户服务（可选） */
    private Integer agentType;

    /** 状态：0=禁用 1=启用（可选） */
    private Integer statusEnabled;

    /**
     * 排序字段：
     * default=默认（创建时间倒序）
     * rating=评分最高
     * popular=最热门（对话次数）
     * name=名称升序
     */
    private String sortBy;
}
