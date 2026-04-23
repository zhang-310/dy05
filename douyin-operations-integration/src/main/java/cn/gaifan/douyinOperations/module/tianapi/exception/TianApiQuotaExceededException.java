package cn.gaifan.douyinOperations.module.tianapi.exception;

/**
 * TianAPI 配额已用尽异常（code=150，API可用次数不足）
 * 每类每日限制约 1 万次，类目数×1万=账号总可用。某类超限后触发，仅跳过该分类，继续其他类目。
 */
public class TianApiQuotaExceededException extends RuntimeException {

    private final String path;
    private final int code;

    public TianApiQuotaExceededException(String path, int code, String msg) {
        super(String.format("TianAPI 配额已用尽: path=%s, code=%d, msg=%s", path, code, msg));
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
