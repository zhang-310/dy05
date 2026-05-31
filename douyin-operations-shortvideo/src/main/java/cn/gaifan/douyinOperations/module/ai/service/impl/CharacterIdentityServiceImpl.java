package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.service.CharacterIdentityService;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDramaCharacter;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvDramaCharacterRepository;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色身份管理服务实现 (Phase 8)
 * 多参考图 + 加权 Prompt，LoRA 训练为占位
 */
@Service
public class CharacterIdentityServiceImpl implements CharacterIdentityService {

    private static final Logger log = LoggerFactory.getLogger(CharacterIdentityServiceImpl.class);

    @Resource
    private SvDramaCharacterRepository characterRepository;

    @Override
    public String buildWeightedPrompt(Long characterId, String basePrompt) {
        if (characterId == null) return basePrompt;
        SvDramaCharacter c = characterRepository.findById(characterId).orElse(null);
        if (c == null) return basePrompt;
        String tags = c.getPromptTags();
        if (!StringUtils.hasText(tags)) return basePrompt;
        String[] arr = tags.split("[,，]");
        StringBuilder sb = new StringBuilder(basePrompt);
        for (String t : arr) {
            String tag = t.trim();
            if (!tag.isEmpty()) sb.append(", ").append(tag).append(":1.2");
        }
        return sb.toString();
    }

    @Override
    public List<String> getReferenceImageUrls(Long characterId) {
        List<String> urls = new ArrayList<>();
        if (characterId == null) return urls;
        SvDramaCharacter c = characterRepository.findById(characterId).orElse(null);
        if (c == null) return urls;
        if (StringUtils.hasText(c.getReferenceImageUrl())) {
            urls.add(c.getReferenceImageUrl());
        }
        String refs = c.getReferenceImages();
        if (StringUtils.hasText(refs)) {
            try {
                JSONArray arr = JSON.parseArray(refs);
                for (int i = 0; i < arr.size(); i++) {
                    var obj = arr.getJSONObject(i);
                    if (obj != null && obj.containsKey("url")) {
                        urls.add(obj.getString("url"));
                    }
                }
            } catch (Exception e) {
                log.debug("JSON解析referenceImages失败: {}", e.getMessage());
            }
        }
        return urls;
    }

    @Override
    public String getLoraStatus(Long characterId) {
        if (characterId == null) return "pending";
        SvDramaCharacter c = characterRepository.findById(characterId).orElse(null);
        if (c == null) return "pending";
        return StringUtils.hasText(c.getLoraModelPath()) ? "ready" : "pending";
    }

    @Override
    public String submitLoraTraining(Long characterId, List<String> referenceImageUrls) {
        if (characterId == null || referenceImageUrls == null || referenceImageUrls.isEmpty()) {
            return null;
        }
        return "task_" + characterId;
    }
}
