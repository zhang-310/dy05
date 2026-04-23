package cn.gaifan.douyinOperations.module.auth.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 资源列表/详情 VO
 */
@Data
public class AuthResourceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String resourceType;
    private String resourceCode;
    private String requestMethod;
    private String module;
    private String resourceName;
    private Long parentId;
    private Integer sortOrder;
    private java.sql.Timestamp createTime;
    private java.sql.Timestamp updateTime;
}
