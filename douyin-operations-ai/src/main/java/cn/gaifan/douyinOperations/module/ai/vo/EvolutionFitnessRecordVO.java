package cn.gaifan.douyinOperations.module.ai.vo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class EvolutionFitnessRecordVO {

    private Long id;
    private Long kbId;
    private String taskId;
    private String parentTaskId;
    private String metricName;
    private Double metricValue;
    private String payloadJson;
    private String experimentId;
    private Timestamp createTime;
}
