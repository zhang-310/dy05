package cn.gaifan.douyinOperations.module.shortvideo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * 短视频字幕段。
 */
@Getter
@Setter
@Entity
@Table(name = "sv_subtitle_segment", indexes = {
        @Index(name = "idx_sv_subtitle_owner_video", columnList = "owner_id, video_id"),
        @Index(name = "idx_sv_subtitle_video_start", columnList = "video_id, start_time")
})
@NoArgsConstructor
public class SvSubtitleSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "segment_key", nullable = false, length = 64)
    private String segmentKey;

    @Column(name = "start_time", nullable = false)
    private Double startTime;

    @Column(name = "end_time", nullable = false)
    private Double endTime;

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "font_size")
    private Integer fontSize;

    @Column(name = "color", length = 32)
    private String color;

    @Column(name = "font_family", length = 128)
    private String fontFamily;

    @Column(name = "position", length = 32)
    private String position;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time", nullable = false)
    private Timestamp createTime;

    @Column(name = "update_time", nullable = false)
    private Timestamp updateTime;

    @PrePersist
    public void prePersist() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (createTime == null) createTime = now;
        if (updateTime == null) updateTime = now;
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
