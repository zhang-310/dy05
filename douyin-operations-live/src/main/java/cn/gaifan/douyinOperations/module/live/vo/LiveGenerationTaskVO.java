package cn.gaifan.douyinOperations.module.live.vo;

import cn.gaifan.douyinOperations.module.live.entity.LiveGenerationTask;
import lombok.Data;

import java.sql.Timestamp;

/**
 * 话术生成任务响应 VO
 */
@Data
public class LiveGenerationTaskVO {

    private Long id;
    private Long sessionId;
    private String status;
    private Integer totalSlots;
    private Integer completedSlots;
    private Integer failedSlots;
    private String style;
    private Long modelId;
    private Boolean useKbRef;
    private String hotKeywords;
    private String errorMessage;
    private String executionMode;
    private Long ownerId;
    private Timestamp createTime;
    private Timestamp updateTime;

    /**
     * 从实体转换为 VO
     */
    public static LiveGenerationTaskVO fromEntity(LiveGenerationTask entity) {
        if (entity == null) return null;
        LiveGenerationTaskVO vo = new LiveGenerationTaskVO();
        vo.setId(entity.getId());
        vo.setSessionId(entity.getSessionId());
        vo.setStatus(entity.getStatus());
        vo.setTotalSlots(entity.getTotalSlots());
        vo.setCompletedSlots(entity.getCompletedSlots());
        vo.setFailedSlots(entity.getFailedSlots());
        vo.setStyle(entity.getStyle());
        vo.setModelId(entity.getModelId());
        vo.setUseKbRef(entity.getUseKbRef());
        vo.setHotKeywords(entity.getHotKeywords());
        vo.setErrorMessage(entity.getErrorMessage());
        vo.setExecutionMode(entity.getExecutionMode());
        vo.setOwnerId(entity.getOwnerId());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
