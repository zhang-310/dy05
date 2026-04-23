package cn.gaifan.douyinOperations.module.benchmark.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * waiting：等待扫码；success：已登录 cookieValue 为浏览器 Cookie 请求头格式；expired：超时或会话不存在；error：异常。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DouyinQrLoginPollResultVO {

    private String status;

    /** success 时：当前 Playwright 上下文的全部 Cookie 拼成一条请求头（name=value; name2=value2; …） */
    private String cookieValue;

    private String message;
}
