package cn.gaifan.douyinOperations.module.ai.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 知识反馈入参：1=有用 -1=无用 0=不确定
 */
@Data
public class KbFeedbackVO {

    @NotNull(message = "docId 不能为空")
    private Long docId;

    @NotNull(message = "query 不能为空")
    private String query;

    @NotNull(message = "rating 不能为空")
    @Min(-1)
    @Max(1)
    private Integer rating;

    private String comment;

    private String searchMode;
}
