package cn.gaifan.douyinOperations.module.auth.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限查询 VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuthPermissionSearchVO extends BasicQueryDto {
    private String keyword;
    private String type;
}
