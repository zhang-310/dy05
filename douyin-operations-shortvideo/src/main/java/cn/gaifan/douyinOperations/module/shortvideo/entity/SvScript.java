package cn.gaifan.douyinOperations.module.shortvideo.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 脚本表，与 sql/shortvideo/migration-bos-production.sql 中 sv_script 对应
 */
@Data
@Entity
@Table(name = "sv_script")
@SQLRestriction("deleted = 0")
public class SvScript {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "script_type", nullable = false, length = 50)
    private String scriptType;  // viral_clone/daily/soft_ad

    @Column(name = "generation_type", length = 50)
    private String generationType;  // ai/manual

    @Column(name = "reference_viral_id")
    private Long referenceViralId;

    @Column(name = "theme", length = 255)
    private String theme;

    @Column(name = "style", length = 50)
    private String style;  // funny/emotional/educational

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;  // JSON

    @Column(name = "ai_prompt", columnDefinition = "TEXT")
    private String aiPrompt;

    @Column(name = "ai_model", length = 100)
    private String aiModel;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
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
