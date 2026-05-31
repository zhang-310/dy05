package cn.gaifan.douyinOperations.module.drama.entity;

import jakarta.persistence.*;import lombok.Getter; import lombok.Setter; import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction; import java.sql.Timestamp;

@Getter @Setter @Entity @Table(name = "drama_project")
@SQLRestriction("deleted = 0") @NoArgsConstructor
public class DramaProject {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long userId;
    @Column(length = 512) private String description;
    @Column(length = 64) private String genre = "other";
    @Column(length = 256) private String title;
    @Column(length = 32) private String status = "draft";
    @Column(columnDefinition = "TEXT") private String script;
    @Column private Integer episodeCount = 0;
    @Column(length = 16) private String visibility = "private";
    @Column private Long costCredits = 0L;
    @Column private Integer deleted = 0;
    @Column private Timestamp createTime;
    @Column private Timestamp updateTime;
    @PrePersist public void prePersist(){if(createTime==null)createTime=new Timestamp(System.currentTimeMillis());if(updateTime==null)updateTime=new Timestamp(System.currentTimeMillis());}
    @PreUpdate public void preUpdate(){updateTime=new Timestamp(System.currentTimeMillis());}
}
