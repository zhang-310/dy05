package cn.gaifan.douyinOperations.module.benchmark.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 脚本相似度索引实体
 */
@Getter
@Setter
@Entity
@Table(name = "benchmark_script_similarity")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class BenchmarkScriptSimilarity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "source_script_id", nullable = false)
    private Long sourceScriptId;

    @Column(name = "target_script_id", nullable = false)
    private Long targetScriptId;

    @Column(name = "similarity_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal similarityScore;

    @Column(name = "similarity_type", length = 20)
    private String similarityType = "semantic";

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "deleted")
    private Integer deleted = 0;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
