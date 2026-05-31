package cn.gaifan.douyinOperations.module.abtest.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class AbVariantVO {
    private Long id;
    private Long experimentId;
    private String variantName;
    private String variantType;
    private String content;
    private String entityType;
    private Long entityId;
    private String styleCode;
    private Long viewCount;
    private Long clickCount;
    private Long conversionCount;
    private BigDecimal conversionRate;
    private Integer isWinner;
    private Timestamp createTime;
    private Timestamp updateTime;
}
