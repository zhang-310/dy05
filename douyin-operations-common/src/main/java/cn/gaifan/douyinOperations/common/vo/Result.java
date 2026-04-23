package cn.gaifan.douyinOperations.common.vo;

/**
 * Result 类 - RESTResult 的别名
 * 用于统一响应体格式
 *
 * @deprecated 建议使用 RESTResult，保留此类仅为向后兼容
 */
@Deprecated
public class Result<T> extends RESTResult<T> {

    /**
     * 成功响应（无数据）
     */
    public static Result<Void> success() {
        Result<Void> result = new Result<>();
        result.setStatus(0);
        result.setMessage("操作成功");
        result.setValid(true);
        result.setTimestamp(new java.sql.Timestamp(System.currentTimeMillis()));
        return result;
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setStatus(0);
        result.setMessage("操作成功");
        result.setValid(true);
        result.setTimestamp(new java.sql.Timestamp(System.currentTimeMillis()));
        result.setData(data);
        return result;
    }

    /**
     * 成功响应（带数据和消息）
     */
    public static <T> Result<T> success(T data, String message) {
        Result<T> result = success(data);
        result.setMessage(message);
        return result;
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setStatus(-1);
        result.setMessage(message);
        result.setError(message);
        result.setValid(false);
        result.setTimestamp(new java.sql.Timestamp(System.currentTimeMillis()));
        return result;
    }

    /**
     * 失败响应（带状态码）
     */
    public static <T> Result<T> error(int status, String message) {
        Result<T> result = error(message);
        result.setStatus(status);
        return result;
    }
}
