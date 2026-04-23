package cn.gaifan.douyinOperations.module.copy.vo;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class CopyApprovalVO {
    private Long id;
    private Long copyId;
    private Long userId;
    private Integer approvalStatus;
    private String comments;
    private Timestamp approvalTime;
    private Timestamp createTime;
    private Timestamp updateTime;
    /** 文案标题（关联 cp_library） */
    private String copyTitle;
    /** 文案内容（关联 cp_library） */
    private String copyContent;
    /** 提交人 ID（cp_library.user_id） */
    private Long submitterId;
}
