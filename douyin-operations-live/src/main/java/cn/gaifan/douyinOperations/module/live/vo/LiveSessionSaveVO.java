package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 直播场次保存/更新 VO
 */
@Data
public class LiveSessionSaveVO {

    private Long id;

    private Long userId;

    private Long accountId;

    @NotBlank(message = "直播标题不能为空")
    private String liveTitle;

    private String liveDescription;

    private String liveUrl;

    private Integer status = 0;

    // P0-2: 补全必需字段
    private Long personaId;              // 人设 ID

    private String scriptStyle;          // 话术风格（professional/friendly/passionate等）

    private String sessionType;          // 场次类型（normal/brand/promotion等）

    private String liveFormat;           // 直播形式（single/multi/collab等）

    private String scheduledTime;        // 预定开始时间

    private String scheduledEndTime;     // 预定结束时间
}
