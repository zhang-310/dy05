package cn.gaifan.douyinOperations.module.live.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 整场批量提交审核请求 VO
 * P1-9: 替换 Map 参数，添加校验
 */
@Data
public class SubmitBySessionVO {

    @NotNull(message = "sessionId 不能为空")
    private Long sessionId;

    private String comments;
}
