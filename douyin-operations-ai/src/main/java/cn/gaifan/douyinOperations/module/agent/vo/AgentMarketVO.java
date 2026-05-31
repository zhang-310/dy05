package cn.gaifan.douyinOperations.module.agent.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能体市场 VO
 */
@Data
public class AgentMarketVO {
    private Long id;
    private String name;
    private String description;
    private String category;
    private Double rating;
    private Integer downloadCount;
    private LocalDateTime createTime;
}
