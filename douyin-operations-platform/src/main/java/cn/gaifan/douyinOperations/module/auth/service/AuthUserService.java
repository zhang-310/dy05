package cn.gaifan.douyinOperations.module.auth.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.auth.vo.AuthUserVO;
import cn.gaifan.douyinOperations.module.auth.vo.AuthUserSearchVO;
import cn.gaifan.douyinOperations.module.auth.vo.LoginLogQueryVO;
import cn.gaifan.douyinOperations.module.auth.vo.LoginLogVO;
import cn.gaifan.douyinOperations.module.auth.vo.OnlineUserVO;
import cn.gaifan.douyinOperations.module.auth.vo.ProfileVO;
import cn.gaifan.douyinOperations.module.auth.vo.ProfileUpdateVO;
import cn.gaifan.douyinOperations.module.auth.vo.ChangePasswordVO;
import cn.gaifan.douyinOperations.module.auth.vo.AuthUserSaveVO;

import java.util.List;

/**
 * 用户相关：个人信息、管理员-用户列表/详情/保存/封禁/登录记录、在线用户列表
 */
public interface AuthUserService {

    ProfileVO getProfile(Long userId);

    void updateProfile(Long userId, ProfileUpdateVO vo);

    /**
     * 修改当前用户登录密码（需校验旧密码）
     */
    void changePassword(Long userId, ChangePasswordVO vo);

    /**
     * 分页搜索用户。多租户隔离：非 admin 且 callerOrganizationId 非空时仅返回同机构用户。
     * @param callerOrganizationId 当前请求用户所属机构 ID（可为 null）
     * @param callerIsAdmin 当前请求用户是否为 admin 角色
     */
    PageResultVO<AuthUserVO> search(AuthUserSearchVO vo, Long callerOrganizationId, boolean callerIsAdmin);

    /**
     * 按 ID 获取用户。多租户隔离：非 admin 且 callerOrganizationId 非空时，仅允许查看同机构用户。
     */
    AuthUserVO getById(Long id, Long callerOrganizationId, boolean callerIsAdmin);

    /**
     * 保存用户。多租户隔离：非 admin 且 callerOrganizationId 非空时，新建用户绑定该机构，编辑时仅允许同机构用户。
     */
    long save(AuthUserSaveVO vo, Long callerOrganizationId, boolean callerIsAdmin);

    /**
     * 封禁/解封用户。多租户：非 admin 且 callerOrganizationId 非空时，仅允许操作同机构用户。
     */
    void ban(Long userId, boolean ban, String reason, Long callerOrganizationId, boolean callerIsAdmin);

    /**
     * 逻辑删除用户。多租户：非 admin 且 callerOrganizationId 非空时，仅允许操作同机构用户。
     */
    void deleteById(Long id, Long callerOrganizationId, boolean callerIsAdmin);

    /** 登录日志分页查询；管理员可按 userId/username/status 筛选，普通用户仅能查自己 */
    PageResultVO<LoginLogVO> getLoginLogs(LoginLogQueryVO vo);

    /**
     * 在线用户列表（最近一段时间内有登录记录的用户，按最后登录时间倒序；首版基于 auth_login_log）
     *
     * @param withinMinutes 统计最近多少分钟内的登录（如 30）
     * @param maxSize        最多返回条数（如 100）
     * @return 去重后的用户+最后登录信息
     */
    List<OnlineUserVO> getOnlineUsers(int withinMinutes, int maxSize);
}
