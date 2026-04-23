package cn.gaifan.douyinOperations.module.log.config;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * 包装 Response 以捕获 HTTP 状态码（用于操作日志记录）
 */
public class StatusCaptureResponseWrapper extends HttpServletResponseWrapper {

    private int status = 200;

    public StatusCaptureResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public void setStatus(int sc) {
        this.status = sc;
        super.setStatus(sc);
    }

    @Override
    public void sendError(int sc) throws java.io.IOException {
        this.status = sc;
        super.sendError(sc);
    }

    @Override
    public void sendError(int sc, String msg) throws java.io.IOException {
        this.status = sc;
        super.sendError(sc, msg);
    }

    public int getCapturedStatus() {
        return status;
    }
}
