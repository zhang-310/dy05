package cn.gaifan.douyinOperations.module.live.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LiveScriptApprovalSearchVO extends BasicQueryDto {

    private Long scriptId;
    private Long sessionId;
    /** 审核状态: 1=待审核, 2=已通过, 3=已拒绝 */
    private Integer status;
    private Long submitterId;
    private Long reviewerId;
}
