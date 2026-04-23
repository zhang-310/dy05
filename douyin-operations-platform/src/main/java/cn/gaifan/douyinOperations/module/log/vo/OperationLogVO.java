package cn.gaifan.douyinOperations.module.log.vo;

import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 操作日志展示 VO
 */
@Data
public class OperationLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String traceId;
    private Long userId;
    private String username;
    private String module;
    private String action;
    private String requestUri;
    private String requestMethod;
    private String ip;
    private String userAgent;
    private Integer durationMs;
    private Integer status;
    private String errorMsg;
    private String requestBody;
    private String responseBody;
    private Timestamp createTime;
}
