package cn.gaifan.douyinOperations.module.ai.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 知识反馈表：用户对检索结果的评价，用于更新 boost_factor
 */
@Data
@Entity
@Table(name = "kb_feedback")
public class KbFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "query", nullable = false, length = 512)
    private String query;

    @Column(name = "doc_id", nullable = false, length = 128)
    private String docId;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", length = 512)
    private String comment;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "search_mode", length = 16)
    private String searchMode;

    @Column(name = "create_time")
    private Timestamp createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
    }
}
