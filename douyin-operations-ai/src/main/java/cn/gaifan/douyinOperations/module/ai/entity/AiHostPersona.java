package cn.gaifan.douyinOperations.module.ai.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import java.sql.Timestamp;

/**
 * 五位主播人设配置
 * 肖瑶/肖蝉/阳阳/智慧/田玲红
 */
@Getter
@Setter
@Entity
@Table(name = "ai_host_persona")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class AiHostPersona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "host_code", nullable = false, length = 32)
    private String hostCode;

    @Column(name = "host_name", nullable = false, length = 64)
    private String hostName;

    @Column(name = "age")
    private Integer age;

    /** C=消费者 B=商业 */
    @Column(name = "orientation", nullable = false, length = 16)
    private String orientation = "C";

    @Column(name = "positioning", length = 256)
    private String positioning;

    /** JSON 内容矩阵 */
    @Column(name = "content_matrix", columnDefinition = "TEXT")
    private String contentMatrix;

    /** JSON AI需求优先级 */
    @Column(name = "ai_priorities", columnDefinition = "TEXT")
    private String aiPriorities;

    /** JSON 多模态风格向量 */
    @Column(name = "style_vector", columnDefinition = "TEXT")
    private String styleVector;

    /** JSON 因果推理因子权重 */
    @Column(name = "bayes_factors", columnDefinition = "TEXT")
    private String bayesFactors;

    /** 0=流量入口 1=转化节点 2=B端沉淀 */
    @Column(name = "flow_phase")
    private Integer flowPhase = 0;

    /** 目标品类：护肤品/美妆/综合 */
    @Column(name = "target_category", length = 64)
    private String targetCategory;

    /** GMV档位：新锐/腰部/头部 */
    @Column(name = "target_gmv_tier", length = 32)
    private String targetGmvTier;

    /** 转型阶段：1=品类测试 2=品牌深度 3=头部竞争 */
    @Column(name = "strategy_phase")
    private Integer strategyPhase;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "deleted", nullable = false)
    private Integer deleted = 0;

    @Column(name = "create_time")
    private Timestamp createTime;

    @Column(name = "update_time")
    private Timestamp updateTime;

    @PrePersist
    public void prePersist() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (createTime == null) createTime = now;
        updateTime = now;
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = new Timestamp(System.currentTimeMillis());
    }
}
