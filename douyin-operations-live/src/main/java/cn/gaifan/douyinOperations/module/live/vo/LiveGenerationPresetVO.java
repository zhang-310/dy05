package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import java.sql.Timestamp;

/**
 * 生成配置预设响应 VO
 */
@Data
public class LiveGenerationPresetVO {

    private Long id;
    private String name;
    private String description;
    private String style;
    private Long modelId;
    private Boolean useKbRef;
    private String durationMode;
    private String hotKeywords;
    private String ipType;
    private String materialType;
    private String scriptModule;
    private String retentionStrategy;
    private String interactionLevel;
    private Boolean isDefault;
    private Long ownerId;
    private Timestamp createTime;
    private Timestamp updateTime;
}
