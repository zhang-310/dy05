package cn.gaifan.douyinOperations.module.auth.vo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 用户新增/编辑 VO（管理员）
 */
@Data
public class AuthUserSaveVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    @NotBlank(message = "用户名不能为空")
    @Size(min = 1, max = 64)
    private String username;
    @Size(max = 64)
    private String nickname;
    @Size(max = 20)
    private String mobile;
    @Size(max = 128)
    private String email;
    @Size(max = 256)
    private String avatarUrl;
    @NotBlank(message = "角色不能为空")
    @Size(max = 32)
    private String roleCode;
    /** 新增时必填；编辑时为空表示不修改密码 */
    @Size(max = 128)
    private String password;
}
