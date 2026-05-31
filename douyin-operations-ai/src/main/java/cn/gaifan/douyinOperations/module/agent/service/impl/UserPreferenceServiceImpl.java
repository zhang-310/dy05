package cn.gaifan.douyinOperations.module.agent.service.impl;

import cn.gaifan.douyinOperations.module.agent.entity.AgentUserPreference;
import cn.gaifan.douyinOperations.module.agent.repository.AgentUserPreferenceRepository;
import cn.gaifan.douyinOperations.module.agent.service.UserPreferenceService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserPreferenceServiceImpl implements UserPreferenceService {

    @Resource
    private AgentUserPreferenceRepository repository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void record(Long userId, String key, String value) {
        if (userId == null || key == null || value == null || key.isBlank() || value.isBlank()) return;
        repository.findByUserIdAndPrefKeyAndPrefValueAndDeleted(userId, key, value, 0)
                .ifPresentOrElse(
                        p -> {
                            p.setUsageCount(p.getUsageCount() + 1);
                            p.setLastUsedAt(new Timestamp(System.currentTimeMillis()));
                            repository.save(p);
                        },
                        () -> {
                            AgentUserPreference p = new AgentUserPreference();
                            p.setUserId(userId);
                            p.setPrefKey(key);
                            p.setPrefValue(value);
                            p.setUsageCount(1);
                            p.setLastUsedAt(new Timestamp(System.currentTimeMillis()));
                            repository.save(p);
                        }
                );
    }

    @Override
    public List<String> getTopPreferences(Long userId, String key, int limit) {
        if (userId == null || key == null || limit <= 0) return List.of();
        return repository.findTopByUserIdAndKey(userId, key, PageRequest.of(0, limit))
                .stream()
                .map(AgentUserPreference::getPrefValue)
                .collect(Collectors.toList());
    }
}
