package cn.gaifan.douyinOperations.module.dashboard.vo;

import lombok.Data;

import java.util.List;

@Data
public class ProductGmvSummaryVO {

    private int lookbackDays;
    private String since;
    private List<ProductGmvRowVO> rows;
}
