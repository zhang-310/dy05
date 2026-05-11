package cn.gaifan.douyinOperations.module.douyin.vo;

import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 抖音账号 VO（列表/详情）
 */
@Data
public class DouyinAccountVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long ownerId;
    private String accountName;
    private String accountId;
    private Long followCount;
    private Long fanCount;
    private Long videoCount;
    private Long totalLikes;
    private String description;
    private Integer status;
    private Timestamp bindTime;
    private Timestamp createTime;
    private Timestamp updateTime;
}
