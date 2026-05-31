package cn.gaifan.douyinOperations.module.ai.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * AI 语音合成记录表
 */
@Getter
@Setter
@Entity
@Table(name = "ai_tts_generation")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class AiTtsGeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    @Column(name = "voice", length = 64)
    private String voice;

    @Column(name = "language", length = 16)
    private String language;

    @Column(name = "audio_url", length = 512)
    private String audioUrl;

    @Column(name = "duration")
    private Long duration;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "status", nullable = false)
    private Integer status = 0;

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
