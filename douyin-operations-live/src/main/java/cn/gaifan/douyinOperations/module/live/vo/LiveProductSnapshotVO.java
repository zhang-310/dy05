package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 直播模块本地产品快照 VO：解耦 live ↔ product Entity 直接引用。
 * 从 DyProduct 实体字段映射而来，仅包含直播话术生成和排品所需的字段。
 */
@Data
@Builder
public class LiveProductSnapshotVO {
    private Long id;
    private String productName;
    private String description;
    private BigDecimal price;
    private BigDecimal costPrice;
    private String productCategory;
    private String imageUrl;
    private String aiSellingPoints;
    private BigDecimal profitMarginPct;
    private BigDecimal lossPerUnit;
    private String controlStrategy;
}
