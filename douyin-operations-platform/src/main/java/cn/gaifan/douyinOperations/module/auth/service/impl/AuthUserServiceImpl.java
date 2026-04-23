package cn.gaifan.douyinOperations.module.auth.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.auth.entity.AuthLoginLog;
import cn.gaifan.douyinOperations.module.auth.entity.AuthOrganization;
import cn.gaifan.douyinOperations.module.auth.entity.AuthUser;
import cn.gaifan.douyinOperations.module.auth.repository.AuthLoginLogRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthOrganizationRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthUserRepository;
import cn.gaifan.douyinOperations.module.auth.service.AuthUserService;
import cn.gaifan.douyinOperations.module.auth.vo.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import jakarta.persistence.criteria.Predicate;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户：个人信息、管理员-用户 CRUD、封禁、登录记录
 */
@Service
public class AuthUserServiceImpl implements AuthUserService {

    /** 新增用户时未填密码的默认密码（生产建议从配置读取） */
    private static final String DEFAULT_INITIAL_PASSWORD = "123456";
    private static final int LOGIN_LOGS_PAGE_SIZE_MAX = 100;
    private static final int ONLINE_DEFAULT_WITHIN_MINUTES = 30;
    private static final int ONLINE_DEFAULT_MAX_SIZE = 100;

    @Resource
    private AuthUserRepository authUserRepository;
    @Resource
    private AuthLoginLogRepository authLoginLogRepository;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private AuthOrganizationRepository authOrganizationRepository;

