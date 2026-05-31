package cn.gaifan.douyinOperations.module.photoavatar.entity;

import jakarta.persistence.*;import lombok.Getter;import lombok.Setter;import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;import java.sql.Timestamp;

@Getter @Setter @Entity @Table(name = "photo_avatar_task")
@SQLRestriction("deleted = 0") @NoArgsConstructor
public class PhotoAvatarTask {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long userId;
    @Column(length = 512) private String photoUrl;
    @Column(length = 32) private String outfitStyle;
    @Column(length = 32) private String background;
    @Column(length = 16) private String status = "pending";
    @Column(length = 512) private String outputUrl;
    @Column(length = 512) private String errorMessage;
    @Column private Integer progress = 0;
    @Column private Long costCredits = 0L;
    @Column private Integer deleted = 0;
    @Column private Timestamp createTime;
    @Column private Timestamp updateTime;
    @PrePersist public void prePersist(){if(createTime==null)createTime=new Timestamp(System.currentTimeMillis());if(updateTime==null)updateTime=new Timestamp(System.currentTimeMillis());}
    @PreUpdate public void preUpdate(){updateTime=new Timestamp(System.currentTimeMillis());}
}
