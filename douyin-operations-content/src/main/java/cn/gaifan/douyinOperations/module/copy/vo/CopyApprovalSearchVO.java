package cn.gaifan.douyinOperations.module.copy.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CopyApprovalSearchVO extends BasicQueryDto {
    private Long copyId;
    private Integer approvalStatus;
    private Long userId;
    /** 关键词（搜索文案标题） */
    private String keyword;
    /** P0-2: 数据隔离 - 审批记录所有者 ID */
    private Long ownerId;
}
