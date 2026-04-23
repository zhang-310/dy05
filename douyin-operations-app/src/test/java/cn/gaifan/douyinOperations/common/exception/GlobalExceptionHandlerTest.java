package cn.gaifan.douyinOperations.common.exception;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler 全局异常处理测试")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        MDC.put("traceId", "test-trace-001");
    }

    @Test
    void handleBusiness_shouldReturnErrorCodeAndMessage() {
        BusinessException ex = new BusinessException(ErrorCode.DATA_NOT_FOUND, "数据不存在");
        RESTResult<?> result = handler.handleBusiness(ex, null);

        assertThat(result.getStatus()).isEqualTo(ErrorCode.DATA_NOT_FOUND);
        assertThat(result.getMessage()).isEqualTo("数据不存在");
        assertThat(result.getTraceId()).isEqualTo("test-trace-001");
    }

    @Test
    void handleValid_shouldReturnValidationFail() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("obj", "name", "不能为空");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        RESTResult<?> result = handler.handleValid(ex, null);

        assertThat(result.getStatus()).isEqualTo(ErrorCode.VALIDATION_FAIL);
        assertThat(result.getMessage()).contains("name");
        assertThat(result.getMessage()).contains("不能为空");
        assertThat(result.getTraceId()).isEqualTo("test-trace-001");
    }

    @Test
    void handleBind_shouldReturnValidationFail() {
        BindException ex = mock(BindException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("obj", "email", "格式不正确");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        RESTResult<?> result = handler.handleBind(ex, null);

        assertThat(result.getStatus()).isEqualTo(ErrorCode.VALIDATION_FAIL);
        assertThat(result.getMessage()).contains("email");
    }

    @Test
    void handleOther_shouldReturnSystemBusy() {
        RuntimeException ex = new RuntimeException("unexpected");
        RESTResult<?> result = handler.handleOther(ex, null);

        assertThat(result.getStatus()).isEqualTo(ErrorCode.SYSTEM_BUSY);
        assertThat(result.getMessage()).contains("系统繁忙");
        assertThat(result.getTraceId()).isEqualTo("test-trace-001");
    }
}
