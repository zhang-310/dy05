package cn.gaifan.douyinOperations.common.tenant;

import cn.gaifan.douyinOperations.module.auth.entity.AuthUser;
import cn.gaifan.douyinOperations.module.auth.repository.AuthUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 按用户解析租户组织 ID（来自 auth_user.organization_id），用于写入 org_id 冗余列。
 */
@Component
public class TenantOrgResolutionHelper {

    @Autowired(required = false)
    private AuthUserRepository authUserRepository;

    public Long organizationIdForUser(Long userId) {
        if (userId == null || authUserRepository == null) {
            return null;
        }
        return authUserRepository.findById(userId).map(AuthUser::getOrganizationId).orElse(null);
    }
}
