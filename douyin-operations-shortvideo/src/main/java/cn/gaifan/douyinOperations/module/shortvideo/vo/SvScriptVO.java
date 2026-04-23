package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class SvScriptVO {

    private Long id;
    private Long ownerId;
    private String title;
    private String content;
    private String scriptType;
    private String generationType;
    private Long referenceViralId;
    private String theme;
    private String style;
    private Integer duration;
    private Integer wordCount;
    private String tags;
    private String aiPrompt;
    private String aiModel;
    private Timestamp createTime;
    private Timestamp updateTime;
}
