package cn.gaifan.douyinOperations.module.ai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * 增量导入时间戳（按路径只处理新/改文件）
 */
@Getter
@Setter
@Entity
@Table(name = "kb_import_checkpoint")
@org.hibernate.annotations.SQLRestriction("deleted = 0")
@NoArgsConstructor
public class KbImportCheckpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kb_id", nullable = false)
    private Long kbId;

    @Column(name = "source_path", nullable = false, length = 500)
    private String sourcePath;

    @Column(name = "last_import_time", nullable = false)
    private Timestamp lastImportTime;

    @Column(name = "file_count")
    private Integer fileCount = 0;

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
