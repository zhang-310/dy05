package cn.gaifan.douyinOperations.module.shortvideo.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 发布时间分析表，与 sql/shortvideo/schema.sql 中 sv_publish_time_analysis 一一对应
 * 注意：无 deleted 字段，分析数据不支持逻辑删除
 */
@Data
@Entity
@Table(name = "sv_publish_time_analysis")
public class SvPublishTimeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;  // 1=周一 ... 7=周日

    @Column(name = "hour_of_day", nullable = false)
    private Integer hourOfDay;  // 0-23

    @Column(name = "avg_view_count")
    private Long avgViewCount = 0L;

    @Column(name = "video_count")
    private Integer videoCount = 0;

    @Column(name = "recommended")
    private Boolean recommended = false;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (updateTime == null) updateTime = new Timestamp(System.currentTimeMillis());
    }
}
