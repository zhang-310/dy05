package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 直播模块本地人设快照 VO：解耦 live ↔ douyin Entity 直接引用。
 * 从 DyPersona 实体字段映射而来，仅包含直播话术 prompt 构建所需的字段。
 */
@Data
@Builder
public class LivePersonaSnapshotVO {
    private Long id;
    private String personaName;
    private String tone;
    private String localFlavor;
    private String personaTraits;
    private String ipType;
    private String ageRange;
    private String positioningTags;
    private String liveStyle;
    private String contentRatio;
}
