package cn.gaifan.douyinOperations.module.shortvideo.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import java.sql.Timestamp;

@Data
@Entity
@Table(name = "sv_ops_report_template")
@SQLRestriction("deleted = 0")
public class SvOpsReportTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 256)
    private String name;

    @Column(name = "definition_json", columnDefinition = "TEXT")
    private String definitionJson;

    @Column(nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) {
            createTime = new Timestamp(System.currentTimeMillis());
        }
        if (updateTime == null) {
            updateTime = new Timestamp(System.currentTimeMillis());
        }
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
