package cn.gaifan.douyinOperations.module.auth.service;

import cn.gaifan.douyinOperations.module.auth.vo.MenuItemVO;

import java.util.List;

/**
 * 当前用户可访问菜单（树形）
 */
public interface AuthMenuService {

    List<MenuItemVO> getMenuList(Long userId);
}
