package cn.gaifan.douyinOperations.module.live.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 直播话术段落值对象
 * 用于返回话术段落信息给前端
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiveSessionScriptSlotVO {

    /**
     * 话术段落 ID
     */
    private Long id;

    /**
     * 直播场次 ID
     */
    private Long liveSessionId;

    /**
     * 段落序号（从 0 开始）
     */
    private Integer slotIndex;

    /**
     * 话术版本 ID
     */
    private Long scriptVersionId;

    /**
     * 话术内容
     */
    private String content;

    /**
     * 建议讲解时长（秒）
     */
    private Integer durationSeconds;

    /**
     * 话术类型
     */
    private String scriptType;

    /**
     * 话术风格
     */
    private String style;

    /**
     * 是否当前段落
     */
    private Boolean isCurrent;

    /**
     * 是否已讲解完成
     */
    private Boolean isCompleted;

    /**
     * 开始讲解时间
     */
    private LocalDateTime startedAt;

    /**
     * 完成讲解时间
     */
    private LocalDateTime completedAt;
}
