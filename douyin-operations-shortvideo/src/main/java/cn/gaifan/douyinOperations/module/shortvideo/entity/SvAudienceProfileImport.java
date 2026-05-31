package cn.gaifan.douyinOperations.module.shortvideo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.sql.Timestamp;

/**
 * N-3：受众画像 CSV/第三方导入快照（与 V093 sv_audience_profile_import 对应）
 */
@Getter
@Setter
@Entity
@Table(name = "sv_audience_profile_import")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class SvAudienceProfileImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "data_source", nullable = false, length = 64)
    private String dataSource = "third_party_csv";

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "row_count", nullable = false)
    private Integer rowCount = 0;

    @Column(name = "create_time", nullable = false)
    private Timestamp createTime;

    @Column(name = "update_time", nullable = false)
    private Timestamp updateTime;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @PrePersist
    public void prePersist() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (createTime == null) {
            createTime = now;
        }
        if (updateTime == null) {
            updateTime = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
