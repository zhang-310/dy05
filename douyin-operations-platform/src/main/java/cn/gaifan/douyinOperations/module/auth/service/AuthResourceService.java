package cn.gaifan.douyinOperations.module.auth.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.auth.vo.AuthResourceSaveVO;
import cn.gaifan.douyinOperations.module.auth.vo.AuthResourceSearchVO;
import cn.gaifan.douyinOperations.module.auth.vo.AuthResourceVO;
import cn.gaifan.douyinOperations.module.auth.vo.MenuItemVO;
import cn.gaifan.douyinOperations.module.auth.vo.ResourceCodeVO;

import java.util.List;

/**
 * 资源：当前用户资源编码（菜单+API+按钮）；管理端资源 CRUD、菜单树
 */
public interface AuthResourceService {

    ResourceCodeVO getResourceCodes(Long userId);

    PageResultVO<AuthResourceVO> search(AuthResourceSearchVO vo);

    List<MenuItemVO> listMenuTree();

    /** 完整资源树（含 menu/api/button），用于角色资源分配 */
    List<MenuItemVO> listResourceTree();

    AuthResourceVO getById(Long id);

    long save(AuthResourceSaveVO vo);

    void deleteById(Long id);
}
