package cn.gaifan.douyinOperations.module.ai.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * OpenClaw / 外部 RAG 检索请求
 * 支持跨全部知识库检索，用于构建话术、短视频脚本等
 */
@Data
public class OpenClawRagRetrieveVO {

    /** 查询内容（主题/关键词），必填 */
    @NotBlank(message = "query 不能为空")
    @Size(max = 512)
    private String query;

    /** 返回条数，默认 15 */
    @Min(1)
    @Max(50)
    private Integer topK = 15;

    /**
     * 检索范围：all=全部，huashu=话术库，douyin=抖音库，zhishi=技术库，custom=按 kbNames
     * 默认 all
     */
    private String scope = "all";

    /**
     * 指定知识库名称列表，当 scope=custom 时生效；如 ["huashu","douyin"]
     */
    private List<String> kbNames;

    /** 是否返回拼接后的上下文文本，便于直接注入 LLM prompt，默认 true */
    private Boolean includeContextText = true;
}
