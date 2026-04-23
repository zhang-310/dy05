package cn.gaifan.douyinOperations.contract.auth;

/**
 * API 权限校验 SPI：根据用户与请求 URI/Method 判断是否有权限。
 */
public interface AuthPermissionService {

    boolean hasPermission(Long userId, String uri, String method);
}
