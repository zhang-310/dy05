package cn.gaifan.douyinOperations.module.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录成功返回：token + 用户基本信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String token;
    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String roleCode;
    private Long organizationId;
    private String organizationName;
}
