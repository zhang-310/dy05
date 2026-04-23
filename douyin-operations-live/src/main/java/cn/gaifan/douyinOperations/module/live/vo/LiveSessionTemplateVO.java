package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class LiveSessionTemplateVO {
    private Long id;
    private Long ownerId;
    private String name;
    private String code;
    private String description;
    private String structureJson;
    private Timestamp createTime;
    private Timestamp updateTime;
}
