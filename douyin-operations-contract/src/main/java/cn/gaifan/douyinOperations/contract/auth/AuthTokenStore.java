package cn.gaifan.douyinOperations.contract.auth;

/**
 * Token 存储（生成、校验、获取用户信息）— SPI，实现位于 platform/auth。
 */
public interface AuthTokenStore {

    String createToken(Long userId, String roleCode);

    String createToken(Long userId, String roleCode, Long organizationId);

    Long getUserId(String token);

    String getRoleCode(String token);

    Long getOrganizationId(String token);

    void removeToken(String token);

    boolean isValid(String token);
}
