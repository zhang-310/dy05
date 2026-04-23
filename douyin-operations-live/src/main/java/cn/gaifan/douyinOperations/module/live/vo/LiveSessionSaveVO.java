package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 直播场次保存/更新 VO
 */
@Data
public class LiveSessionSaveVO {

    private Long id;

    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    private Long accountId;

    @NotBlank(message = "直播标题不能为空")
    private String liveTitle;

    private String liveDescription;

    private String liveUrl;

    private Integer status = 0;
}
