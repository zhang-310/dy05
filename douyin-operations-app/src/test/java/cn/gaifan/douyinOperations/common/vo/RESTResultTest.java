package cn.gaifan.douyinOperations.common.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RESTResult 统一响应封装测试")
class RESTResultTest {

    @Nested
    @DisplayName("成功响应")
    class SuccessResponses {

        @Test
        void success_shouldReturn200WithData() {
            RESTResult<String> r = RESTResult.success("ok", "hello");
            assertThat(r.getStatus()).isEqualTo(200);
            assertThat(r.getMessage()).isEqualTo("ok");
            assertThat(r.getData()).isEqualTo("hello");
            assertThat(r.getTimestamp()).isNotNull();
            assertThat(r.isSuccess()).isTrue();
        }

        @Test
        void success_withTimes_shouldSetTimes() {
            RESTResult<String> r = RESTResult.success("ok", "data", 123L);
            assertThat(r.getTimes()).isEqualTo(123L);
        }

        @Test
        void getSuccess_withData_shouldReturn200() {
            RESTResult<String> r = RESTResult.getSuccess("data");
            assertThat(r.getStatus()).isEqualTo(200);
            assertThat(r.getData()).isEqualTo("data");
        }

        @Test
        void getSuccess_withNull_shouldReturn204() {
            RESTResult<String> r = RESTResult.getSuccess(null);
            assertThat(r.getStatus()).isEqualTo(204);
        }

        @Test
        void addSuccess_withData_shouldReturn200() {
            RESTResult<Long> r = RESTResult.addSuccess(1L);
            assertThat(r.getStatus()).isEqualTo(200);
            assertThat(r.getMessage()).contains("添加成功");
        }

        @Test
        void addSuccess_withNull_shouldReturn204() {
            RESTResult<Long> r = RESTResult.addSuccess(null);
            assertThat(r.getStatus()).isEqualTo(204);
        }

        @Test
        void updateSuccess_withData_shouldReturn200() {
            RESTResult<String> r = RESTResult.updateSuccess("updated");
            assertThat(r.getStatus()).isEqualTo(200);
            assertThat(r.getMessage()).contains("修改成功");
        }

        @Test
        void deleteSuccess_withPositiveCount_shouldReturn200() {
            RESTResult<Integer> r = RESTResult.deleteSuccess(1);
            assertThat(r.getStatus()).isEqualTo(200);
            assertThat(r.getMessage()).contains("删除成功");
        }

        @Test
        void deleteSuccess_withZero_shouldReturn204() {
            RESTResult<Integer> r = RESTResult.deleteSuccess(0);
            assertThat(r.getStatus()).isEqualTo(204);
        }

        @Test
        void deleteSuccess_withNull_shouldReturn204() {
            RESTResult<Integer> r = RESTResult.deleteSuccess(null);
            assertThat(r.getStatus()).isEqualTo(204);
        }

        @Test
        void countSuccess_shouldReturn200() {
            RESTResult<Long> r = RESTResult.countSuccess(100L);
            assertThat(r.getStatus()).isEqualTo(200);
            assertThat(r.getData()).isEqualTo(100L);
        }
    }

    @Nested
    @DisplayName("验证响应")
    class ValidationResponses {

        @Test
        void validSuccess_shouldSetValidTrue() {
            RESTResult<Void> r = RESTResult.validSuccess("通过");
            assertThat(r.getStatus()).isEqualTo(200);
            assertThat(r.isValid()).isTrue();
        }

        @Test
        void validError_shouldSetValidFalse() {
            RESTResult<Void> r = RESTResult.validError("不通过");
            assertThat(r.getStatus()).isEqualTo(200);
            assertThat(r.isValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("错误响应")
    class ErrorResponses {

        @Test
        void error_shouldReturnSpecifiedStatus() {
            RESTResult<Void> r = RESTResult.error(1001, "参数错误");
            assertThat(r.getStatus()).isEqualTo(1001);
            assertThat(r.getMessage()).isEqualTo("参数错误");
            assertThat(r.isSuccess()).isFalse();
        }

        @Test
        void getFailed_shouldReturn500() {
            RESTResult<Void> r = RESTResult.getFailed("服务器错误");
            assertThat(r.getStatus()).isEqualTo(500);
        }

        @Test
        void providerError_default_shouldReturn501() {
            RESTResult<Void> r = RESTResult.providerError();
            assertThat(r.getStatus()).isEqualTo(501);
            assertThat(r.getMessage()).contains("系统繁忙");
        }

        @Test
        void serviceError_default_shouldReturn501() {
            RESTResult<Void> r = RESTResult.serviceError();
            assertThat(r.getStatus()).isEqualTo(501);
        }

        @Test
        void dataNull_shouldReturn204() {
            RESTResult<Void> r = RESTResult.dataNull();
            assertThat(r.getStatus()).isEqualTo(204);
        }

        @Test
        void forbidden_shouldReturn403() {
            RESTResult<Void> r = RESTResult.forbidden();
            assertThat(r.getStatus()).isEqualTo(403);
        }
    }

    @Nested
    @DisplayName("工具方法")
    class UtilityMethods {

        @Test
        void setSuccess_true_shouldSet200() {
            RESTResult<Void> r = new RESTResult<>();
            r.setSuccess(true);
            assertThat(r.getStatus()).isEqualTo(200);
        }

        @Test
        void setSuccess_false_shouldSet500() {
            RESTResult<Void> r = new RESTResult<>();
            r.setSuccess(false);
            assertThat(r.getStatus()).isEqualTo(500);
        }

        @Test
        void traceId_shouldBeSettable() {
            RESTResult<Void> r = RESTResult.success("ok", null);
            r.setTraceId("abc-123");
            assertThat(r.getTraceId()).isEqualTo("abc-123");
        }
    }
}
