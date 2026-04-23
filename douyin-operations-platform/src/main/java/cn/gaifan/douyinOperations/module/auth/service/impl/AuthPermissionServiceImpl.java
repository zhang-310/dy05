package cn.gaifan.douyinOperations.module.auth.service.impl;

import cn.gaifan.douyinOperations.module.auth.entity.AuthResource;
import cn.gaifan.douyinOperations.module.auth.entity.AuthRole;
import cn.gaifan.douyinOperations.module.auth.entity.AuthUser;
import cn.gaifan.douyinOperations.module.auth.repository.AuthResourceRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthRoleRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthRoleResourceRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthUserRepository;
import cn.gaifan.douyinOperations.contract.auth.AuthPermissionService;
import cn.gaifan.douyinOperations.module.auth.service.AuthRoleService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * API 权限校验：角色-资源前缀匹配
 */
@Service
public class AuthPermissionServiceImpl implements AuthPermissionService {

    @Resource
    private AuthUserRepository authUserRepository;
    @Resource
    private AuthRoleRepository authRoleRepository;
    @Resource
    private AuthRoleResourceRepository authRoleResourceRepository;
    @Resource
    private AuthResourceRepository authResourceRepository;
    @Resource
    private AuthRoleService authRoleService;

    @Override
    public boolean hasPermission(Long userId, String uri, String method) {
        if (userId == null || uri == null) return false;
        // 去掉 query 部分
        int q = uri.indexOf('?');
        String path = q >= 0 ? uri.substring(0, q) : uri;

        Optional<AuthUser> userOpt = authUserRepository.findById(userId);
        if (!userOpt.isPresent()) return false;
        AuthUser user = userOpt.get();

        // admin 角色跳过权限校验
        if ("admin".equals(user.getRoleCode())) return true;

        Optional<AuthRole> roleOpt = authRoleRepository.findByRoleCodeAndDeleted(user.getRoleCode(), 0);
        if (!roleOpt.isPresent()) return false;
        Long roleId = roleOpt.get().getId();

        List<Long> resourceIds = authRoleService.getResourceIdsByRoleId(roleId);
        if (resourceIds == null || resourceIds.isEmpty()) return false;

        List<AuthResource> apiResources = authResourceRepository.findAllById(resourceIds).stream()
                .filter(r -> "api".equals(r.getResourceType()))
                .collect(Collectors.toList());

        for (AuthResource r : apiResources) {
            String code = r.getResourceCode();
            String prefix = code.endsWith("*") ? code.substring(0, code.length() - 1) : code;
            if (!path.startsWith(prefix)) continue;
            String reqMethod = r.getRequestMethod();
            if (reqMethod == null || "*".equals(reqMethod) || method != null && reqMethod.equalsIgnoreCase(method)) {
                return true;
            }
        }
        return false;
    }
}
