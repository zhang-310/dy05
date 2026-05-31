package cn.gaifan.douyinOperations.module.shortvideo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "sv_story_formula_template")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class SvStoryFormulaTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 256)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "formula_json", columnDefinition = "TEXT")
    private String formulaJson;

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
