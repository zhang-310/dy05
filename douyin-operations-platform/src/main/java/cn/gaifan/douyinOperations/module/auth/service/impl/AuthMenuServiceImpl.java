package cn.gaifan.douyinOperations.module.auth.service.impl;

import cn.gaifan.douyinOperations.module.auth.entity.AuthResource;
import cn.gaifan.douyinOperations.module.auth.entity.AuthRole;
import cn.gaifan.douyinOperations.module.auth.entity.AuthUser;
import cn.gaifan.douyinOperations.module.auth.repository.AuthResourceRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthRoleRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthRoleResourceRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthUserRepository;
import cn.gaifan.douyinOperations.module.auth.service.AuthMenuService;
import cn.gaifan.douyinOperations.module.auth.vo.MenuItemVO;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 当前用户可访问菜单（按角色-资源查 menu 类型，组树）
 */
@Service
public class AuthMenuServiceImpl implements AuthMenuService {

    @Resource
    private AuthUserRepository authUserRepository;
    @Resource
    private AuthRoleRepository authRoleRepository;
    @Resource
    private AuthRoleResourceRepository authRoleResourceRepository;
    @Resource
    private AuthResourceRepository authResourceRepository;

    @Override
    public List<MenuItemVO> getMenuList(Long userId) {
        Optional<AuthUser> userOpt = authUserRepository.findById(userId);
        if (!userOpt.isPresent()) return Collections.emptyList();
        Optional<AuthRole> roleOpt = authRoleRepository.findByRoleCodeAndDeleted(userOpt.get().getRoleCode(), 0);
        if (!roleOpt.isPresent()) return Collections.emptyList();
        List<Long> resourceIds = authRoleResourceRepository.findByRoleId(roleOpt.get().getId()).stream()
                .map(rr -> rr.getResourceId()).collect(Collectors.toList());
        if (resourceIds.isEmpty()) return Collections.emptyList();
        List<AuthResource> all = authResourceRepository.findAllById(resourceIds);
        List<AuthResource> menus = all.stream().filter(r -> "menu".equals(r.getResourceType()))
                .sorted(Comparator.comparing(AuthResource::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
        return buildTree(menus, 0L);
    }

    private List<MenuItemVO> buildTree(List<AuthResource> menus, Long parentId) {
        List<MenuItemVO> result = new ArrayList<>();
        for (AuthResource r : menus) {
            if (!Objects.equals(r.getParentId() != null ? r.getParentId() : 0L, parentId)) continue;
            MenuItemVO vo = new MenuItemVO();
            vo.setId(r.getId());
            vo.setResourceCode(r.getResourceCode());
            vo.setResourceName(r.getResourceName());
            vo.setParentId(r.getParentId());
            vo.setSortOrder(r.getSortOrder());
            vo.setChildren(buildTree(menus, r.getId()));
            result.add(vo);
        }
        result.sort(Comparator.comparing(MenuItemVO::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }
}
