package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 直播话术导航 VO
 */
@Data
public class LiveScriptNavigationVO {
    private Long id;
    private Long sessionId;
    private String scriptContent;
    private Integer sequenceNo;
    private LocalDateTime createTime;
}
