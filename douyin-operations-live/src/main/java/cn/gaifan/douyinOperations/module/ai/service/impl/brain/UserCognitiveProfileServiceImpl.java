package cn.gaifan.douyinOperations.module.ai.service.impl.brain;

import cn.gaifan.douyinOperations.module.ai.entity.AiUserCognitiveProfile;
import cn.gaifan.douyinOperations.module.ai.repository.AiUserCognitiveProfileRepository;
import cn.gaifan.douyinOperations.module.ai.service.brain.HostPersonaService;
import cn.gaifan.douyinOperations.module.ai.service.brain.UserCognitiveProfileService;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptEffectivenessRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户认知画像服务实现（Phase1/Phase5）
 * 基于 ai_user_cognitive_profile、live_script、live_script_effectiveness 等真实数据聚合
 */
@Service
public class UserCognitiveProfileServiceImpl implements UserCognitiveProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserCognitiveProfileServiceImpl.class);

    @Value("${app.ai.brain.user-profile.enabled:true}")
    private boolean enabled;

    @Autowired(required = false)
    private cn.gaifan.douyinOperations.module.ai.service.ContentEffectivenessService contentEffectivenessService;

    @Autowired(required = false)
    private HostPersonaService hostPersonaService;

    @Resource
    private AiUserCognitiveProfileRepository profileRepository;

    @Autowired(required = false)
    private LiveScriptRepository liveScriptRepository;

    @Autowired(required = false)
    private LiveScriptEffectivenessRepository effectivenessRepository;

    private final Map<Long, UserProfile> profileCache = new HashMap<>();

    @Override
    public UserProfile getProfile(Long userId) {
        if (!enabled || userId == null) {
            return emptyProfile(userId);
        }
        UserProfile cached = profileCache.get(userId);
        if (cached != null) return cached;
        UserProfile p = buildProfile(userId);
        profileCache.put(userId, p);
        return p;
    }

    @Override
    @Transactional
    public void updateFromFeedback(Long userId, String actionType, Map<String, Object> feedback) {
        if (!enabled || userId == null) return;
        profileCache.remove(userId);
        try {
            AiUserCognitiveProfile profile = profileRepository.findByOwnerIdAndDeleted(userId, 0).orElse(null);
            if (profile != null && "script_generated".equals(actionType)) {
                profile.setLastUpdated(LocalDateTime.now());
                profileRepository.save(profile);
            }
        } catch (Exception e) {
            log.warn("[UserProfile] updateFromFeedback failed: {}", e.getMessage());
        }
        log.debug("[UserProfile] updated from feedback userId={} action={}", userId, actionType);
    }

    @Override
    public Map<String, Double> getRecommendationWeights(Long userId) {
        if (!enabled) return Collections.emptyMap();
        UserProfile p = getProfile(userId);
        if (p == null || p.contentPreferences() == null) return Collections.emptyMap();
        return new HashMap<>(p.contentPreferences());
    }

    @Override
    public Map<String, Double> getRecommendationWeightsForHost(Long userId, String hostCode) {
        Map<String, Double> base = getRecommendationWeights(userId);
        if (hostCode == null || hostPersonaService == null) return base;
        List<String> hostPriorities = hostPersonaService.getAiPriorities(hostCode);
        if (hostPriorities.isEmpty()) return base;
        Map<String, Double> merged = new HashMap<>(base);
        double boost = 1.0;
        for (String priority : hostPriorities) {
            merged.merge(priority, 0.85 * boost, Double::sum);
            boost *= 0.95;
        }
        return merged;
    }

    @Override
    public boolean isAvailable() {
        return enabled;
    }

    private UserProfile emptyProfile(Long userId) {
        return new UserProfile(userId, Map.of(), List.of(), 0.0, Map.of(), System.currentTimeMillis());
    }

    private UserProfile buildProfile(Long userId) {
        AiUserCognitiveProfile dbProfile = profileRepository.findByOwnerIdAndDeleted(userId, 0).orElse(null);
        boolean needsUpdate = dbProfile == null || (dbProfile.getLastUpdated() != null
                && dbProfile.getLastUpdated().isBefore(LocalDateTime.now().minusDays(7)));

        Map<String, Double> prefs;
        List<String> styles = List.of("口语化", "热情");
        double learningProgress = 0.5;
        Map<String, Object> extra = new HashMap<>();

        if (dbProfile != null && !needsUpdate && dbProfile.getPreferredScriptTypes() != null && !dbProfile.getPreferredScriptTypes().isEmpty()) {
            prefs = new HashMap<>(dbProfile.getPreferredScriptTypes());
            if (dbProfile.getPreferredStyles() != null) {
                styles = dbProfile.getPreferredStyles().entrySet().stream()
                        .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                        .limit(5).map(Map.Entry::getKey).collect(Collectors.toList());
            }
            learningProgress = dbProfile.getAvgEditRatio() != null ? dbProfile.getAvgEditRatio() : 0.5;
            extra.put("isDefault", false);
        } else {
            prefs = aggregateFromData(userId);
            extra.put("isDefault", prefs.isEmpty());
            if (prefs.isEmpty()) {
                prefs.put("种草", 0.8);
                prefs.put("促销", 0.6);
                prefs.put("产品介绍", 0.7);
            }
            if (profileRepository != null) {
                try {
                    saveOrUpdateProfile(userId, prefs, dbProfile);
                } catch (Exception e) {
                    log.warn("Failed to save profile: {}", e.getMessage());
                }
            }
        }

        return new UserProfile(userId, prefs, styles, learningProgress, extra, System.currentTimeMillis());
    }

    private Map<String, Double> aggregateFromData(Long userId) {
        Map<String, Double> prefs = new HashMap<>();
        if (liveScriptRepository == null) return prefs;
        try {
            List<Object[]> rows = liveScriptRepository.countScriptTypesByOwnerId(userId);
            if (rows.isEmpty()) return prefs;
            long total = rows.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();
            if (total == 0) return prefs;
            for (Object[] r : rows) {
                String type = (String) r[0];
                long cnt = ((Number) r[1]).longValue();
                prefs.put(type, (double) cnt / total);
            }
            if (effectivenessRepository != null) {
                Double avgConv = effectivenessRepository.findAvgConversionByUserId(userId);
                if (avgConv != null && avgConv > 0) {
                    prefs.put("_avgConversion", avgConv / 100.0);
                }
            }
        } catch (Exception e) {
            log.debug("aggregateFromData failed: {}", e.getMessage());
        }
        return prefs;
    }

    private void saveOrUpdateProfile(Long userId, Map<String, Double> prefs, AiUserCognitiveProfile existing) {
        AiUserCognitiveProfile p = existing != null ? existing : new AiUserCognitiveProfile();
        p.setOwnerId(userId);
        p.setPreferredScriptTypes(prefs);
        p.setLastUpdated(LocalDateTime.now());
        p.setDeleted(0);
        profileRepository.save(p);
    }
}
