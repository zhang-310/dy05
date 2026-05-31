package cn.gaifan.douyinOperations.module.shortvideo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "sv_content_calendar")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class SvContentCalendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "persona_id")
    private Long personaId;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @Column(name = "content_type", length = 32, nullable = false)
    private String contentType;

    @Column(name = "title", length = 256)
    private String title;

    @Column(name = "brief", columnDefinition = "TEXT")
    private String brief;

    @Column(name = "script_id")
    private Long scriptId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "shooting_task_id")
    private Long shootingTaskId;

    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Column(name = "priority", nullable = false)
    private Integer priority = 0;

    @Column(name = "publish_time", length = 16)
    private String publishTime;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
