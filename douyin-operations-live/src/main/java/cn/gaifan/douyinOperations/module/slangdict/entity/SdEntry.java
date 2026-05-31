package cn.gaifan.douyinOperations.module.slangdict.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "sd_entry")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class SdEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "phrase", nullable = false, length = 256)
    private String phrase;

    @Column(name = "meaning", length = 512)
    private String meaning;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "usage_scene", length = 128)
    private String usageScene;

    @Column(name = "example", columnDefinition = "TEXT")
    private String example;

    @Column(name = "source", length = 128)
    private String source;

    @Column(name = "use_count")
    private Integer useCount = 0;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

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
