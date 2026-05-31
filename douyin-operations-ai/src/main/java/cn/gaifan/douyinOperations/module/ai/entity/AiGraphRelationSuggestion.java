package cn.gaifan.douyinOperations.module.ai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.sql.Timestamp;

/**
 * G-2：图谱关系建议队列（V093 ai_graph_relation_suggestion）
 */
@Getter
@Setter
@Entity
@Table(name = "ai_graph_relation_suggestion")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class AiGraphRelationSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId = 0L;

    @Column(name = "source_entity_key", nullable = false, length = 512)
    private String sourceEntityKey;

    @Column(name = "target_entity_key", nullable = false, length = 512)
    private String targetEntityKey;

    @Column(name = "relation_type", nullable = false, length = 64)
    private String relationType;

    @Column(name = "confidence", nullable = false)
    private Double confidence = 0.0;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "pending";

    @Column(name = "evidence_json", columnDefinition = "TEXT")
    private String evidenceJson;

    @Column(name = "create_time", nullable = false)
    private Timestamp createTime;

    @Column(name = "update_time", nullable = false)
    private Timestamp updateTime;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @PrePersist
    public void prePersist() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (createTime == null) {
            createTime = now;
        }
        if (updateTime == null) {
            updateTime = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
