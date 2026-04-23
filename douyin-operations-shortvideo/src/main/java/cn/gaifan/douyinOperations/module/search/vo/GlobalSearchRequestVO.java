package cn.gaifan.douyinOperations.module.search.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class GlobalSearchRequestVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "关键词不能为空")
    @Size(min = 2, max = 64, message = "关键词长度为 2–64 字符")
    private String q;

    /** 总结果上限（单请求合并各类型） */
    @Min(1)
    @Max(50)
    private Integer limit = 24;
}
