package cn.gaifan.douyinOperations.module.photoavatar.vo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class PhotoAvatarTaskVO {

    private Long id;
    private Long userId;
    private String photoUrl;
    private String outfitStyle;
    private String background;
    private String status;
    private String outputUrl;
    private String errorMessage;
    private Integer progress;
    private Long costCredits;
    private Timestamp createTime;
    private Timestamp updateTime;
}
