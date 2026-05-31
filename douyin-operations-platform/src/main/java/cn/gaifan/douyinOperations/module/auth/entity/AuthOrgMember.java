package cn.gaifan.douyinOperations.module.auth.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 机构成员表，与 sql/auth/organization.sql 中 auth_org_member 对应
 */
@Getter
@Setter
@Entity
@Table(name = "auth_org_member")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class AuthOrgMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role_in_org", length = 32)
    private String roleInOrg = "member";

    /** 0=待确认 1=已加入 2=已拒绝 */
    @Column(name = "status", nullable = false)
    private Integer status = 0;

    @Column(name = "invited_at")
    private Timestamp invitedAt;

    @Column(name = "joined_at")
    private Timestamp joinedAt;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @PrePersist
    public void prePersist() {
        if (invitedAt == null) invitedAt = new Timestamp(System.currentTimeMillis());
        if (createTime == null) createTime = new Timestamp(System.currentTimeMillis());
        if (updateTime == null) updateTime = new Timestamp(System.currentTimeMillis());
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
