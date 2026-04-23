package cn.gaifan.douyinOperations.module.douyin.vo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 抖音账号保存 VO
 */
@Data
public class DouyinAccountSaveVO {

    private Long id;

    private Long userId;

    @NotBlank(message = "账号名称不能为空")
    private String accountName;

    @NotBlank(message = "账号 ID 不能为空")
    private String accountId;

    private Long followCount;

    private Long fanCount;

    private Long videoCount;

    private Long totalLikes;

    private String description;

    private Integer status;
}
