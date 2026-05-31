package cn.gaifan.douyinOperations.common.vo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RESTResultTest {

    @Test
    void successSetsStatus200() {
        var r = RESTResult.getSuccess("data");
        assertEquals(200, r.getStatus());
        assertEquals("data", r.getData());
    }

    @Test
    void errorSetsStatus() {
        var r = RESTResult.error(404, "not found");
        assertEquals(404, r.getStatus());
        assertEquals("not found", r.getMessage());
    }

    @Test
    void traceIdCanBeSet() {
        var r = RESTResult.success(null);
        r.setTraceId("trace-001");
        assertEquals("trace-001", r.getTraceId());
    }
}
