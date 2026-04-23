package cn.gaifan.douyinOperations.module.shortvideo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 场景-运镜推荐映射 (Phase 5 运镜知识库)
 * 表: sv_scene_camera_mapping
 */
@Data
@Entity
@Table(name = "sv_scene_camera_mapping")
public class SvSceneCameraMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scene_keyword", nullable = false, length = 100)
    private String sceneKeyword;

    @Column(name = "recommended_camera", nullable = false, length = 50)
    private String recommendedCamera;

    @Column(name = "confidence", precision = 5, scale = 2)
    private BigDecimal confidence;

    @Column(name = "source", length = 20)
    private String source;

    @Column(name = "create_time")
    private Timestamp createTime;
}
