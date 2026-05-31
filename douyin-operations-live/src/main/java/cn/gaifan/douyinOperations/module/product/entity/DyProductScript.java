package cn.gaifan.douyinOperations.module.product.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 产品话术表
 */
@Getter
@Setter
@Entity
@Table(name = "dy_product_script")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class DyProductScript {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 产品ID；情绪价值话术时为 null */
    @Column(name = "product_id")
    private Long productId;

    /** 是否为情绪价值话术 */
    @Column(name = "is_emotional")
    private Boolean isEmotional = false;

    @Column(name = "script_type", nullable = false, length = 32)
    private String scriptType;

    @Column(name = "script_content", nullable = false, columnDefinition = "TEXT")
    private String scriptContent;

    @Column(name = "persona_id")
    private Long personaId;

    @Column(name = "style", length = 32)
    private String style;

    @Column(name = "version")
    private Integer version = 1;

    @Column(name = "is_active")
    private Boolean isActive = false;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "token_usage")
    private Integer tokenUsage;

    @Column(name = "created_by")
    private Long createdBy;

    /** 来源：ai=AI生成 manual=人工编写 import=导入 */
    @Column(name = "source", length = 16)
    private String source = "ai";

    /** 应用场景：short_video/guopin/cangbo/danpin/yubo */
    @Column(name = "scene", length = 32)
    private String scene;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
        if (updateTime == null) updateTime = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
