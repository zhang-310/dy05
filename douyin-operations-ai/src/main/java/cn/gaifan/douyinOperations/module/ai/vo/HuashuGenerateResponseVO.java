package cn.gaifan.douyinOperations.module.ai.vo;

import lombok.Data;

import java.util.List;

/**
 * 高光话术生成响应
 */
@Data
public class HuashuGenerateResponseVO {

    /** 生成的高光话术内容（Markdown 格式） */
    private String content;

    /** 导入结果摘要（若本次有导入） */
    private ImportSummary importSummary;

    /** RAG 引用数量 */
    private int ragRefCount;

    /** 检索到的参考片段标题列表 */
    private List<String> ragRefTitles;

    @Data
    public static class ImportSummary {
        private int total;
        private int success;
        private int failed;
    }
}
