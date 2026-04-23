package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 效果评分权重配置 - 返回值
 */
@Data
public class LiveEffectivenessConfigVO {

    private Long id;
    private Long userId;
    private Long ownerId;
    private String configName;
    private BigDecimal conversionWeight;
    private BigDecimal interactionWeight;
    private BigDecimal retentionWeight;
    private BigDecimal gmvWeight;
    private BigDecimal viewerWeight;
    private Integer isDefault;
    private Timestamp createTime;
    private Timestamp updateTime;

    /**
     * 从实体转换
     */
    public static LiveEffectivenessConfigVO fromEntity(
            cn.gaifan.douyinOperations.module.live.entity.LiveEffectivenessConfig entity) {
        LiveEffectivenessConfigVO vo = new LiveEffectivenessConfigVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setOwnerId(entity.getOwnerId());
        vo.setConfigName(entity.getConfigName());
        vo.setConversionWeight(entity.getConversionWeight());
        vo.setInteractionWeight(entity.getInteractionWeight());
        vo.setRetentionWeight(entity.getRetentionWeight());
        vo.setGmvWeight(entity.getGmvWeight());
        vo.setViewerWeight(entity.getViewerWeight());
        vo.setIsDefault(entity.getIsDefault());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
