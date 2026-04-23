package cn.gaifan.douyinOperations.common.exception;

import lombok.Getter;

/**
 * 业务异常，携带错误码与提示信息
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;
    private final String message;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(String message) {
        this(cn.gaifan.douyinOperations.common.constant.ErrorCode.SYSTEM_BUSY, message);
    }

    /** 兼容：与 getCode() 一致 */
    public int getErrorCode() {
        return code;
    }
}
