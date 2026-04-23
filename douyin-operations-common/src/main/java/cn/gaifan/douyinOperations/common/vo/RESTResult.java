package cn.gaifan.douyinOperations.common.vo;

import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * RESTful API 统一响应结果封装类
 *
 * @param <T> 响应数据的类型
 */
@Data
public class RESTResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 返回状态码
     */
    private int status;

    /**
     * 输出信息
     */
    private String message;

    /**
     * 返回数据
     */
    private T data;

    /**
     * 时间戳（项目统一使用 java.sql.Timestamp）
     */
    private Timestamp timestamp;

    /**
     * 错误信息
     */
    private String error;

    /**
     * 验证结果
     */
    private boolean valid = false;

    /**
     * 执行耗时（毫秒）
     */
    private Long times;

    /**
     * 请求追踪 ID（从 MDC 注入，便于排查）
     */
    private String traceId;

    /**
     * 默认构造函数
     */
    public RESTResult() {
    }

    // ==================== 成功响应方法 ====================

    /**
     * 验证通过
     *
     * @param message 提示信息
     * @return 状态码 200，valid 为 true
     */
    public static <T> RESTResult<T> validSuccess(String message) {
        RESTResult<T> result = new RESTResult<>();
        result.status = 200;
        result.message = message;
        result.valid = true;
        result.timestamp = new Timestamp(System.currentTimeMillis());
        return result;
    }

    /**
     * 验证不通过
     *
     * @param message 提示信息
     * @return 状态码 200，valid 为 false
     */
    public static <T> RESTResult<T> validError(String message) {
        RESTResult<T> result = new RESTResult<>();
        result.status = 200;
        result.message = message;
        result.valid = false;
        result.timestamp = new Timestamp(System.currentTimeMillis());
        return result;
    }

    /**
     * 成功响应（仅数据，默认提示「成功」）
     *
     * @param data 返回数据
     * @return 状态码 200
     */
    public static <T> RESTResult<T> success(T data) {
        return success("成功", data, null);
    }

    /**
     * 成功响应（无数据，用于 Void 返回）
     *
     * @return 状态码 200
     */
    public static RESTResult<Void> success() {
        RESTResult<Void> r = new RESTResult<>();
        r.setStatus(200);
        r.setMessage("成功");
        r.setData(null);
        r.setTimestamp(new Timestamp(System.currentTimeMillis()));
        return r;
    }

    /**
     * 成功响应（兼容 ok 命名）
     *
     * @param data 返回数据
     * @return 状态码 200
     */
    public static <T> RESTResult<T> ok(T data) {
        return success("成功", data, null);
    }

    /**
     * 错误响应（兼容 fail 命名）
     *
     * @param status  状态码
     * @param message 错误信息
     * @return 指定状态码的响应
     */
    public static <T> RESTResult<T> fail(int status, String message) {
        return error(status, message);
    }

    /**
     * 成功响应
     *
     * @param message 提示信息
     * @param data    返回数据
     * @return 状态码 200
     */
    public static <T> RESTResult<T> success(String message, T data) {
        return success(message, data, null);
    }

    /**
     * 成功响应（带执行耗时）
     *
     * @param message 提示信息
     * @param data    返回数据
     * @param times   执行耗时（毫秒）
     * @return 状态码 200
     */
    public static <T> RESTResult<T> success(String message, T data, Long times) {
        RESTResult<T> result = new RESTResult<>();
        result.status = 200;
        result.message = message;
        result.data = data;
        result.times = times;
        result.timestamp = new Timestamp(System.currentTimeMillis());
        return result;
    }

    /**
     * 获取成功（自动判断数据是否为空）
     *
     * @param data 返回数据
     * @return 数据为空返回 204，否则返回 200
     */
    public static <T> RESTResult<T> getSuccess(T data) {
        return getSuccess(data, null);
    }

    /**
     * 获取成功（带执行耗时）
     *
     * @param data  返回数据
     * @param times 执行耗时（毫秒）
     * @return 数据为空返回 204，否则返回 200
     */
    public static <T> RESTResult<T> getSuccess(T data, Long times) {
        return data == null ? dataNull() : success("获取成功!", data, times);
    }

    /**
     * 添加成功（自动判断数据是否为空）
     *
     * @param data 返回数据
     * @return 数据为空返回 204，否则返回 200
     */
    public static <T> RESTResult<T> addSuccess(T data) {
        return data == null ? dataNull() : success("添加成功!", data);
    }

    /**
     * 修改成功（自动判断数据是否为空）
     *
     * @param data 返回数据
     * @return 数据为空返回 204，否则返回 200
     */
    public static <T> RESTResult<T> updateSuccess(T data) {
        return data == null ? dataNull() : success("修改成功", data);
    }

    /**
     * 删除成功（自动判断删除数量是否有效）
     *
     * @param data 删除的数据（通常是删除的记录数）
     * @return 删除数量无效返回 204，否则返回 200
     */
    public static <T> RESTResult<T> deleteSuccess(T data) {
        if (data == null) {
            return dataNull();
        }
        try {
            int num = Integer.parseInt(data.toString());
            return num > 0 ? success("删除成功", data) : dataNull();
        } catch (Exception e) {
            return dataNull();
        }
    }

    /**
     * 统计查询成功
     *
     * @param data 统计数据
     * @return 状态码 200
     */
    public static <T> RESTResult<T> countSuccess(T data) {
        return success("统计查询成功", data);
    }

    // ==================== 错误响应方法 ====================

    /**
     * 错误响应
     *
     * @param status  状态码
     * @param message 错误信息
     * @return 指定状态码的响应
     */
    public static <T> RESTResult<T> error(int status, String message) {
        return error(status, message, null);
    }

    /**
     * 错误响应（带数据）
     *
     * @param status  状态码
     * @param message 错误信息
     * @param data    错误相关数据
     * @return 指定状态码的响应
     */
    public static <T> RESTResult<T> error(int status, String message, T data) {
        RESTResult<T> result = new RESTResult<>();
        result.status = status;
        result.message = message;
        result.data = data;
        result.timestamp = new Timestamp(System.currentTimeMillis());
        return result;
    }

    /**
     * 服务器内部错误
     *
     * @param message 错误信息
     * @return 状态码 500
     */
    public static <T> RESTResult<T> getFailed(String message) {
        return error(500, message);
    }

    /**
     * 验证错误
     *
     * @param status  状态码
     * @param message 错误信息
     * @return 指定状态码的响应
     */
    public static <T> RESTResult<T> validError(int status, String message) {
        return error(status, message);
    }

    /**
     * 非法状态异常
     *
     * @param status  状态码
     * @param message 错误信息
     * @return 指定状态码的响应
     */
    public static <T> RESTResult<T> illegalStateException(int status, String message) {
        return error(status, message);
    }

    /**
     * 服务提供者错误
     *
     * @return 状态码 501，默认错误信息
     */
    public static <T> RESTResult<T> providerError() {
        return providerError("系统繁忙，请稍后再试");
    }

    /**
     * 服务提供者错误
     *
     * @param message 错误信息
     * @return 状态码 501
     */
    public static <T> RESTResult<T> providerError(String message) {
        return error(501, message);
    }

    /**
     * 服务错误
     *
     * @return 状态码 501，默认错误信息
     */
    public static <T> RESTResult<T> serviceError() {
        return serviceError("系统繁忙，请稍后再试");
    }

    /**
     * 服务错误
     *
     * @param message 错误信息
     * @return 状态码 501
     */
    public static <T> RESTResult<T> serviceError(String message) {
        return error(501, message);
    }

    /**
     * 数据为空
     *
     * @return 状态码 204
     */
    public static <T> RESTResult<T> dataNull() {
        return error(204, "资源或者信息为空!");
    }

    /**
     * 访问被禁止
     *
     * @return 状态码 403
     */
    public static <T> RESTResult<T> forbidden() {
        return error(403, "该用户接口访问权限不足!");
    }

    /**
     * 访问被禁止（兼容旧方法名）
     *
     * @return 状态码 403
     * @deprecated 请使用 {@link #forbidden()} 方法
     */
    @Deprecated
    public static <T> RESTResult<T> Forbidden() {
        return forbidden();
    }

    // ==================== 工具方法 ====================

    /**
     * 判断响应是否成功
     *
     * @return true 表示状态码为 200
     */
    public boolean isSuccess() {
        return status == 200;
    }

    /**
     * 设置成功状态
     *
     * @param success true 设置状态码为 200，false 设置为 500
     */
    public void setSuccess(boolean success) {
        this.status = success ? 200 : 500;
    }

    // ==================== 显式 getter/setter（Lombok @Data 降级兜底）====================

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public Long getTimes() { return times; }
    public void setTimes(Long times) { this.times = times; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

}
