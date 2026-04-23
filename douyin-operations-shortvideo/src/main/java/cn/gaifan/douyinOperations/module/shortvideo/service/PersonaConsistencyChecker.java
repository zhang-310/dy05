package cn.gaifan.douyinOperations.module.shortvideo.service;

import java.util.Map;

public interface PersonaConsistencyChecker {
    Map<String, Object> checkConsistency(Long personaId, String generatedContent, Long userId);
}
