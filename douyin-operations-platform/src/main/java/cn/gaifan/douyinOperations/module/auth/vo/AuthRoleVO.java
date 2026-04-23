package cn.gaifan.douyinOperations.module.auth.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 角色列表/详情 VO
 */
@Data
public class AuthRoleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String roleCode;
    private String roleName;
    private Integer sortOrder;
    private Integer status;
    private java.sql.Timestamp createTime;
    private java.sql.Timestamp updateTime;
}
