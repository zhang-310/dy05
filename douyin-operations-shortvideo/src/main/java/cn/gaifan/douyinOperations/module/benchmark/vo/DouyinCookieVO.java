package cn.gaifan.douyinOperations.module.benchmark.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Cookie返回VO
 */
@Data
public class DouyinCookieVO {

    private Long id;
    private String cookieName;
    private String cookieValue;
    private String platform;
    private String accountName;
    private LocalDateTime expireTime;
    private Boolean isValid;
    private LocalDateTime lastCheckTime;
    private String checkStatus;
    private Integer usageCount;
    private LocalDateTime lastUsedTime;
    private String notes;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
