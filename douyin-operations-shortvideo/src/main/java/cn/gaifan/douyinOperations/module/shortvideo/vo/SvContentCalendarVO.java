package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

@Data
public class SvContentCalendarVO {
    private Long id;
    private Long ownerId;
    private Long personaId;
    private String planDate;
    private String contentType;
    private String title;
    private String brief;
    private Long scriptId;
    private Long projectId;
    private Long shootingTaskId;
    private Integer status;
    private Integer priority;
    private String publishTime;
    private Long accountId;
    private String tags;
    private String createTime;
    private String updateTime;
}
