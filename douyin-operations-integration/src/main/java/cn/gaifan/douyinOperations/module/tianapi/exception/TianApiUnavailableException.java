package cn.gaifan.douyinOperations.module.tianapi.exception;

/**
 * TianAPI 接口不可用异常，例如未申请该 API。此类错误应停止当前分类，
 * 避免对同一个不可用接口重复打满每日额度循环。
 */
public class TianApiUnavailableException extends RuntimeException {

    private final String path;
    private final int code;

    public TianApiUnavailableException(String path, int code, String msg) {
        super(String.format("TianAPI 接口不可用: path=%s, code=%d, msg=%s", path, code, msg));
        this.path = path;
        this.code = code;
    }

    public String getPath() {
        return path;
    }

    public int getCode() {
        return code;
    }
}
