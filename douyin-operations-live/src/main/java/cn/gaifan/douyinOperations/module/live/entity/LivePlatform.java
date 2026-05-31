package cn.gaifan.douyinOperations.module.live.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "live_platform")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class LivePlatform {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "platform_code", nullable = false, unique = true, length = 32)
    private String platformCode;

    @Column(name = "platform_name", nullable = false, length = 64)
    private String platformName;

    @Column(name = "icon_url", length = 512)
    private String iconUrl;

    @Column(name = "prompt_template", columnDefinition = "TEXT")
    private String promptTemplate;

    @Column(name = "max_script_length")
    private Integer maxScriptLength = 0;

    @Column(name = "forbidden_topics", columnDefinition = "TEXT")
    private String forbiddenTopics;

    @Column(name = "active", nullable = false)
    private Integer active = 1;

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
