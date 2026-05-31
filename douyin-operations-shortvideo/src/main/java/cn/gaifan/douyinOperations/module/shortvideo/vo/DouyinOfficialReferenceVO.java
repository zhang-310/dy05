package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

@Data
public class DouyinOfficialReferenceVO {

    private String kbName;

    private String refType;

    private Long docId;

    private Long chunkId;

    private String title;

    private String contentPreview;

    private Double score;
}
