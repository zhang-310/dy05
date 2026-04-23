package cn.gaifan.douyinOperations.module.script.vo;

import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class ViolationCheckBatchVO {
    @NotEmpty(message = "待检测文本列表不能为空")
    @Valid
    private List<TextItem> texts;
    /** 检测范围：all / live / video，默认 all */
    private String scope = "all";

    @Data
    public static class TextItem {
        @NotNull(message = "文本标识不能为空")
        private String key;
        private String text;
    }
}
