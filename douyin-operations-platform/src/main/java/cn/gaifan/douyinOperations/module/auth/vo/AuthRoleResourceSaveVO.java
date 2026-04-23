package cn.gaifan.douyinOperations.module.auth.vo;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 角色-资源授权 VO（保存某角色绑定的资源 ID 列表）
 */
@Data
public class AuthRoleResourceSaveVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "角色ID不能为空")
    private Long roleId;
    /** 资源 ID 列表，空列表表示清空该角色全部授权 */
    private List<Long> resourceIds;
}
