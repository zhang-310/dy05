package cn.gaifan.douyinOperations.module.live.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import java.sql.Timestamp;

/**
 * 直播竞品话术库（C-4），按 owner 隔离。
 */
@Data
@Entity
@Table(name = "live_competitor_script")
@SQLRestriction("deleted = 0")
public class LiveCompetitorScript {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 256)
    private String title;

    @Column(name = "competitor_name", length = 128)
    private String competitorName;

    @Column(length = 32)
    private String platform = "douyin";

    @Column(name = "script_content", nullable = false, columnDefinition = "TEXT")
    private String scriptContent;

    @Column(name = "source_url", length = 1024)
    private String sourceUrl;

    @Column(length = 256)
    private String tags;

    @Column(length = 512)
    private String notes;

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
