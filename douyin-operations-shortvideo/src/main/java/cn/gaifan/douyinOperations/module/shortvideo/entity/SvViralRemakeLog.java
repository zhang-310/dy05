package cn.gaifan.douyinOperations.module.shortvideo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "sv_viral_remake_log")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class SvViralRemakeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "viral_video_id", nullable = false)
    private Long viralVideoId;

    @Column(name = "from_status")
    private Integer fromStatus;

    @Column(name = "to_status", nullable = false)
    private Integer toStatus;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = new Timestamp(System.currentTimeMillis());
        }
    }
}
