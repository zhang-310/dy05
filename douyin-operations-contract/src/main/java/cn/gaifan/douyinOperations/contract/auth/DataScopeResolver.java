package cn.gaifan.douyinOperations.contract.auth;

import java.util.List;

/**
 * 数据范围：按角色解析当前用户可见的 owner/user ID 列表。
 * null 表示不限制（如 admin）。
 */
public interface DataScopeResolver {

    List<Long> getVisibleUserIds(Long currentUserId, String roleCode);
}
