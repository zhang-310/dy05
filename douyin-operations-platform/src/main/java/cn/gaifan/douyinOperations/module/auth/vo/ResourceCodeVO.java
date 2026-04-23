package cn.gaifan.douyinOperations.module.auth.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 当前用户拥有的资源编码列表（菜单+API+按钮），前端用于按钮级权限
 */
@Data
public class ResourceCodeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<String> menus;
    private List<String> apis;
    private List<String> buttons;
}
