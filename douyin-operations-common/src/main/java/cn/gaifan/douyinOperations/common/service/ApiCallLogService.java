package cn.gaifan.douyinOperations.common.service;

/**
 * P1-10: API 调用日志记录服务（统一日志记录逻辑）
 *
 * 用途：
 * 1. RestTemplate 通过 ApiCallLogInterceptor 自动调用
 * 2. HttpClient (ExternalApiGateway) 手动调用
 * 3. 统一日志格式、脱敏规则、存储逻辑
 */
public interface ApiCallLogService {

    /**
     * 记录 API 调用日志
     *
     * @param module 模块名称（douyin/wecom/ai/unknown）
     * @param apiName API 名称（通常是路径）
     * @param requestUrl 完整请求 URL
     * @param requestMethod HTTP 方法（GET/POST/PUT/DELETE）
     * @param requestParams 请求参数（已脱敏）
     * @param responseStatus HTTP 状态码
     * @param responseBody 响应体（已截断）
     * @param status 调用状态（1=成功 0=失败）
     * @param errorMessage 错误信息
     * @param durationMs 耗时（毫秒）
     * @param userId 用户 ID（可选）
     */
    void logApiCall(String module, String apiName, String requestUrl, String requestMethod,
                    String requestParams, Integer responseStatus, String responseBody,
                    int status, String errorMessage, long durationMs, Long userId);

    /**
     * 脱敏请求参数（JSON 或 Form 格式）
     *
     * @param body 原始请求体
     * @return 脱敏后的字符串（截断至 2000 字符）
     */
    String sanitizeParams(byte[] body);

    /**
     * 脱敏响应体（截断至 2000 字符）
     *
     * @param responseBody 原始响应体
     * @return 脱敏后的字符串
     */
    String sanitizeResponse(String responseBody);
}
