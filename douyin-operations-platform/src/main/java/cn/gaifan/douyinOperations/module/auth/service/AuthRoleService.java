package cn.gaifan.douyinOperations.module.auth.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.auth.vo.AuthRoleSaveVO;
import cn.gaifan.douyinOperations.module.auth.vo.AuthRoleSearchVO;
import cn.gaifan.douyinOperations.module.auth.vo.AuthRoleVO;

import java.util.List;

/**
 * 角色管理（仅管理员）：分页列表、全部列表、详情、保存、逻辑删除、角色-资源授权
 */
public interface AuthRoleService {

    PageResultVO<AuthRoleVO> search(AuthRoleSearchVO vo);

    List<AuthRoleVO> listAll();

    AuthRoleVO getById(Long id);

    long save(AuthRoleSaveVO vo);

    void deleteById(Long id);

    List<Long> getResourceIdsByRoleId(Long roleId);

    void saveRoleResources(Long roleId, List<Long> resourceIds);
}
