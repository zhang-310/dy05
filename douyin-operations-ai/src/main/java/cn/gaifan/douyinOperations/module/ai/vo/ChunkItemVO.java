package cn.gaifan.douyinOperations.module.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文档分块预览项（供前端 chunk 列表展示）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChunkItemVO {

    private String content;
    private List<String> labels;
    private Integer chunkIndex;
}
