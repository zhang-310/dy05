package cn.gaifan.douyinOperations.module.microservice;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 服务角色配置
 *
 * 通过环境变量 DY05_SERVICE_ROLE 控制当前进程扮演的角色：
 * - all（默认）: 单体模式，所有接口可用
 * - platform:    仅平台域（auth, platform, payment, governance 等）
 * - ai-mcp:      AI/MCP/知识库域
 * - content:     业务域（douyin, live, shortvideo 等）
 *
 * 参考 gaifan-ops ServiceRoleProperties
 */
@Component
@ConfigurationProperties(prefix = "dy05.service")
public class ServiceRoleProperties {

    /** 服务角色：all | platform | ai-mcp | content */
    private String role = "all";

    /** 模式：all-in-one（默认）| minimal-microservices */
    private String mode = "all-in-one";

    /** 允许的 API 前缀（由角色决定，无需手动配置） */
    private List<String> allowedApiPrefixes = List.of();

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public List<String> getAllowedApiPrefixes() { return allowedApiPrefixes; }
    public void setAllowedApiPrefixes(List<String> allowedApiPrefixes) { this.allowedApiPrefixes = allowedApiPrefixes; }

    public boolean isAllInOne() { return "all".equals(role) || "all-in-one".equals(mode); }
}
