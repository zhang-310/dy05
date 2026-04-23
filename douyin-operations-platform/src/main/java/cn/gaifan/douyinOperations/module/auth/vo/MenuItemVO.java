package cn.gaifan.douyinOperations.module.auth.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 菜单项（当前用户可访问菜单）
 */
@Data
public class MenuItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String resourceCode;
    private String resourceName;
    private String resourceType;
    private String module;
    private Long parentId;
    private Integer sortOrder;
    private List<MenuItemVO> children;
}
