package cn.gaifan.douyinOperations.module.copy.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 文案库表
 * 与 sql/copy/schema.sql 中 copy_library 一一对应
 */
@Data
@Entity
@Table(name = "copy_library")
@SQLRestriction("deleted = 0")
public class CopyLibrary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = 256)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "tags", length = 512)
    private String tags;

    @Column(name = "word_count")
    private Integer wordCount = 0;

    @Column(name = "use_count", nullable = false)
    private Integer useCount = 0;

    @Column(name = "rating")
    private Integer rating;

    /** 状态：0=待审核 1=已审核 */
    @Column(name = "status", nullable = false)
    private Integer status = 0;

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
