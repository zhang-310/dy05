package cn.gaifan.douyinOperations.module.ai.entity;

import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Map;

/**
 * 进化报告表
 * 存储进化报告持久化数据
 */
@Data
@Entity
@Table(name = "ai_knowledge_evolution_report")
@SQLRestriction("deleted = 0")
public class KnowledgeEvolutionReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType;

    @Column(name = "period_start", nullable = false)
    private Date periodStart;

    @Column(name = "period_end", nullable = false)
    private Date periodEnd;

    @Column(name = "total_in_library")
    private Integer totalInLibrary = 0;

    @Column(name = "new_added_count")
    private Integer newAddedCount = 0;

    @Column(name = "archived_count")
    private Integer archivedCount = 0;

    @Column(name = "deduplication_count")
    private Integer deduplicationCount = 0;

    @Column(name = "average_quality_score", precision = 5, scale = 2)
    private BigDecimal averageQualityScore;

    @Column(name = "trend", length = 20)
    private String trend;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "report_content", columnDefinition = "JSONB")
    private Map<String, Object> reportContent;

    @Column(name = "generated_by", length = 255)
    private String generatedBy = "system";

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

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
