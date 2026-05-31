package cn.gaifan.douyinOperations.module.shortvideo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.sql.Timestamp;

/**
 * 短剧主表 (Phase 3)
 * 类型: 都市/古装/悬疑/甜宠/搞笑
 */
@Getter
@Setter
@Entity
@Table(name = "sv_drama")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class SvDrama {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String genre;

    @Column(name = "total_episodes")
    private Integer totalEpisodes = 1;

    @Column(length = 20)
    private String status = "draft";

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    /** 有剧情的剧集数（仅列表返回时填充，不持久化） */
    @Transient
    private Integer episodesWithSynopsis;
    /** 已关联项目的剧集数（仅列表返回时填充，不持久化） */
    @Transient
    private Integer episodesWithProject;

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
