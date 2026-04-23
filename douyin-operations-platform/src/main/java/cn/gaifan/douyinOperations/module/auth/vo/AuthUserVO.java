package cn.gaifan.douyinOperations.module.auth.vo;

import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 用户列表/详情 VO（管理员）
 */
@Data
public class AuthUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String mobile;
    private String email;
    private String nickname;
    private String avatarUrl;
    private String roleCode;
    private Integer status;
    private Timestamp bannedAt;
    private String bannedReason;
    private Timestamp lastLoginAt;
    private Timestamp createTime;
}
