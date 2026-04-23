package cn.gaifan.douyinOperations.common.util;

import jakarta.servlet.http.HttpServletRequest;

public class IPUtils {
    /**
     * 获取用户真实IP地址
     * <p>
     * 不使用request.getRemoteAddr()的原因是有可能用户使用了代理软件方式避免真实IP地址。
     * 如果通过了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串IP值，会取第一个IP作为真实IP。
     * </p>
     * <p>
     * 检查顺序：
     * 1. X-Forwarded-For (优先检查，需处理逗号分隔的多个IP)
     * 2. Proxy-Client-IP
     * 3. WL-Proxy-Client-IP
     * 4. HTTP_CLIENT_IP
     * 5. HTTP_X_FORWARDED_FOR
     * 6. X-Real-IP
     * 7. request.getRemoteAddr() (最后兜底)
     * </p>
     *
     * @param request HTTP请求对象
     * @return 用户真实IP地址，如果无法获取则返回request.getRemoteAddr()，如果getRemoteAddr()也为null则返回"0.0.0.0"
     */
    public static String getRealIP(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        // 优先检查 X-Forwarded-For，可能需要处理多个IP
        String ip = getHeaderValue(request, "X-Forwarded-For");
        if (isValidIp(ip)) {
            // 多次反向代理后会有多个ip值，第一个ip才是真实ip
            int commaIndex = ip.indexOf(',');
            if (commaIndex != -1) {
                ip = ip.substring(0, commaIndex).trim();
            }
            // 重新验证处理后的IP是否有效（trim后可能变成空字符串）
            if (isValidIp(ip)) {
                return ip;
            }
        }

        // 按优先级顺序检查其他HTTP头
        String[] headers = {
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR",
                "X-Real-IP"
        };

        for (String header : headers) {
            ip = getHeaderValue(request, header);
            if (isValidIp(ip)) {
                return ip;
            }
        }

        // 最后使用 getRemoteAddr() 作为兜底方案
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null) ? remoteAddr : "0.0.0.0";
    }

    /**
     * 获取HTTP请求头值并去除首尾空白字符
     *
     * @param request HTTP请求对象
     * @param headerName 请求头名称（不区分大小写）
     * @return 去除空白字符后的请求头值，如果请求头不存在则返回null
     */
    private static String getHeaderValue(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        if (value != null) {
            value = value.trim();
            // trim后可能变成空字符串，统一返回null
            if (value.isEmpty()) {
                return null;
            }
        }
        return value;
    }

    /**
     * 验证IP地址是否有效
     * 排除null、空字符串和"unknown"字符串
     *
     * @param ip IP地址字符串
     * @return 如果IP有效返回true，否则返回false
     */
    private static boolean isValidIp(String ip) {
        return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip);
    }
}