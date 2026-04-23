package cn.gaifan.douyinOperations.module.storage.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * BOS 文件元数据表
 * 用于追溯每个文件的成本、用途、使用次数
 */
@Data
@Entity
@Table(name = "bos_file_metadata")
@SQLRestriction("deleted = 0")
public class BosFileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bos_key", nullable = false, length = 500)
    private String bosKey;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "file_size")
    private Long fileSize = 0L;

    @Column(name = "storage_cost_monthly", precision = 10, scale = 4)
    private BigDecimal storageCostMonthly = BigDecimal.ZERO;

    @Column(name = "usage_count")
    private Integer usageCount = 0;

    @Column(name = "shot_id")
    private Long shotId;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @PrePersist
    public void prePersist() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (createTime == null) createTime = now;
        if (updateTime == null) updateTime = now;
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
