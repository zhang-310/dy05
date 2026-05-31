package cn.gaifan.douyinOperations.module.ai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * 进化/实验适应度记录（与 V145__evolution_fitness_record.sql 对应）
 */
@Getter
@Setter
@Entity
@Table(name = "evolution_fitness_record")
@NoArgsConstructor
public class EvolutionFitnessRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kb_id")
    private Long kbId;

    @Column(name = "task_id", nullable = false, length = 64)
    private String taskId;

    @Column(name = "parent_task_id", length = 64)
    private String parentTaskId;

    @Column(name = "metric_name", nullable = false, length = 64)
    private String metricName;

    @Column(name = "metric_value")
    private Double metricValue;

    @Column(name = "payload_json", columnDefinition = "text")
    private String payloadJson;

    /** 可选：AB/实验 ID（V146） */
    @Column(name = "experiment_id", length = 64)
    private String experimentId;

    @Column(name = "create_time", nullable = false)
    private Timestamp createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) {
            createTime = new Timestamp(System.currentTimeMillis());
        }
    }
}
