package cn.gaifan.douyinOperations.module.system.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.system.entity.SysSyncLog;

import java.util.Map;

public interface SystemService {

    // API 调用日志
    PageResultVO<Map<String, Object>> searchApiLogs(String module, String apiName,
                                                     Integer status, String startTime,
                                                     String endTime, String sortName, String sortOrder,
                                                     int page, int rows);

    Map<String, Object> getApiLogStats(String module, String startTime, String endTime);

    Map<String, Object> getApiLogById(Long id);

    void saveApiLog(String module, String apiName, String requestUrl, String requestMethod,
                    String requestParams, Integer responseStatus, String responseBody,
                    Integer status, String errorMessage, Long durationMs, Long userId);

    // 同步日志
    PageResultVO<Map<String, Object>> searchSyncLogs(String syncType, String status,
                                                      Long userId, String startTime,
                                                      String endTime, int page, int rows);

    Long startSync(String syncType, Long userId, Long accountId);

    void updateSyncProgress(Long syncLogId, int total, int success, int fail);

    void completeSync(Long syncLogId, int total, int success, int fail);

    void failSync(Long syncLogId, String errorMessage);

    // 系统监控
    Map<String, Object> checkHealth();

    Map<String, Object> getSystemInfo();
}
