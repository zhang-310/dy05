package cn.gaifan.douyinOperations.module.log.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作日志查询 VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LogOperationSearchVO extends BasicQueryDto {
    private String keyword;
    private String operationType;
    private Long userId;
}
