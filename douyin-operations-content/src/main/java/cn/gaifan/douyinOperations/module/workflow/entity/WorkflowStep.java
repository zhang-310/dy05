package cn.gaifan.douyinOperations.module.workflow.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "workflow_step")
@SQLRestriction("deleted = 0")
public class WorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "definition_id", nullable = false)
    private Long definitionId;

    @Column(name = "step_code", nullable = false, length = 64)
    private String stepCode;

    @Column(name = "step_name", length = 128)
    private String stepName;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo = 0;

    @Column(name = "step_config", columnDefinition = "TEXT")
    private String stepConfig;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time", nullable = false)
    private Timestamp createTime;

    @Column(name = "update_time", nullable = false)
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
