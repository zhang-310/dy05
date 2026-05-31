package cn.gaifan.douyinOperations.module.ai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "ai_model_benchmark")
@NoArgsConstructor
public class AiModelBenchmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_id", nullable = false)
    private Long modelId;

    @Column(name = "task_code", nullable = false, length = 64)
    private String taskCode;

    @Column(name = "latency_ms", nullable = false)
    private Long latencyMs;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "create_time")
    private Timestamp createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
    }
}
