package cn.gaifan.douyinOperations.common.exception;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BusinessException 业务异常测试")
class BusinessExceptionTest {

    @Test
    void constructor_withCodeAndMessage() {
        BusinessException ex = new BusinessException(ErrorCode.DATA_NOT_FOUND, "数据不存在");
        assertThat(ex.getCode()).isEqualTo(1005);
        assertThat(ex.getMessage()).isEqualTo("数据不存在");
    }

    @Test
    void constructor_withMessageOnly_shouldUseSystemBusy() {
        BusinessException ex = new BusinessException("系统繁忙");
        assertThat(ex.getCode()).isEqualTo(ErrorCode.SYSTEM_BUSY);
        assertThat(ex.getMessage()).isEqualTo("系统繁忙");
    }

    @Test
    void shouldBeRuntimeException() {
        BusinessException ex = new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}
