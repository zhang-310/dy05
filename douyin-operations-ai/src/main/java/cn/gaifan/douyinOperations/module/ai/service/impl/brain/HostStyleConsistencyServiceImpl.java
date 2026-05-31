package cn.gaifan.douyinOperations.module.ai.service.impl.brain;

import cn.gaifan.douyinOperations.module.ai.service.VectorService;
import cn.gaifan.douyinOperations.module.ai.service.brain.HostPersonaService;
import cn.gaifan.douyinOperations.module.ai.service.brain.HostStyleConsistencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 多模态风格一致性服务实现
 */
@Service
public class HostStyleConsistencyServiceImpl implements HostStyleConsistencyService {

    private static final Logger log = LoggerFactory.getLogger(HostStyleConsistencyServiceImpl.class);

    @Value("${app.ai.brain.style-consistency.enabled:true}")
    private boolean enabled;

    @Autowired(required = false)
    private HostPersonaService hostPersonaService;

    @Autowired(required = false)
    private VectorService vectorService;

    @Override
    public String getTextStylePrompt(String hostCode) {
        Map<String, String> vec = getStyleVector(hostCode);
        if (vec.isEmpty()) return "";
        return String.format("表达风格：%s；视觉意象：%s；节奏：%s。",
                vec.getOrDefault("tone", ""),
                vec.getOrDefault("visual", ""),
                vec.getOrDefault("pacing", ""));
    }

    @Override
    public Map<String, String> getStyleVector(String hostCode) {
        if (!enabled || hostPersonaService == null || hostCode == null) return Map.of();
        return hostPersonaService.getStyleVector(hostCode);
    }

    @Override
    public double getStyleConsistencyScore(String hostCode, String content) {
        Map<String, String> vec = getStyleVector(hostCode);
        if (vec.isEmpty() || content == null || content.isBlank()) return 1.0;
        if (vectorService != null) {
            try {
                String styleRef = String.join(" ", vec.getOrDefault("tone", ""),
                        vec.getOrDefault("visual", ""), vec.getOrDefault("pacing", ""));
                if (!styleRef.isBlank()) {
                    List<Float> refEmb = vectorService.generateEmbedding(styleRef);
                    List<Float> contentEmb = vectorService.generateEmbedding(content);
                    if (refEmb != null && contentEmb != null && refEmb.size() == contentEmb.size()) {
                        return cosineSimilarity(refEmb, contentEmb);
                    }
                }
            } catch (Exception e) {
                log.debug("[StyleConsistency] 向量相似度失败，回退关键词: {}", e.getMessage());
            }
        }
        return getStyleConsistencyScoreByKeyword(vec, content);
    }

    private double getStyleConsistencyScoreByKeyword(Map<String, String> vec, String content) {
        String tone = vec.get("tone");
        if (tone == null || tone.isBlank()) return 1.0;
        int match = 0;
        String lower = content.toLowerCase();
        for (String word : tone.split("[、,，]")) {
            if (lower.contains(word.trim().toLowerCase())) match++;
        }
        return match > 0 ? 0.5 + 0.5 * Math.min(1.0, match / 3.0) : 0.5;
    }

    private static double cosineSimilarity(List<Float> a, List<Float> b) {
        if (a == null || b == null || a.size() != b.size()) return 0.5;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.size(); i++) {
            float va = a.get(i) != null ? a.get(i) : 0;
            float vb = b.get(i) != null ? b.get(i) : 0;
            dot += va * vb;
            normA += va * va;
            normB += vb * vb;
        }
        if (normA <= 0 || normB <= 0) return 0.5;
        return Math.max(0, Math.min(1, dot / (Math.sqrt(normA) * Math.sqrt(normB))));
    }

    @Override
    public boolean isAvailable() {
        return enabled && hostPersonaService != null;
    }
}
