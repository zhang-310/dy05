package cn.gaifan.douyinOperations.common.exception;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

/**
 * 全局异常处理，将异常转为 RESTResult
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<RESTResult<?>> handleBusiness(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: uri={}, code={}, msg={}", request != null ? request.getRequestURI() : "n/a", e.getCode(), e.getMessage());
        RESTResult<?> r = RESTResult.error(e.getCode(), e.getMessage());
        r.setTraceId(MDC.get("traceId"));
        HttpStatus status = (e.getCode() == ErrorCode.INSUFFICIENT_CREDITS
                || e.getCode() == ErrorCode.ENTITLEMENT_DENIED)
                ? HttpStatus.PAYMENT_REQUIRED
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(r);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public RESTResult<?> handleValid(MethodArgumentNotValidException e, HttpServletRequest request) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: uri={}, msg={}", request != null ? request.getRequestURI() : "n/a", msg);
        RESTResult<?> r = RESTResult.error(ErrorCode.VALIDATION_FAIL, msg);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @SuppressWarnings("null")
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public RESTResult<?> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        String path = request != null ? request.getRequestURI() : "";
        String msg = path.contains("/login") ? "登录接口请使用 POST 方法，请通过登录页提交" : "不支持的请求方法: " + request.getMethod();
        log.warn("请求方法错误: uri={}, method={}", path, request != null ? request.getMethod() : "n/a");
        RESTResult<?> r = RESTResult.error(ErrorCode.VALIDATION_FAIL, msg);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.OK)
    public RESTResult<?> handleMaxUploadSize(MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.warn("文件大小超限: uri={}", request != null ? request.getRequestURI() : "n/a");
        RESTResult<?> r = RESTResult.error(ErrorCode.STORAGE_FILE_TOO_LARGE, "上传文件过大，请压缩后重试（单文件限 50MB）");
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.OK)
    public RESTResult<?> handleMissingParam(MissingServletRequestParameterException e, HttpServletRequest request) {
        String msg = "缺少必要参数: " + e.getParameterName() + "（若为文件上传，请确保使用 multipart/form-data 且字段名正确）";
        log.warn("缺少请求参数: uri={}, param={}", request != null ? request.getRequestURI() : "n/a", e.getParameterName());
        RESTResult<?> r = RESTResult.error(ErrorCode.VALIDATION_FAIL, msg);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.OK)
    public RESTResult<?> handleBind(BindException e, HttpServletRequest request) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("绑定异常: uri={}, msg={}", request != null ? request.getRequestURI() : "n/a", msg);
        RESTResult<?> r = RESTResult.error(ErrorCode.VALIDATION_FAIL, msg);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RESTResult<?> handleOther(Throwable e, HttpServletRequest request) {
        log.error("未处理异常: uri={}, method={}", request != null ? request.getRequestURI() : "n/a", request != null ? request.getMethod() : "n/a", e);
        RESTResult<?> r = RESTResult.error(ErrorCode.SYSTEM_BUSY, "系统繁忙，请稍后重试");
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
