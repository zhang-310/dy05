package cn.gaifan.douyinOperations.module.product.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 产品话术版本 VO（响应）
 */
@Data
public class ProductScriptVO {

    /** 话术 ID */
    private Long id;

    /** 产品 ID */
    private Long productId;

    /** 版本号 */
    private Integer versionNumber;

    /** 话术内容 */
    private String content;

    /** 话术风格 */
    private String style;

    /** 效果评分（0-100） */
    private BigDecimal effectivenessScore;

    /** 使用次数 */
    private Integer usageCount;

    /** 是否为当前版本：1=当前 0=历史 */
    private Integer isActive;

    /** 变更原因 */
    private String changeReason;

    /** 编辑者 ID */
    private Long editorId;

    /** 编辑者名称 */
    private String editorName;

    /** 创建时间 */
    private Timestamp createTime;

    /** 更新时间 */
    private Timestamp updateTime;

    /** 推荐分数（用于推荐算法，transient） */
    private Double recommendScore;
}
