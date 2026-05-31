package cn.gaifan.douyinOperations.module.abtest.vo;

import lombok.Data;
import java.sql.Timestamp;
import java.util.List;

@Data
public class AbExperimentVO {
    private Long id;
    private Long ownerId;
    private String name;
    private String description;
    private String experimentType;
    private String targetEntityType;
    private Long targetEntityId;
    private Integer status;
    private Timestamp startTime;
    private Timestamp endTime;
    private Long winnerVariantId;
    private String conclusion;
    private Timestamp createTime;
    private Timestamp updateTime;
    private List<AbVariantVO> variants;
}
