package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiTextGenerateResultVO {

    private String content;

    private String scene;

    private List<DouyinOfficialReferenceVO> officialReferences = new ArrayList<>();

    private String referencedChunkIds;

    private Long aiCallLogId;

    private Boolean officialReferenceRequired;

    private Boolean officialReferenceSatisfied;

    private String officialReferenceStatus;
}
