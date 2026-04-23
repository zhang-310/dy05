package cn.gaifan.douyinOperations.module.dashboard.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * D5-02 经营驾驶舱 CSV 导出筛选（≥3 维可选组合：时间 / 账号 / 状态 / 时段 / 品类）
 */
@Data
public class CockpitExportRequestVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 抖音账号 ID（可选） */
    private Long accountId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateFrom;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateTo;

    /** 场次状态（可选，与 live_session.status 一致） */
    private Integer sessionStatus;

    /**
     * 开播时段桶：all | morning(6–11) | afternoon(12–17) | evening(18–23) | night(0–5)
     */
    private String hourBucket;

    /** 商品品类关键字（模糊匹配 dy_product.product_category） */
    private String productCategory;
}
