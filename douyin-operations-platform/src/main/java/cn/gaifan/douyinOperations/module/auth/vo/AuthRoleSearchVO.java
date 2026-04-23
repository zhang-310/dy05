package cn.gaifan.douyinOperations.module.auth.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色搜索 VO（分页、条件）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuthRoleSearchVO extends BasicQueryDto {

    private String roleCode;
    private String roleName;
    /** 状态 0禁用 1正常 */
    private Integer status;
}
