package cn.gaifan.douyinOperations.module.log.vo;

import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 系统日志展示 VO
 */
@Data
public class SystemLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String module;
    private String eventType;
    private String summary;
    private String detail;
    private Integer status;
    private Timestamp createTime;
}
