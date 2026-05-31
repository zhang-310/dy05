package cn.gaifan.douyinOperations.module.live.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "live_style_preset")
@NoArgsConstructor
public class LiveStylePreset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "style_key", nullable = false, unique = true, length = 50)
    private String styleKey;

    @Column(name = "label", nullable = false, length = 50)
    private String label;

    @Column(name = "group_name", nullable = false, length = 30)
    private String groupName;

    @Column(name = "prompt_template", nullable = false, columnDefinition = "TEXT")
    private String promptTemplate;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "active", nullable = false)
    private Integer active = 1;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
