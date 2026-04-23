package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;
import java.sql.Timestamp;
import java.util.List;

/**
 * 场次数据概览 VO
 */
@Data
public class LiveSessionOverviewVO {
    private LiveSessionVO session;
    private LiveSessionDataVO sessionData;
    private List<LiveProductDataVO> productDataList;
    private List<LiveScriptVO> scriptTop5;
}
