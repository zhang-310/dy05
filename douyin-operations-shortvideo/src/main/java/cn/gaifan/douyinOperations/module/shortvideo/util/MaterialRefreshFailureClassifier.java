package cn.gaifan.douyinOperations.module.shortvideo.util;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;

/**
 * A-5：素材批量刷新与异步富化失败 bucket 的单一来源，避免 Controller 与 Service 侧口径漂移。
 */
public final class MaterialRefreshFailureClassifier {

    private MaterialRefreshFailureClassifier() {
    }

    /**
     * 低基数 tag：供 Prometheus、批量 API {@code reasonBucket}、富化指标共用。
     */
    public static String failureReasonBucket(Throwable ex) {
        if (ex == null) {
            return "unknown";
        }
        if (ex instanceof BusinessException be) {
            int code = be.getCode();
            if (code == ErrorCode.DATA_NOT_FOUND) {
                return "not_found";
            }
            if (code == ErrorCode.FORBIDDEN) {
                return "forbidden";
            }
            if (code == ErrorCode.VALIDATION_FAIL) {
                return "validation";
            }
            if (code == ErrorCode.UNAUTHORIZED) {
                return "unauthorized";
            }
            return "business_" + code;
        }
        if (ex instanceof java.net.http.HttpTimeoutException
                || ex instanceof java.io.InterruptedIOException
                || ex instanceof InterruptedException) {
            return "timeout";
        }
        String m = String.valueOf(ex.getMessage()).toLowerCase();
        if (m.contains("timeout")) {
            return "timeout";
        }
        if (ex instanceof java.io.IOException) {
            return "http";
        }
        return "other";
    }
}
