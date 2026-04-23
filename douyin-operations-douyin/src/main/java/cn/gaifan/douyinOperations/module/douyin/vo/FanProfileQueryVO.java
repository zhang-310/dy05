package cn.gaifan.douyinOperations.module.douyin.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 粉丝画像查询 VO
 */
@Data
public class FanProfileQueryVO {
    @NotNull(message = "accountId 不能为空")
    private Long accountId;

    private String statType;
}
