package cn.gaifan.douyinOperations.module.shortvideo.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 热点话题表，与 sql/shortvideo/schema.sql 中 sv_hot_topic 一一对应
 * 注意：无 deleted 字段，热点话题不支持逻辑删除
 */
@Getter
@Setter
@Entity
@Table(name = "sv_hot_topic")
@NoArgsConstructor
public class SvHotTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source", nullable = false, length = 32)
    private String source = "manual";  // manual / douyin / weibo

    @Column(name = "douyin_hot_id", length = 128)
    private String douyinHotId;

    @Column(name = "title", nullable = false, length = 256)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "heat_score")
    private Long heatScore = 0L;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "related_tags", length = 512)
    private String relatedTags;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "active";  // active / expired

    @Column(name = "expiry_time")
    private Timestamp expiryTime;

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
