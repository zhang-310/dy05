package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.util.RequestRoleResolver;
import cn.gaifan.douyinOperations.module.live.entity.LiveSessionScriptSlot;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionScriptSlotRepository;
import cn.gaifan.douyinOperations.module.live.service.LivePanelTimerRedisService;
import cn.gaifan.douyinOperations.module.live.vo.LivePanelTimerSaveVO;
import cn.gaifan.douyinOperations.module.live.vo.LivePanelTimerStateVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.util.Optional;

/**
 * 倒计时效据存 Redis：刷新页面后可恢复同一场次、同一槽位下的运行/暂停状态。
 */
@Slf4j
@Service
public class LivePanelTimerRedisServiceImpl implements LivePanelTimerRedisService {

    private static final String KEY_PREFIX = "live:realtime-panel:timer:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private LiveSessionRepository liveSessionRepository;

    @Resource
    private LiveSessionScriptSlotRepository slotRepository;

    @Resource
    private ObjectMapper objectMapper;

    @Value("${app.live.realtime-panel.timer.enabled:true}")
    private boolean timerPersistenceEnabled;

    @Value("${app.live.realtime-panel.timer.ttl-hours:168}")
    private long ttlHours;

    @Override
    public void save(Long userId, LivePanelTimerSaveVO vo) {
        if (!timerPersistenceEnabled) {
            return;
        }
        validateSession(userId, vo.getLiveSessionId());
        validateSlot(userId, vo);

        if (Boolean.TRUE.equals(vo.getRunning())) {
            if (vo.getDeadlineEpochMs() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "运行中时必须提供 deadlineEpochMs");
            }
        }

        LivePanelTimerStateVO state = LivePanelTimerStateVO.builder()
                .liveSessionId(vo.getLiveSessionId())
                .slotId(vo.getSlotId())
                .slotIndex(vo.getSlotIndex())
                .durationSeconds(vo.getDurationSeconds())
                .running(vo.getRunning())
                .deadlineEpochMs(vo.getDeadlineEpochMs())
                .pausedRemainingSeconds(vo.getPausedRemainingSeconds())
                .build();

        String key = buildKey(userId, vo.getLiveSessionId());
        try {
            String json = objectMapper.writeValueAsString(state);
            stringRedisTemplate.opsForValue().set(key, json, Duration.ofHours(Math.max(1, ttlHours)));
        } catch (JsonProcessingException e) {
            log.warn("倒计时状态序列化失败: sessionId={}", vo.getLiveSessionId(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存倒计时状态失败");
        }
    }

    @Override
    public Optional<LivePanelTimerStateVO> load(Long userId, Long liveSessionId) {
        if (!timerPersistenceEnabled) {
            return Optional.empty();
        }
        validateSession(userId, liveSessionId);
        String key = buildKey(userId, liveSessionId);
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            LivePanelTimerStateVO state = objectMapper.readValue(json, LivePanelTimerStateVO.class);
            if (state.getLiveSessionId() == null || !state.getLiveSessionId().equals(liveSessionId)) {
                return Optional.empty();
            }
            return Optional.of(state);
        } catch (JsonProcessingException e) {
            log.warn("倒计时状态反序列化失败: sessionId={}", liveSessionId, e);
            return Optional.empty();
        }
    }

    private void validateSession(Long userId, Long sessionId) {
        if (RequestRoleResolver.isAdmin()) {
            liveSessionRepository.findByIdAndDeleted(sessionId, 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.LIVE_ACCESS_DENIED, "直播场次不存在"));
        } else {
            liveSessionRepository.findByIdAndUserIdAndDeleted(sessionId, userId, 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.LIVE_ACCESS_DENIED, "无权限访问该直播场次"));
        }
    }

    private void validateSlot(Long userId, LivePanelTimerSaveVO vo) {
        LiveSessionScriptSlot slot = slotRepository.findById(vo.getSlotId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术段落不存在"));
        if (!slot.getLiveSessionId().equals(vo.getLiveSessionId())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "话术段落与场次不匹配");
        }
        if (!RequestRoleResolver.isAdmin() && (slot.getOwnerId() == null || !slot.getOwnerId().equals(userId))) {
            throw new BusinessException(ErrorCode.LIVE_ACCESS_DENIED, "无权限操作该话术段落");
        }
        if (!slot.getSlotIndex().equals(vo.getSlotIndex())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "段落序号与记录不一致");
        }
    }

    private static String buildKey(Long userId, Long sessionId) {
        return KEY_PREFIX + userId + ":" + sessionId;
    }
}
