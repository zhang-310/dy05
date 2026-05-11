package cn.gaifan.douyinOperations.module.live.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量添加产品请求 VO
 * Batch Add Products Request VO
 */
@Data
public class LiveProductBatchAddVO {

    @NotNull(message = "场次 ID 不能为空")
    private Long sessionId;

    @NotEmpty(message = "产品列表不能为空")
    @Valid
    private List<LiveProductBatchAddItemVO> items;
}
