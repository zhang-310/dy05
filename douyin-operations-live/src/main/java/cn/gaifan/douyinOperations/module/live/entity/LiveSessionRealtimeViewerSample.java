package cn.gaifan.douyinOperations.module.live.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * 直播实时在线人数采样（与实时面板 ingest 同源，GMV-02：≥2 条可参与完播/留存推算）
 */
@Entity
@Table(name = "live_session_realtime_viewer_sample", indexes = {
        @Index(name = "idx_lsrvs_session_sampled", columnList = "live_session_id,sampled_at")
})
@Data
@NoArgsConstructor
@SQLRestriction("deleted = 0")
public class LiveSessionRealtimeViewerSample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "live_session_id", nullable = false)
    private Long liveSessionId;

    @Column(name = "viewer_count", nullable = false)
    private Integer viewerCount = 0;

    @Column(name = "sampled_at", nullable = false)
    private LocalDateTime sampledAt;

    /** 来源，如 douyin_ingest */
    @Column(name = "source", length = 32)
    private String source;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @PrePersist
    void prePersist() {
        if (sampledAt == null) {
            sampledAt = LocalDateTime.now();
        }
        if (deleted == null) {
            deleted = 0;
        }
        if (viewerCount == null) {
            viewerCount = 0;
        }
    }
}
