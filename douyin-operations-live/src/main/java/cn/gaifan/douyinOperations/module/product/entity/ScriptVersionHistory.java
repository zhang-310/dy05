package cn.gaifan.douyinOperations.module.product.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 产品话术版本历史表
 */
@Data
@Entity
@Table(name = "script_version_history")
@SQLRestriction("deleted = 0")
public class ScriptVersionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "script_id", nullable = false)
    private Long scriptId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "script_type", nullable = false, length = 32)
    private String scriptType;

    @Column(name = "style", length = 64)
    private String style;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "script_content", nullable = false, columnDefinition = "TEXT")
    private String scriptContent;

    @Column(name = "persona_id")
    private Long personaId;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
    }
}
