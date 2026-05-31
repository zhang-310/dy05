package cn.gaifan.douyinOperations.module.digitalhuman.vo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class DigitalHumanTaskVO {
    private Long id;
    private Long userId;
    private String scriptContent;
    private String voiceType;
    private Long avatarId;
    private String status;
    private String outputUrl;
    private String errorMessage;
    private Integer progress;
    private Long costCredits;
    private Timestamp createTime;
    private Timestamp updateTime;
}
