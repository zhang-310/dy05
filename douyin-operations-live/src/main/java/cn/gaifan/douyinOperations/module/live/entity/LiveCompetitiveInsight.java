package cn.gaifan.douyinOperations.module.live.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "live_competitive_insight")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class LiveCompetitiveInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "competitor_label", nullable = false, length = 128)
    private String competitorLabel;

    @Column(name = "product_price", precision = 12, scale = 2)
    private BigDecimal productPrice;

    @Column(name = "market_share_percent", precision = 6, scale = 2)
    private BigDecimal marketSharePercent;

    @Column(name = "gmv_estimate", precision = 14, scale = 2)
    private BigDecimal gmvEstimate;

    @Column(name = "win_loss_notes", columnDefinition = "TEXT")
    private String winLossNotes;

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
            updateTime = createTime;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
