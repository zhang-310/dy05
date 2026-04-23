package cn.gaifan.douyinOperations.module.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品话术引用快照实体类
 * 记录直播场次中引用的话术快照
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Entity
@Table(name = "product_script_snapshot", indexes = {
    @Index(name = "idx_product_script_snapshot_live_session_id", columnList = "live_session_id"),
    @Index(name = "idx_product_script_snapshot_product_script_version_id", columnList = "product_script_version_id")
})
@SQLRestriction("deleted = 0")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductScriptSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 直播场次 ID
     */
    @Column(name = "live_session_id", nullable = false)
    private Long liveSessionId;

    /**
     * 话术版本 ID
     */
    @Column(name = "product_script_version_id", nullable = false)
    private Long productScriptVersionId;

    /**
     * 快照内容（引用时的内容）
     */
    @Column(name = "content_snapshot", columnDefinition = "TEXT NOT NULL")
    private String contentSnapshot;

    /**
     * 引用时间
     */
    @Column(name = "referenced_at")
    private LocalDateTime referencedAt;

    /**
     * 数据隔离：所有者 ID
     */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 逻辑删除标记（0=未删除, 1=已删除）
     */
    @Column(name = "deleted", nullable = false)
    private Integer deleted;

    /**
     * 自动维护创建时间
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.referencedAt == null) {
            this.referencedAt = LocalDateTime.now();
        }
        if (this.deleted == null) {
            this.deleted = 0;
        }
    }
}
