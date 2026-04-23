package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 直播话术保存/更新 VO
 */
@Data
public class LiveScriptSaveVO {

    private Long id;

    @NotNull(message = "直播场次 ID 不能为空")
    private Long sessionId;

    @NotBlank(message = "话术内容不能为空")
    private String scriptContent;

    private Integer sequenceNo;

    private Long executionTime;

    private Integer executed = 0;
}
