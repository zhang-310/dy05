package cn.gaifan.douyinOperations.module.dashboard.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 驾驶舱场次行（下钻预览 / CSV 同源） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CockpitSessionRowVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long sessionId;
    private String liveTitle;
    private Long accountId;
    private Long userId;
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private BigDecimal gmv;
    private Long productLineCount;
}
