package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 实时协作在线状态 Controller（P3-02 升级：优先使用 Redis，降级为本地内存）
 * <p>
 * 存储结构：Redis Hash {@code live:collab:presence:{sessionId}} → field={userId} value=JSON(UserPresence)
 * TTL 1小时，用户加入时刷新 TTL。多实例部署下共享在线状态。
 */
@RestController
@RequestMapping("/api/v1/live/collaboration")
@Tag(name = "协作在线状态 / Collaboration Presence", description = "实时协作在线状态管理（需登录）")
public class LiveCollaborationPresenceController {

    private static final Logger log = LoggerFactory.getLogger(LiveCollaborationPresenceController.class);
    private static final String PRESENCE_KEY_PREFIX = "live:collab:presence:";
    private static final long PRESENCE_TTL_HOURS = 1L;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    /** 降级：单实例内存存储（Redis 不可用时） */
    private final ConcurrentHashMap<Long, Set<UserPresence>> localPresenceMap = new ConcurrentHashMap<>();

    @PostMapping("/join")
    @Operation(summary = "加入场次 / Join Session",
            description = "注册在线状态，标记用户正在查看某场次")
    public RESTResult<Void> join(HttpServletRequest request,
                                 @RequestBody(required = false) Map<String, Object> body) {
        Long userId = requireUserId(request);
        Long sessionId = parseLong(body, "sessionId");
        if (sessionId == null) {
            return withTraceId(RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId"));
        }
        String userName = body != null && body.get("userName") != null ? body.get("userName").toString() : "User-" + userId;
        UserPresence presence = new UserPresence(userId, userName, new Timestamp(System.currentTimeMillis()));
        if (redisTemplate != null) {
            try {
                String key = PRESENCE_KEY_PREFIX + sessionId;
                String value = objectMapper.writeValueAsString(presence);
                redisTemplate.opsForHash().put(key, String.valueOf(userId), value);
                redisTemplate.expire(key, PRESENCE_TTL_HOURS, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("Redis presence join 失败，降级为内存: {}", e.getMessage());
                joinLocal(userId, sessionId, presence);
            }
        } else {
            joinLocal(userId, sessionId, presence);
        }
        log.debug("User {} joined session {}", userId, sessionId);
        return withTraceId(RESTResult.success());
    }

    @PostMapping("/leave")
    @Operation(summary = "离开场次 / Leave Session",
            description = "移除在线状态")
    public RESTResult<Void> leave(HttpServletRequest request,
                                  @RequestBody(required = false) Map<String, Object> body) {
        Long userId = requireUserId(request);
        Long sessionId = parseLong(body, "sessionId");
        if (sessionId == null) {
            return withTraceId(RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId"));
        }
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForHash().delete(PRESENCE_KEY_PREFIX + sessionId, String.valueOf(userId));
            } catch (Exception e) {
                log.warn("Redis presence leave 失败: {}", e.getMessage());
                leaveLocal(userId, sessionId);
            }
        } else {
            leaveLocal(userId, sessionId);
        }
        log.debug("User {} left session {}", userId, sessionId);
        return withTraceId(RESTResult.success());
    }

    @PostMapping("/viewers")
    @Operation(summary = "查看在线用户 / Get Viewers",
            description = "获取当前场次的所有在线用户列表")
    public RESTResult<List<Map<String, Object>>> viewers(HttpServletRequest request,
                                                          @RequestBody(required = false) Map<String, Object> body) {
        requireUserId(request);
        Long sessionId = parseLong(body, "sessionId");
        if (sessionId == null) {
            return withTraceId(RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId"));
        }
        List<Map<String, Object>> viewers = new ArrayList<>();
        if (redisTemplate != null) {
            try {
                Map<Object, Object> entries = redisTemplate.opsForHash().entries(PRESENCE_KEY_PREFIX + sessionId);
                for (Object val : entries.values()) {
                    try {
                        UserPresence p = objectMapper.readValue(val.toString(), UserPresence.class);
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("userId", p.userId);
                        item.put("userName", p.userName);
                        item.put("joinedAt", p.joinedAt);
                        viewers.add(item);
                    } catch (Exception ignore) {}
                }
            } catch (Exception e) {
                log.warn("Redis presence viewers 失败，降级为内存: {}", e.getMessage());
                viewers = viewersLocal(sessionId);
            }
        } else {
            viewers = viewersLocal(sessionId);
        }
        return withTraceId(RESTResult.getSuccess(viewers));
    }

    // ── 内存降级方法 ──────────────────────────────────────────────

    private void joinLocal(Long userId, Long sessionId, UserPresence presence) {
        localPresenceMap.compute(sessionId, (k, set) -> {
            Set<UserPresence> s = set != null ? set : ConcurrentHashMap.newKeySet();
            s.removeIf(p -> p.userId.equals(userId));
            s.add(presence);
            return s;
        });
    }

    private void leaveLocal(Long userId, Long sessionId) {
        Set<UserPresence> set = localPresenceMap.get(sessionId);
        if (set != null) {
            set.removeIf(p -> p.userId.equals(userId));
            if (set.isEmpty()) localPresenceMap.remove(sessionId);
        }
    }

    private List<Map<String, Object>> viewersLocal(Long sessionId) {
        Set<UserPresence> set = localPresenceMap.getOrDefault(sessionId, Collections.emptySet());
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserPresence p : set) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", p.userId);
            item.put("userName", p.userName);
            item.put("joinedAt", p.joinedAt);
            result.add(item);
        }
        return result;
    }

    // ==================== UserPresence 内部记录 ====================

    public static class UserPresence {
        public Long userId;
        public String userName;
        public Timestamp joinedAt;

        public UserPresence() {}

        public UserPresence(Long userId, String userName, Timestamp joinedAt) {
            this.userId = userId;
            this.userName = userName;
            this.joinedAt = joinedAt;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return Objects.equals(userId, ((UserPresence) o).userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId);
        }
    }

    // ==================== 工具方法 ====================

    private Long requireUserId(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        return userId;
    }

    private <T> RESTResult<T> withTraceId(RESTResult<T> r) {
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    private static Long parseLong(Map<String, Object> body, String key) {
        if (body == null) return null;
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
