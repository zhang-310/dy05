package cn.gaifan.douyinOperations.module.live.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * 直播话术段落实体
 * 记录直播场次中的话术段落信息，支持话术导航、倒计时和完成状态跟踪
 */
@Entity
@Table(name = "live_session_script_slot", indexes = {
    @Index(name = "idx_live_session_script_slot_session_id", columnList = "live_session_id"),
    @Index(name = "idx_live_session_script_slot_session_slot", columnList = "live_session_id, slot_index"),
    @Index(name = "idx_live_session_script_slot_is_current", columnList = "live_session_id, is_current"),
    @Index(name = "idx_live_session_script_slot_owner_id", columnList = "owner_id")
})
@SQLRestriction("deleted = 0")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LiveSessionScriptSlot {

    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 直播场次 ID
     */
    @Column(name = "live_session_id", nullable = false)
    private Long liveSessionId;

    /**
     * 段落序号（从 0 开始）
     */
    @Column(name = "slot_index", nullable = false)
    private Integer slotIndex;

    /**
     * 话术版本 ID（可关联 ProductScriptVersion）
     */
    @Column(name = "script_version_id")
    private Long scriptVersionId;

    /**
     * 话术内容
     */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * 建议讲解时长（秒）
     */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /**
     * 话术类型
     * 取值: opening（开场）、product（产品）、discount（优惠）、closing（结尾）、emotional（情感价值）
     */
    @Column(name = "script_type", length = 32)
    private String scriptType;

    /**
     * 话术风格
     * 取值: enthusiastic（激情）、professional（专业）、gentle（温柔）、humorous（幽默）
     */
    @Column(name = "style", length = 64)
    private String style;

    /**
     * 是否当前段落
     */
    @Column(name = "is_current")
    private Boolean isCurrent;

    /**
     * 是否已讲解完成
     */
    @Column(name = "is_completed")
    private Boolean isCompleted;

    /**
     * 开始讲解时间
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * 完成讲解时间
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * 所有者 ID（数据隔离）
     */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除标记
     */
    @Column(name = "deleted", nullable = false)
    private Integer deleted;

    /**
     * 插入前自动设置创建和更新时间
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        deleted = 0;
        if (durationSeconds == null) {
            durationSeconds = 120;
        }
        if (isCurrent == null) {
            isCurrent = false;
        }
        if (isCompleted == null) {
            isCompleted = false;
        }
    }

    /**
     * 更新前自动更新修改时间
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
