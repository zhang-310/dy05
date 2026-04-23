package cn.gaifan.douyinOperations.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 审计日志实体
 * 记录系统中所有的操作日志
 *
 * @author gaifan
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_action", columnList = "action"),
        @Index(name = "idx_create_time", columnList = "create_time"),
        @Index(name = "idx_username_created", columnList = "username,create_time")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 操作用户ID
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * 操作用户名
     */
    @Column(nullable = false, length = 100)
    private String username;

    /**
     * 操作类型: CREATE, UPDATE, DELETE, LOGIN, LOGOUT
     */
    @Column(nullable = false, length = 50)
    private String action;

    /**
     * 操作的实体类型
     */
    @Column(nullable = false, length = 100)
    private String entity;

    /**
     * 操作的实体ID
     */
    @Column(nullable = false)
    private Long entityId;

    /**
     * 修改前的值
     */
    @Column(columnDefinition = "TEXT")
    private String oldValue;

    /**
     * 修改后的值
     */
    @Column(columnDefinition = "TEXT")
    private String newValue;

    /**
     * 客户端IP地址
     */
    @Column(nullable = false, length = 50)
    private String ip;

    /**
     * User-Agent
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String userAgent;

    /**
     * 创建时间
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 操作状态: 1=成功, 0=失败
     */
    @Column(nullable = false)
    private Integer status;

    /**
     * 错误消息（如果操作失败）
     */
    @Column(columnDefinition = "TEXT")
    private String errorMsg;

    /**
     * 创建前置事件
     */
    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
    }
}
