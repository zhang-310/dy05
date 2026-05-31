package cn.gaifan.douyinOperations.module.ai.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 进化报告表
 */
@Getter
@Setter
@Entity
@Table(name = "ai_evolve_report")
@NoArgsConstructor
public class AiEvolveReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "report_title", length = 256)
    private String reportTitle;

    @Column(name = "methodology_section", columnDefinition = "TEXT")
    private String methodologySection;

    @Column(name = "deepen_section", columnDefinition = "TEXT")
    private String deepenSection;

    @Column(name = "iterate_section", columnDefinition = "TEXT")
    private String iterateSection;

    @Column(name = "full_content", nullable = false, columnDefinition = "TEXT")
    private String fullContent;

    @Column(name = "methodology_count")
    private Integer methodologyCount = 0;

    @Column(name = "deepen_count")
    private Integer deepenCount = 0;

    @Column(name = "has_failure_case", nullable = false)
    private Integer hasFailureCase = 0;

    @Column(name = "has_sop", nullable = false)
    private Integer hasSop = 0;

    @Column(name = "has_benchmark", nullable = false)
    private Integer hasBenchmark = 0;

    @Column(name = "index_status", nullable = false, length = 16)
    private String indexStatus = "pending";

    @Column(name = "indexed_time")
    private Timestamp indexedTime;

    @Column(name = "create_time")
    private Timestamp createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
    }
}
