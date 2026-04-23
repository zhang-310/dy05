package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.live.config.LiveRealtimeProperties;
import cn.gaifan.douyinOperations.module.live.realtime.LiveRealtimeSseHub;
import cn.gaifan.douyinOperations.module.live.service.LiveRealtimePanelService;
import cn.gaifan.douyinOperations.module.live.service.LiveRealtimeSuggestionService;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionRealtimeDataVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionScriptSlotVO;
import cn.gaifan.douyinOperations.module.live.vo.RealtimeSuggestionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 实时指标/弹幕 ingest 后，在配置开启且命中白名单与频控时自动执行建议（含结构化审计日志）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveRealtimeAutoSuggestionExecutor {

    private static final String KEY_COOLDOWN = "live:rt:auto:cooldown:";
    private static final String KEY_JUMP_HOUR = "live:rt:auto:jump:";

    private final LiveRealtimeProperties liveRealtimeProperties;
    private final LiveRealtimeSuggestionService suggestionService;
    private final LiveRealtimePanelService liveRealtimePanelService;
    private final LiveRealtimeSseHub liveRealtimeSseHub;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 在实时数据更新或弹幕 ingest 后调用；失败不影响主流程。
     */
    public void tryAutoExecute(Long liveSessionId, Long userId, LiveSessionRealtimeDataVO data) {
        if (liveSessionId == null || userId == null || data == null) {
            return;
        }
        LiveRealtimeProperties.Suggestion cfg = liveRealtimeProperties.getSuggestion();
        if (!cfg.isAutoExecutionEnabled()) {
            return;
        }
        try {
            doTryAuto(liveSessionId, userId, data, cfg);
        } catch (Exception e) {
            log.debug("auto suggestion skipped: session={} err={}", liveSessionId, e.getMessage());
        }
    }

    private void doTryAuto(Long liveSessionId, Long userId, LiveSessionRealtimeDataVO data,
                          LiveRealtimeProperties.Suggestion cfg) {
        Set<String> allowed = cfg.getAllowedAutoActionTypes() == null ? Set.of()
                : cfg.getAllowedAutoActionTypes().stream()
                .map(s -> s == null ? "" : s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        if (allowed.isEmpty()) {
            return;
        }

        List<RealtimeSuggestionVO> list = suggestionService.evaluateSuggestions(data, liveSessionId);
        RealtimeSuggestionVO pick = list.stream()
                .filter(s -> s != null && s.getActionType() != null)
                .filter(s -> s.getUrgency() >= cfg.getMinUrgencyForAuto())
                .filter(s -> allowed.contains(s.getActionType().trim().toLowerCase(Locale.ROOT)))
                .filter(s -> !"jump_slot".equalsIgnoreCase(s.getActionType().trim()))
                .max(Comparator.comparingInt(RealtimeSuggestionVO::getUrgency))
                .orElse(null);
        if (pick == null) {
            return;
        }

        String action = pick.getActionType().trim().toLowerCase(Locale.ROOT);
        String traceId = MDC.get("traceId");
        String ruleType = pick.getType() != null ? pick.getType() : "";

        if (!acquireCooldown(liveSessionId, cfg.getMinCooldownSeconds())) {
            audit(traceId, liveSessionId, userId, action, "skipped_cooldown", ruleType, pick.getReason());
            return;
        }

        if ("next_slot".equals(action)) {
            if (!acquireJumpBudget(liveSessionId, cfg.getMaxAutoJumpsPerHour())) {
                audit(traceId, liveSessionId, userId, action, "skipped_hourly_cap", ruleType, pick.getReason());
                return;
            }
            LiveSessionScriptSlotVO vo = liveRealtimePanelService.nextSlot(liveSessionId, userId);
            liveRealtimeSseHub.broadcastSlotChange(liveSessionId, vo);
            audit(traceId, liveSessionId, userId, action, "executed_next_slot", ruleType, pick.getReason());
            return;
        }

        if ("inject_interaction".equals(action)) {
            String hint = pick.getSuggestion() != null ? pick.getSuggestion() : pick.getReason();
            liveRealtimeSseHub.broadcastInteractionHint(liveSessionId, hint, pick.getReason());
            audit(traceId, liveSessionId, userId, action, "executed_inject_hint", ruleType, pick.getReason());
        }
    }

    private void audit(String traceId, Long sessionId, Long userId, String action, String outcome,
                       String ruleType, String reason) {
        log.info("AUDIT realtime_suggest_auto traceId={} sessionId={} userId={} action={} outcome={} ruleType={} reason={}",
                traceId, sessionId, userId, action, outcome, ruleType, reason == null ? "" : reason.replace('\n', ' '));
    }

    private boolean acquireCooldown(Long sessionId, int cooldownSec) {
        if (cooldownSec <= 0) {
            return true;
        }
        if (stringRedisTemplate == null) {
            return true;
        }
        String key = KEY_COOLDOWN + sessionId;
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(cooldownSec));
        return Boolean.TRUE.equals(ok);
    }

    private boolean acquireJumpBudget(Long sessionId, int maxPerHour) {
        if (maxPerHour <= 0) {
            return false;
        }
        if (stringRedisTemplate == null) {
            return true;
        }
        String hour = ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ofPattern("yyyyMMddHH"));
        String key = KEY_JUMP_HOUR + sessionId + ":" + hour;
        Long n = stringRedisTemplate.opsForValue().increment(key);
        if (n != null && n == 1L) {
            stringRedisTemplate.expire(key, Duration.ofHours(2));
        }
        return n != null && n <= maxPerHour;
    }
}
