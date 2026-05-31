package cn.gaifan.douyinOperations.module.ai.entity;

import cn.gaifan.douyinOperations.common.converter.JsonbStringConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * 知识库导入报告（目录/文件上传/增量）
 */
@Getter
@Setter
@Entity
@Table(name = "kb_import_report")
@org.hibernate.annotations.SQLRestriction("deleted = 0")
@NoArgsConstructor
public class KbImportReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kb_id", nullable = false)
    private Long kbId;

    @Column(name = "source_path", length = 500)
    private String sourcePath;

    @Column(name = "import_type", length = 16)
    private String importType;

    @Column(name = "total_files")
    private Integer totalFiles = 0;

    @Column(name = "success_count")
    private Integer successCount = 0;

    @Column(name = "failed_count")
    private Integer failedCount = 0;

    @Column(name = "skipped_count")
    private Integer skippedCount = 0;

    @Column(name = "dedup_skipped")
    private Integer dedupSkipped = 0;

    @Column(name = "dedup_downweighted")
    private Integer dedupDownweighted = 0;

    @Column(name = "new_chunks")
    private Integer newChunks = 0;

    @Column(name = "content_type", length = 16)
    private String contentType;

    @Convert(converter = JsonbStringConverter.class)
    @Column(name = "errors", columnDefinition = "jsonb")
    private String errors;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "user_id", nullable = false)
    private Long userId;

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
