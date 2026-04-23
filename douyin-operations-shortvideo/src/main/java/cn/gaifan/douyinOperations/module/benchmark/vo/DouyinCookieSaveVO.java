package cn.gaifan.douyinOperations.module.benchmark.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Cookie保存VO
 */
@Data
public class DouyinCookieSaveVO {

    /**
     * ID（更新时必填）
     */
    private Long id;

    /**
     * Cookie名称
     */
    @NotBlank(message = "Cookie名称不能为空")
    private String cookieName;

    /**
     * 浏览器请求头 Cookie 整串：多对 name=value 以 "; " 分隔（扫码登录与手动粘贴均为同一格式，非单个键值）。
     */
    @NotBlank(message = "Cookie值不能为空")
    private String cookieValue;

    /**
     * 平台
     */
    private String platform = "douyin";

    /**
     * 账号名称
     */
    private String accountName;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 是否有效
     */
    private Boolean isValid = true;

    /**
     * 备注
     */
    private String notes;
}
