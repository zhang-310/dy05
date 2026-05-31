package cn.gaifan.douyinOperations.module.ai.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OfficialKnowledgeCollectSearchVO extends BasicQueryDto {
    private String keyword;
    private String sourceType;
    private String category;
    private String topicCode;
    private String targetKbName;
    private String collectStatus;
    private String indexStatus;
    private String ocrStatus;
    private String asrStatus;
    private Boolean violation;
    private Boolean failedOnly;
    private Boolean mediaPendingOnly;
}
