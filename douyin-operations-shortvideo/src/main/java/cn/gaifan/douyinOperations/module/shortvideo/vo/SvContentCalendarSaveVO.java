package cn.gaifan.douyinOperations.module.shortvideo.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SvContentCalendarSaveVO {
    private Long id;
    private Long personaId;
    @NotNull(message = "计划日期不能为空")
    private String planDate;
    @NotBlank(message = "内容类型不能为空")
    private String contentType;
    private String title;
    private String brief;
    private Long scriptId;
    private Long projectId;
    private Integer priority;
    private String publishTime;
    private Long accountId;
    private String tags;
    private Integer status;
}
