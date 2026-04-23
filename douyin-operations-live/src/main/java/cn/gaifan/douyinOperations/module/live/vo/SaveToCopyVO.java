package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class SaveToCopyVO {

    @NotBlank(message = "话术内容不能为空")
    @Size(max = 10000, message = "话术内容不能超过 10000 个字符")
    private String content;

    /** 标题（可选，不传则自动生成） */
    @Size(max = 200, message = "标题不能超过 200 个字符")
    private String title;
}
