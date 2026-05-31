package cn.gaifan.douyinOperations.module.ai.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识库混合搜索入参
 */
@Data
public class KbSearchVO {

    @NotBlank(message = "搜索关键词不能为空")
    @Size(max = 512)
    private String query;

    @Min(1)
    @Max(100)
    private Integer topK = 10;

    /**
     * 是否启用 LLM 查询改写（绑定抖音账号时会拆成多子查询并先调大模型，耗时可增加数秒～数十秒）。
     * 默认 false：管理端「RAG 检测」走原句检索，延迟更低。
     */
    private boolean queryRewrite = false;
}
