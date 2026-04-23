package cn.gaifan.douyinOperations.module.script.entity;

import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 话术库表
 * 与 sql/script/schema.sql 中 script_library 一一对应
 */
@Data
@Entity
@Table(name = "script_library")
@SQLRestriction("deleted = 0")
public class ScriptLibrary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = 256)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "category", length = 32)
    private String category;

    /** 来源：manual=手动创建 live=从直播保存 ai=AI生成 */
    @Column(name = "source", length = 16)
    private String source = "manual";

    /** 来源ID（如直播场次ID） */
    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    @Column(name = "use_count", nullable = false)
    private Long useCount = 0L;

    /** 状态：0=禁用 1=有效 */
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
