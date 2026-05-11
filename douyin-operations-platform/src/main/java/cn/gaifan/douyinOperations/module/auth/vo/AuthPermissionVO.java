package cn.gaifan.douyinOperations.module.auth.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限 VO
 */
@Data
public class AuthPermissionVO {
    private Long id;
    private String name;
    private String code;
    private String type;
    private String description;
    private LocalDateTime createTime;
}
