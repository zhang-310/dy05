package cn.gaifan.douyinOperations.common.exception;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerCreditTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void insufficientCreditsReturns402() {
        BusinessException ex = new BusinessException(ErrorCode.INSUFFICIENT_CREDITS, "积分不足");
        ResponseEntity<RESTResult<?>> resp = handler.handleBusiness(ex, new MockHttpServletRequest());
        assertEquals(HttpStatus.PAYMENT_REQUIRED, resp.getStatusCode());
        assertEquals(ErrorCode.INSUFFICIENT_CREDITS, resp.getBody().getStatus());
    }

    @Test
    void forbiddenReturns200() {
        BusinessException ex = new BusinessException(ErrorCode.FORBIDDEN, "无权限");
        ResponseEntity<RESTResult<?>> resp = handler.handleBusiness(ex, new MockHttpServletRequest());
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(ErrorCode.FORBIDDEN, resp.getBody().getStatus());
    }
}
