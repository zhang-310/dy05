package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
public class SvShotListVO {

    private Long id;
    private Long ownerId;
    private Long scriptId;
    private Integer shotCount;
    private List<SvShotVO> shots;
    private Timestamp createTime;
    private Timestamp updateTime;
}
