package cn.gaifan.douyinOperations.module.ai.exception;

/**
 * 视频生成统一异常
 * 所有 Provider 应使用此异常替代 RuntimeException，
 * 便于上层统一捕获和错误日志记录。
 */
public class VideoGenerationException extends RuntimeException {

    private final String providerName;
    private final String errorCode;

    public VideoGenerationException(String providerName, String message) {
        super(message);
        this.providerName = providerName;
        this.errorCode = "UNKNOWN";
    }

    public VideoGenerationException(String providerName, String message, Throwable cause) {
        super(message, cause);
        this.providerName = providerName;
        this.errorCode = "UNKNOWN";
    }

    public VideoGenerationException(String providerName, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.providerName = providerName;
        this.errorCode = errorCode;
    }

    public String getProviderName() { return providerName; }
    public String getErrorCode() { return errorCode; }

    @Override
    public String toString() {
        return String.format("VideoGenerationException[provider=%s, code=%s]: %s",
                providerName, errorCode, getMessage());
    }
}
