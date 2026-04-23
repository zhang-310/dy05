package cn.gaifan.douyinOperations.module.auth.vo;

import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 个人信息（当前用户）展示/更新
 */
@Data
public class ProfileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String mobile;
    private String email;
    private String nickname;
    private String avatarUrl;
    private String roleCode;
    private Long organizationId;
    private String organizationName;
    private Timestamp createTime;
}
