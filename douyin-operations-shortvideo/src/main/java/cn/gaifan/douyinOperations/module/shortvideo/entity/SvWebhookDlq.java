package cn.gaifan.douyinOperations.module.shortvideo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.Instant;

/** T-5：Webhook 投递耗尽重试后的落库记录（URL 仅存哈希） */
@Getter
@Setter
@Entity
@Table(name = "sv_webhook_dlq")
@NoArgsConstructor
public class SvWebhookDlq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "create_time", nullable = false)
    private Timestamp createTime;

    @PrePersist
    void prePersist() {
        if (createTime == null) {
            createTime = Timestamp.from(Instant.now());
        }
    }

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "webhook_url_sha256", nullable = false, length = 64)
    private String webhookUrlSha256;

    @Column(name = "last_http_status")
    private Integer lastHttpStatus;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "error_preview", length = 512)
    private String errorPreview;

    @Column(name = "event_code", length = 64)
    private String eventCode;
}
