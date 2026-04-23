package cn.gaifan.douyinOperations.module.auth.vo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 角色新增/编辑 VO
 */
@Data
public class AuthRoleSaveVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    @NotBlank(message = "角色编码不能为空")
    @Size(max = 32)
    private String roleCode;
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64)
    private String roleName;
    private Integer sortOrder = 0;
    /** 状态 0禁用 1正常 */
    private Integer status = 1;
}
