package cn.gaifan.douyinOperations.module.ai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "ai_official_knowledge_collect_item")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class AiOfficialKnowledgeCollectItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_type", nullable = false, length = 64)
    private String sourceType = "douyin_school_official";

    @Column(name = "source_site", nullable = false, length = 128)
    private String sourceSite = "school.jinritemai.com";

    @Column(name = "source_url", nullable = false, columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "source_url_hash", nullable = false, length = 64)
    private String sourceUrlHash;

    @Column(name = "source_id", length = 128)
    private String sourceId;

    @Column(name = "title", length = 512)
    private String title;

    @Column(name = "category", length = 128)
    private String category;

    @Column(name = "topic_code", length = 64)
    private String topicCode;

    @Column(name = "target_kb_name", length = 128)
    private String targetKbName;

    @Column(name = "target_kb_id")
    private Long targetKbId;

    @Column(name = "doc_id")
    private Long docId;

    @Column(name = "is_violation", nullable = false)
    private Boolean violation = false;

    @Column(name = "collect_status", nullable = false, length = 32)
    private String collectStatus = "DISCOVERED";

    @Column(name = "index_status", nullable = false, length = 32)
    private String indexStatus = "PENDING";

    @Column(name = "ocr_status", nullable = false, length = 32)
    private String ocrStatus = "NOT_REQUIRED";

    @Column(name = "asr_status", nullable = false, length = 32)
    private String asrStatus = "NOT_REQUIRED";

    @Column(name = "image_count", nullable = false)
    private Integer imageCount = 0;

    @Column(name = "video_count", nullable = false)
    private Integer videoCount = 0;

    @Column(name = "image_text_count", nullable = false)
    private Integer imageTextCount = 0;

    @Column(name = "video_text_count", nullable = false)
    private Integer videoTextCount = 0;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "last_discovered_at")
    private Timestamp lastDiscoveredAt;

    @Column(name = "last_collected_at")
    private Timestamp lastCollectedAt;

    @Column(name = "last_indexed_at")
    private Timestamp lastIndexedAt;

    @Column(name = "last_media_extract_at")
    private Timestamp lastMediaExtractAt;

    @Column(name = "next_retry_at")
    private Timestamp nextRetryAt;

    @Column(name = "last_recollect_at")
    private Timestamp lastRecollectAt;

    @Column(name = "official_update_timestamp")
    private Long officialUpdateTimestamp;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
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
