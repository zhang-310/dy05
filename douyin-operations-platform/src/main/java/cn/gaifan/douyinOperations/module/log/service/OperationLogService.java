package cn.gaifan.douyinOperations.module.log.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.log.vo.OperationLogSearchVO;
import cn.gaifan.douyinOperations.module.log.vo.OperationLogVO;

/**
 * 操作日志：分页查询与落库记录
 */
public interface OperationLogService {

    PageResultVO<OperationLogVO> search(OperationLogSearchVO vo);

    /**
     * 记录一条操作日志（异步或同步，由实现决定；异常不抛出避免影响主流程）
     * @param requestBody 请求体摘要（可选，限制 2000 字符）
     * @param responseBody 响应体摘要（可选，限制 2000 字符）
     */
    void save(Long userId, String username, String module, String action, String requestUri, String requestMethod,
              String ip, String userAgent, Integer durationMs, int status, String errorMsg, String traceId,
              String requestBody, String responseBody);
}
