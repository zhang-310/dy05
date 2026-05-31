package cn.gaifan.douyinOperations.module.system.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 全站分类节点（A-2 taxonomy）；与 {@code sys_taxonomy_node} 对应。
 */
@Getter
@Setter
@Entity
@Table(name = "sys_taxonomy_node")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class SysTaxonomyNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 0 = 平台预置；后续可扩展租户私有节点 */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId = 0L;

    @Column(name = "module_scope", nullable = false, length = 64)
    private String moduleScope;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "enabled", nullable = false)
    private Integer enabled = 1;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) {
            createTime = new Timestamp(System.currentTimeMillis());
        }
        if (updateTime == null) {
            updateTime = new Timestamp(System.currentTimeMillis());
        }
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
