package cn.gaifan.douyinOperations.module.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG 检索单项（含来源知识库信息）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagRetrieveItemVO {

    private Long docId;
    private Long kbId;
    private String kbName;
    private String title;
    private String content;
    private double score;
    private String source;
}
