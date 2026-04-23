package cn.gaifan.douyinOperations.module.shortvideo.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;

import java.sql.Timestamp;

/**
 * 短剧角色表 (Phase 3)
 * 角色参考图用于跨集一致性
 */
@Data
@Entity
@Table(name = "sv_drama_character")
@SQLRestriction("deleted = 0")
public class SvDramaCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "drama_id", nullable = false)
    private Long dramaId;

    @Column(name = "character_name", nullable = false, length = 100)
    private String characterName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "reference_image_url", length = 500)
    private String referenceImageUrl;

    @Column(name = "reference_bos_key", length = 500)
    private String referenceBosKey;

    @Column(name = "voice_id", length = 100)
    private String voiceId;

    /** 多参考图 JSON 数组 [{url,bos_key}]，用于角色一致性 */
    @Column(name = "reference_images", columnDefinition = "jsonb")
    private String referenceImages;

    @Column(name = "lora_model_path", length = 500)
    private String loraModelPath;

    @Column(name = "prompt_tags", length = 500)
    private String promptTags;

    @Column(name = "voice_sample_url", length = 500)
    private String voiceSampleUrl;

    @Column(name = "cloned_voice_id", length = 100)
    private String clonedVoiceId;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

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
