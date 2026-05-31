package cn.gaifan.douyinOperations.module.auth.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 机构表，与 sql/auth/organization.sql 中 auth_organization 对应
 */
@Getter
@Setter
@Entity
@Table(name = "auth_organization")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class AuthOrganization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_name", nullable = false, length = 128)
    private String orgName;

    @Column(name = "org_code", length = 64)
    private String orgCode;

    @Column(name = "contact_name", length = 64)
    private String contactName;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

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
