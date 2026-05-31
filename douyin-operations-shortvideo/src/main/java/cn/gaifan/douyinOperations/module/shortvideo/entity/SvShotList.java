package cn.gaifan.douyinOperations.module.shortvideo.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 分镜列表表，与 sql/shortvideo/migration-bos-production.sql 中 sv_shot_list 对应
 */
@Getter
@Setter
@Entity
@Table(name = "sv_shot_list")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class SvShotList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "script_id", nullable = false)
    private Long scriptId;

    @Column(name = "shot_count", nullable = false)
    private Integer shotCount;

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
