package cn.gaifan.douyinOperations.module.auth.vo;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 用户详情查询入参（仅管理员）
 */
@Data
public class AuthUserGetVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "用户ID不能为空")
    private Long id;
}
