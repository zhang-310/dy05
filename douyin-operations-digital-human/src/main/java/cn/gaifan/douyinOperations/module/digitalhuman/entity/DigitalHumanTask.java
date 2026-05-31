package cn.gaifan.douyinOperations.module.digitalhuman.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import java.sql.Timestamp;

@Getter @Setter
@Entity @Table(name = "digital_human_task")
@SQLRestriction("deleted = 0")
@NoArgsConstructor
public class DigitalHumanTask {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "script_content", columnDefinition = "TEXT") private String scriptContent;
    @Column(name = "voice_type", length = 32) private String voiceType = "default";
    @Column(name = "avatar_id") private Long avatarId;
    @Column(name = "status", length = 16) private String status = "pending";
    @Column(name = "output_url", length = 512) private String outputUrl;
    @Column(name = "error_message", length = 512) private String errorMessage;
    @Column(name = "progress") private Integer progress = 0;
    @Column(name = "cost_credits") private Long costCredits = 0L;
    @Column(name = "deleted") private Integer deleted = 0;
    @Column(name = "create_time") private Timestamp createTime;
    @Column(name = "update_time") private Timestamp updateTime;
    @PrePersist public void prePersist() { if(createTime==null)createTime=new Timestamp(System.currentTimeMillis());if(updateTime==null)updateTime=new Timestamp(System.currentTimeMillis());}
    @PreUpdate public void preUpdate() { updateTime=new Timestamp(System.currentTimeMillis());}
}
