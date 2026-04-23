package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;
import java.sql.Timestamp;

/**
 * 历史数据对比单条 VO
 */
@Data
public class LiveHistoryItemVO {
    private Long sessionId;
    private String liveTitle;
    private Timestamp startTime;
    private Timestamp endTime;
    private Integer status;
    /** 场次汇总数据（可能为空） */
    private LiveSessionDataVO sessionData;
}
