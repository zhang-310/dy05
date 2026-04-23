package cn.gaifan.douyinOperations.module.product.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品话术版本返回值对象
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductScriptVersionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    private Long id;

    /**
     * 关联产品 ID
     */
    private Long productId;

    /**
     * 关联话术 ID
     */
    private Long scriptId;

    /**
     * 版本号
     */
    private Integer versionNumber;

    /**
     * 话术内容
     */
    private String content;

    /**
     * 话术风格
     */
    private String style;

    /**
     * 效果评分
     */
    private BigDecimal effectivenessScore;

    /**
     * 使用次数
     */
    private Integer usageCount;

    /**
     * 转化率
     */
    private BigDecimal conversionRate;

    /**
     * 点赞数
     */
    private Integer likesCount;

    /**
     * 评论数
     */
    private Integer commentsCount;

    /**
     * 是否启用
     */
    private Boolean isActive;

    /**
     * 是否推荐
     */
    private Boolean isRecommended;

    /**
     * 是否归档
     */
    private Boolean archived;

    /**
     * 所有者 ID
     */
    private Long ownerId;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updatedAt;
}
