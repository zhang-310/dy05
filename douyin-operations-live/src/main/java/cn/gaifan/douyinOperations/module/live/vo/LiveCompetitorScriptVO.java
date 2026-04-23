package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class LiveCompetitorScriptVO {
    private Long id;
    private Long ownerId;
    private String title;
    private String competitorName;
    private String platform;
    private String scriptContent;
    private String sourceUrl;
    private String tags;
    private String notes;
    private Timestamp createTime;
    private Timestamp updateTime;
}
