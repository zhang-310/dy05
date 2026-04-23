package cn.gaifan.douyinOperations.module.auth.vo;

import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 登录记录 VO
 */
@Data
public class LoginLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String username;
    private String loginType;
    private String deviceType;
    private String ip;
    private String userAgent;
    private Integer status;
    private String failReason;
    private Timestamp loginTime;
}
