package cn.gaifan.douyinOperations.module.auth.dto;

/**
 * AuthUser Projection DTO - 仅包含必要字段，减少数据传输
 */
public interface AuthUserDTO {
    Long getId();
    String getUsername();
    String getNickname();
    String getMobile();
    String getEmail();
    String getAvatarUrl();
    String getRoleCode();
    Integer getStatus();
    java.sql.Timestamp getLastLoginAt();
    java.sql.Timestamp getCreateTime();
}
