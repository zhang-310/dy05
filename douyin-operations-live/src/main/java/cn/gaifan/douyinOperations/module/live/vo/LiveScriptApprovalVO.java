package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class LiveScriptApprovalVO {

    private Long id;
    private Long scriptId;
    private Long sessionId;
    private Long submitterId;
    private Long reviewerId;
    private String action;
    private Integer status;
    private String comments;
    private Timestamp reviewTime;
    private Timestamp createTime;
    private Timestamp updateTime;

    // ── 关联冗余（前端展示用） ──
    private String scriptContent;
    private String scriptType;
    private String sessionTitle;
}
