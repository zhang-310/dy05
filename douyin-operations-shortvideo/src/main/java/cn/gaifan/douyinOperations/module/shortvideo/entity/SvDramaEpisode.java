package cn.gaifan.douyinOperations.module.shortvideo.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import java.sql.Timestamp;

/**
 * 短剧剧集表 (Phase 3)
 * 每集关联一个 SvProject
 */
@Data
@Entity
@Table(name = "sv_drama_episode")
@SQLRestriction("deleted = 0")
public class SvDramaEpisode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "drama_id", nullable = false)
    private Long dramaId;

    @Column(name = "episode_number", nullable = false)
    private Integer episodeNumber;

    @Column(length = 200)
    private String title;

    @Column(name = "project_id")
    private Long projectId;

    @Column(columnDefinition = "TEXT")
    private String synopsis;

    @Column(columnDefinition = "TEXT")
    private String cliffhanger;

    @Column(length = 20)
    private String status = "draft";

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
        if (updateTime == null) updateTime = createTime;
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
