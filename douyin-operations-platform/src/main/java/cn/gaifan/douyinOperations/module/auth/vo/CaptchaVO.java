package cn.gaifan.douyinOperations.module.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 图片验证码响应（captchaId 用于提交登录时校验）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String captchaId;
    private String imageBase64;
}
