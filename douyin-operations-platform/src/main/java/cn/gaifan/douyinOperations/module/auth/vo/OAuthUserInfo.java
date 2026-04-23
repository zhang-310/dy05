package cn.gaifan.douyinOperations.module.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 第三方 OAuth 用户信息（openId/unionId 等，用于登录或绑定）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuthUserInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String openId;
    private String unionId;
    private String nickname;
    private String avatarUrl;
}
