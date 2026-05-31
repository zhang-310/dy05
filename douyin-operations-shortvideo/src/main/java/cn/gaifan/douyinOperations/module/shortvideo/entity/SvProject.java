package cn.gaifan.douyinOperations.module.shortvideo.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

/**
 * 短视频项目表，与 sql/shortvideo/migration-bos-production.sql 中 sv_project 对应
 * 所有媒体 URL 均为 BOS CDN URL
 */
@Getter
@Setter
@Entity
@Table(name = "sv_project")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class SvProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "project_type", nullable = false, length = 50)
    private String projectType;  // viral_clone/daily/soft_ad

    @Column(name = "persona_id")
    private Long personaId;  // 人设 ID（daily 类型）

    @Column(name = "schedule_date")
    private Date scheduleDate;  // 计划拍摄日期（daily 类型）

    @Column(name = "shoot_status", length = 32)
    private String shootStatus;  // not_started/ready/shooting/shot_done（仅 daily）

    @Column(name = "status", nullable = false, length = 50)
    private String status = "draft";  // draft/processing/completed/failed

    @Column(name = "script_id")
    private Long scriptId;

    @Column(name = "shot_list_id")
    private Long shotListId;

    @Column(name = "final_video_url", length = 500)
    private String finalVideoUrl;  // BOS CDN URL

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;  // BOS CDN URL

    @Column(name = "character_reference_url", length = 500)
    private String characterReferenceUrl;  // 人物参考图 BOS CDN URL（素材准备页上传）

    @Column(name = "scene_reference_url", length = 500)
    private String sceneReferenceUrl;  // 场景参考图 BOS CDN URL（素材准备页上传）

    @Column(name = "duration")
    private Integer duration;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "related_product_ids")
    private List<Long> relatedProductIds;

    @Column(name = "publish_title", length = 255)
    private String publishTitle;

    @Column(name = "publish_platforms", columnDefinition = "TEXT")
    private String publishPlatforms;  // JSON

    @Column(name = "publish_time")
    private Timestamp publishTime;

    @Column(name = "review_status", length = 50)
    private String reviewStatus;

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(name = "review_time")
    private Timestamp reviewTime;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 1;

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
