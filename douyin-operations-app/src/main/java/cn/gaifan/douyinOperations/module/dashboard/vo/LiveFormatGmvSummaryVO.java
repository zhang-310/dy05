package cn.gaifan.douyinOperations.module.dashboard.vo;

import lombok.Data;

import java.util.List;

@Data
public class LiveFormatGmvSummaryVO {

    private int lookbackDays;
    /** 统计窗口起始日（本地日期，YYYY-MM-DD） */
    private String since;
    private List<LiveFormatGmvRowVO> rows;
}
