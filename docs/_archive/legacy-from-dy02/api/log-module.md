# log 模块 API 文档

## 文件结构
```
config/LoginSuccessLogListener.java
config/OperationLogFilter.java
config/StatusCaptureResponseWrapper.java
config/SystemLogStartupListener.java
controller/LogController.java
controller/package-info.java
entity/OperationLog.java
entity/SystemLog.java
entity/package-info.java
package-info.java
repository/OperationLogRepository.java
repository/SystemLogRepository.java
repository/package-info.java
service/OperationLogService.java
service/SystemLogService.java
service/impl/OperationLogServiceImpl.java
service/impl/SystemLogServiceImpl.java
service/impl/package-info.java
service/package-info.java
util/BodyMaskUtil.java
util/package-info.java
vo/OperationLogSearchVO.java
vo/OperationLogVO.java
vo/SystemLogSearchVO.java
vo/SystemLogVO.java
vo/package-info.java
```

## API 接口

### LogController
```
@RequestMapping("/api/v1/log")
@PostMapping("/operation/page")
public RESTResult<PageResultVO<OperationLogVO>> operationPage(HttpServletRequest request,
@PostMapping("/system/page")
public RESTResult<PageResultVO<SystemLogVO>> systemPage(HttpServletRequest request,
@PostMapping("/operation/export")
@PostMapping("/system/export")
```

### package-info
```
```

## Entity 字段

### OperationLog
```
@Id
private Long id;
@Column(name = "trace_id", length = 64)
private String traceId;
@Column(name = "user_id")
private Long userId;
@Column(name = "username", length = 64)
private String username;
@Column(name = "module", length = 64)
private String module;
@Column(name = "action", length = 32)
private String action;
@Column(name = "request_uri", length = 256)
private String requestUri;
@Column(name = "request_method", length = 16)
private String requestMethod;
@Column(name = "ip", length = 64)
private String ip;
@Column(name = "user_agent", length = 256)
private String userAgent;
@Column(name = "duration_ms")
private Integer durationMs;
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "error_msg", length = 512)
private String errorMsg;
@Column(name = "request_body", length = 2000)
private String requestBody;
@Column(name = "response_body", length = 2000)
private String responseBody;
@Column(name = "create_time")
private Timestamp createTime;
```

### SystemLog
```
@Id
private Long id;
@Column(name = "module", length = 64)
private String module;
@Column(name = "event_type", nullable = false, length = 32)
private String eventType;
@Column(name = "summary", length = 256)
private String summary;
@Column(name = "detail", columnDefinition = "TEXT")
private String detail;
@Column(name = "status", nullable = false)
private Integer status = 1;
@Column(name = "create_time")
private Timestamp createTime;
```

### package-info
```
```

