package cn.gaifan.douyinOperations.module.auth.vo;

import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 在线用户 VO（最近登录记录聚合）
 */
@Data
public class OnlineUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String nickname;
    private Timestamp lastLoginAt;
    private String ip;
    private String deviceType;
    private String loginType;
}
