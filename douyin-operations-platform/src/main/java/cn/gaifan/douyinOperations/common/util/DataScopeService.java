package cn.gaifan.douyinOperations.common.util;

import cn.gaifan.douyinOperations.common.constant.RoleCode;
import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.module.auth.repository.AuthOrgMemberRepository;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据范围服务：根据角色决定可见的用户 ID 列表
 * - admin：看所有数据（返回 null 表示不限制）
 * - institution：看自己 + 旗下所有达人的数据
 * - talent / user：只看自己的数据
 */
@Service
public class DataScopeService implements DataScopeResolver {

    @Resource
    private AuthOrgMemberRepository orgMemberRepository;

    /**
     * 获取当前用户可见的 user_id 列表
     *
     * @param currentUserId 当前用户 ID
     * @param roleCode      当前用户角色
     * @return null 表示不限制（admin），非空列表表示只能看这些用户的数据
     */
    @Override
    public List<Long> getVisibleUserIds(Long currentUserId, String roleCode) {
        if (RoleCode.ADMIN.equals(roleCode)) {
            return null; // 不限制
        }
        if (RoleCode.INSTITUTION.equals(roleCode)) {
            List<Long> memberIds = orgMemberRepository.findMemberUserIdsByOrgOwnerId(currentUserId);
            List<Long> result = new ArrayList<>(memberIds.size() + 1);
            result.add(currentUserId);
            for (Long id : memberIds) {
                if (!id.equals(currentUserId)) {
                    result.add(id);
                }
            }
            return result;
        }
        // talent / user：只看自己
        return List.of(currentUserId);
    }
}
