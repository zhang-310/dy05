package cn.gaifan.douyinOperations.module.auth.vo;

import lombok.Data;

import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 更新个人信息请求
 */
@Data
public class ProfileUpdateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Size(max = 64)
    private String nickname;
    @Size(max = 256)
    private String avatarUrl;
    @Size(max = 20)
    private String mobile;
    @Size(max = 128)
    private String email;
}