    @Override
    public ProfileVO getProfile(Long userId) {
        AuthUser u = authUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        ProfileVO vo = new ProfileVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setMobile(u.getMobile());
        vo.setEmail(u.getEmail());
        vo.setNickname(u.getNickname());
        vo.setAvatarUrl(u.getAvatarUrl());
        vo.setRoleCode(u.getRoleCode());
        vo.setOrganizationId(u.getOrganizationId());
        vo.setCreateTime(u.getCreateTime());
        if (u.getOrganizationId() != null) {
            authOrganizationRepository.findById(u.getOrganizationId())
                    .ifPresent(org -> vo.setOrganizationName(org.getOrgName()));
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "users", key = "#userId")
    public void updateProfile(Long userId, ProfileUpdateVO vo) {
        AuthUser u = authUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        if (vo.getNickname() != null) u.setNickname(vo.getNickname());
        if (vo.getAvatarUrl() != null) u.setAvatarUrl(vo.getAvatarUrl());
        if (vo.getMobile() != null) u.setMobile(vo.getMobile());
        if (vo.getEmail() != null) u.setEmail(vo.getEmail());
        authUserRepository.save(u);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "users", key = "#userId")
    public void changePassword(Long userId, ChangePasswordVO vo) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "用户 ID 无效");
        }
        AuthUser u = authUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        if (!passwordEncoder.matches(vo.getOldPassword(), u.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH, "当前密码错误");
        }
        u.setPasswordHash(passwordEncoder.encode(vo.getNewPassword()));
        authUserRepository.save(u);
    }

    /** 用户列表允许的排序字段（与 AuthUser 属性一致，避免非法 sortName 导致 JPA 异常） */
    private static final Set<String> SORTABLE_FIELDS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("id", "username", "createTime", "updateTime", "lastLoginAt", "roleCode", "status")));

    @Override
    public PageResultVO<AuthUserVO> search(AuthUserSearchVO vo, Long callerOrganizationId, boolean callerIsAdmin) {
        vo.validateParams();
        String sortName = SORTABLE_FIELDS.contains(vo.getSortName()) ? vo.getSortName() : "id";
        Pageable pageable = PageRequest.of(vo.getPage(), vo.getRows(),
                Sort.by("desc".equalsIgnoreCase(vo.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC, sortName));

        Specification<AuthUser> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (!callerIsAdmin && callerOrganizationId != null) {
                predicates.add(cb.equal(root.get("organizationId"), callerOrganizationId));
            }
            if (vo.getKeyword() != null && !vo.getKeyword().trim().isEmpty()) {
                String kw = "%" + vo.getKeyword().trim() + "%";
                predicates.add(cb.or(
                    cb.like(root.get("username"), kw),
                    cb.like(root.get("nickname"), kw),
                    cb.like(root.get("mobile"), kw),
                    cb.like(root.get("email"), kw)
                ));
            }
            if (vo.getUsername() != null && !vo.getUsername().trim().isEmpty()) {
                predicates.add(cb.like(root.get("username"), "%" + vo.getUsername().trim() + "%"));
            }
            if (vo.getNickname() != null && !vo.getNickname().trim().isEmpty()) {
                predicates.add(cb.like(root.get("nickname"), "%" + vo.getNickname().trim() + "%"));
            }
            if (vo.getMobile() != null && !vo.getMobile().trim().isEmpty()) {
                predicates.add(cb.like(root.get("mobile"), "%" + vo.getMobile().trim() + "%"));
            }
            if (vo.getRoleCode() != null && !vo.getRoleCode().trim().isEmpty()) {
                predicates.add(cb.equal(root.get("roleCode"), vo.getRoleCode().trim()));
            }
            if (vo.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), vo.getStatus()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AuthUser> page = authUserRepository.findAll(spec, pageable);
        List<AuthUserVO> list = page.getContent().stream().map(this::toUserVO).collect(Collectors.toList());
        return PageResultVO.of(page.getTotalElements(), list, vo.getPage(), vo.getRows());
    }

    @Override
    @Cacheable(value = "users", key = "#id")
    public AuthUserVO getById(Long id, Long callerOrganizationId, boolean callerIsAdmin) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "用户 ID 无效");
        }
        AuthUser u = authUserRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        if (!callerIsAdmin && callerOrganizationId != null) {
            if (u.getOrganizationId() == null || !u.getOrganizationId().equals(callerOrganizationId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权限查看该用户");
            }
        }
        return toUserVO(u);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "users", allEntries = true)
    public long save(AuthUserSaveVO vo, Long callerOrganizationId, boolean callerIsAdmin) {
        AuthUser u;
        if (vo.getId() != null && vo.getId() > 0) {
            u = authUserRepository.findById(vo.getId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
            if (!callerIsAdmin && callerOrganizationId != null) {
                if (u.getOrganizationId() == null || !u.getOrganizationId().equals(callerOrganizationId)) {
                    throw new BusinessException(ErrorCode.FORBIDDEN, "无权限编辑该用户");
                }
            }
        } else {
            if (authUserRepository.existsByUsernameAndDeleted(vo.getUsername(), 0)) {
                throw new BusinessException(ErrorCode.USERNAME_EXISTS, "用户名已存在");
            }
            u = new AuthUser();
            u.setUsername(vo.getUsername());
            u.setPasswordHash(passwordEncoder.encode(vo.getPassword() != null && !vo.getPassword().isEmpty() ? vo.getPassword() : DEFAULT_INITIAL_PASSWORD));
            if (!callerIsAdmin && callerOrganizationId != null) {
                u.setOrganizationId(callerOrganizationId);
            }
        }
        u.setNickname(vo.getNickname());
        u.setMobile(vo.getMobile());
        u.setEmail(vo.getEmail());
        u.setAvatarUrl(vo.getAvatarUrl());
        u.setRoleCode(vo.getRoleCode());
        if (vo.getPassword() != null && !vo.getPassword().isEmpty()) {
            u.setPasswordHash(passwordEncoder.encode(vo.getPassword()));
        }
        u = authUserRepository.save(u);
        return u.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "users", key = "#userId")
    public void ban(Long userId, boolean ban, String reason, Long callerOrganizationId, boolean callerIsAdmin) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "用户 ID 无效");
        }
        AuthUser u = authUserRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        if (!callerIsAdmin && callerOrganizationId != null
                && (u.getOrganizationId() == null || !u.getOrganizationId().equals(callerOrganizationId))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅可操作本机构用户");
        }
        u.setStatus(ban ? 1 : 0);
        u.setBannedAt(ban ? new Timestamp(System.currentTimeMillis()) : null);
        u.setBannedReason(reason != null && reason.length() > 256 ? reason.substring(0, 256) : reason);
        authUserRepository.save(u);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "users", allEntries = true)
    public void deleteById(Long id, Long callerOrganizationId, boolean callerIsAdmin) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "用户 ID 无效");
        }
        AuthUser u = authUserRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        if (!callerIsAdmin && callerOrganizationId != null
                && (u.getOrganizationId() == null || !u.getOrganizationId().equals(callerOrganizationId))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅可操作本机构用户");
        }
        u.setDeleted(1);
        authUserRepository.save(u);
    }

    @Override
    public List<LoginLogVO> getLoginLogs(Long userId, int page, int size) {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        if (size > LOGIN_LOGS_PAGE_SIZE_MAX) size = LOGIN_LOGS_PAGE_SIZE_MAX;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "loginTime"));
        if (userId != null && userId > 0) {
            return authLoginLogRepository.findByUserIdOrderByLoginTimeDesc(userId, pageable)
                    .getContent().stream().map(this::toLogVO).collect(Collectors.toList());
        }
        return authLoginLogRepository.findAllByOrderByLoginTimeDesc(pageable)
                .getContent().stream().map(this::toLogVO).collect(Collectors.toList());
    }

    private AuthUserVO toUserVO(AuthUser u) {
        AuthUserVO vo = new AuthUserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setMobile(u.getMobile());
        vo.setEmail(u.getEmail());
        vo.setNickname(u.getNickname());
        vo.setAvatarUrl(u.getAvatarUrl());
        vo.setRoleCode(u.getRoleCode());
        vo.setStatus(u.getStatus());
        vo.setBannedAt(u.getBannedAt());
        vo.setBannedReason(u.getBannedReason());
        vo.setLastLoginAt(u.getLastLoginAt());
        vo.setCreateTime(u.getCreateTime());
        return vo;
    }

    @Override
    public List<OnlineUserVO> getOnlineUsers(int withinMinutes, int maxSize) {
        if (withinMinutes <= 0) withinMinutes = ONLINE_DEFAULT_WITHIN_MINUTES;
        if (maxSize <= 0 || maxSize > 500) maxSize = ONLINE_DEFAULT_MAX_SIZE;
        long afterMs = System.currentTimeMillis() - withinMinutes * 60 * 1000L;
        Timestamp after = new Timestamp(afterMs);
        List<AuthLoginLog> logs = authLoginLogRepository.findTop500ByLoginTimeAfterOrderByLoginTimeDesc(after);
        // 按 userId 去重，保留每条用户最近一条登录记录（列表已按 loginTime 倒序）
        Map<Long, AuthLoginLog> latestByUser = new LinkedHashMap<>();
        for (AuthLoginLog log : logs) {
            if (log.getUserId() == null) continue;  // skip failed login logs
            latestByUser.putIfAbsent(log.getUserId(), log);
            if (latestByUser.size() >= maxSize) break;
        }
        List<OnlineUserVO> result = new ArrayList<>(latestByUser.size());
        for (AuthLoginLog log : latestByUser.values()) {
            AuthUser u = authUserRepository.findById(log.getUserId()).orElse(null);
            OnlineUserVO vo = new OnlineUserVO();
            vo.setUserId(log.getUserId());
            vo.setUsername(u != null ? u.getUsername() : null);
            vo.setNickname(u != null ? u.getNickname() : null);
            vo.setLastLoginAt(log.getLoginTime());
            vo.setIp(log.getIp());
            vo.setDeviceType(log.getDeviceType());
            vo.setLoginType(log.getLoginType());
            result.add(vo);
        }
        return result;
    }

    private LoginLogVO toLogVO(AuthLoginLog l) {
        LoginLogVO vo = new LoginLogVO();
        vo.setId(l.getId());
        vo.setUserId(l.getUserId());
        vo.setUsername(l.getUsername());
        vo.setLoginType(l.getLoginType());
        vo.setDeviceType(l.getDeviceType());
        vo.setIp(l.getIp());
        vo.setUserAgent(l.getUserAgent());
        vo.setStatus(l.getStatus());
        vo.setFailReason(l.getFailReason());
        vo.setLoginTime(l.getLoginTime());
        return vo;
    }
}
