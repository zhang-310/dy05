package cn.gaifan.douyinOperations.module.douyin.vo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 抖音账号保存 VO
 */
@Data
public class DouyinAccountSaveVO {

    private Long id;

    private Long ownerId;

    @NotBlank(message = "账号名称不能为空")
    @Size(max = 128, message = "账号名称不能超过 128 字符")
    private String accountName;

    @NotBlank(message = "账号 ID 不能为空")
    @Size(max = 64, message = "账号 ID 不能超过 64 字符")
    private String accountId;

    private Long followCount;

    private Long fanCount;

    private Long videoCount;

    private Long totalLikes;

    @Size(max = 2000, message = "账号简介不能超过 2000 字符")
    private String description;

    private Integer status;
}
