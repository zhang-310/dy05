package cn.gaifan.douyinOperations.module.ai.service.impl.brain;

import cn.gaifan.douyinOperations.module.ai.entity.AiHostPersona;
import cn.gaifan.douyinOperations.module.ai.repository.AiHostPersonaRepository;
import cn.gaifan.douyinOperations.module.ai.service.brain.HostPersonaService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 五位主播人设服务实现
 */
@Service
public class HostPersonaServiceImpl implements HostPersonaService {

    private static final Logger log = LoggerFactory.getLogger(HostPersonaServiceImpl.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    @Value("${app.ai.brain.host-persona.enabled:true}")
    private boolean enabled;

    @Autowired(required = false)
    private AiHostPersonaRepository repository;

    @Override
    public List<AiHostPersona> listAll() {
        if (!enabled || repository == null) return List.of();
        return repository.findByStatusAndDeletedOrderBySortOrderAsc(1, 0);
    }

    @Override
    public AiHostPersona getByCode(String hostCode) {
        if (!enabled || repository == null || hostCode == null) return null;
        return repository.findByHostCodeAndDeleted(hostCode, 0).orElse(null);
    }

    @Override
    public AiHostPersona getById(Long id) {
        if (!enabled || repository == null || id == null) return null;
        return repository.findById(id).orElse(null);
    }

    @Override
    public Map<String, Double> getBayesFactors(String hostCode) {
        AiHostPersona p = getByCode(hostCode);
        if (p == null || p.getBayesFactors() == null || p.getBayesFactors().isBlank())
            return Map.of();
        try {
            return JSON.readValue(p.getBayesFactors(), new TypeReference<Map<String, Double>>() {});
        } catch (Exception e) {
            log.debug("[HostPersona] parse bayes_factors failed: {}", e.getMessage());
            return Map.of();
        }
    }

    @Override
    public Map<String, String> getStyleVector(String hostCode) {
        AiHostPersona p = getByCode(hostCode);
        if (p == null || p.getStyleVector() == null || p.getStyleVector().isBlank())
            return Map.of();
        try {
            Map<String, ?> raw = JSON.readValue(p.getStyleVector(), Map.class);
            Map<String, String> result = new HashMap<>();
            raw.forEach((k, v) -> result.put(k, v != null ? v.toString() : ""));
            return result;
        } catch (Exception e) {
            log.debug("[HostPersona] parse style_vector failed: {}", e.getMessage());
            return Map.of();
        }
    }

    @Override
    public List<String> getAiPriorities(String hostCode) {
        AiHostPersona p = getByCode(hostCode);
        if (p == null || p.getAiPriorities() == null || p.getAiPriorities().isBlank())
            return List.of();
        try {
            return JSON.readValue(p.getAiPriorities(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.debug("[HostPersona] parse ai_priorities failed: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<AiHostPersona> getFlowPath() {
        List<AiHostPersona> all = listAll();
        all.sort(Comparator.comparingInt(p -> p.getFlowPhase() != null ? p.getFlowPhase() : 0));
        return all;
    }

    @Override
    public boolean isAvailable() {
        return enabled && repository != null;
    }
}
