package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.service.AiMusicProvider;
import cn.gaifan.douyinOperations.module.ai.service.AiMusicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 音乐生成服务实现 (Phase 7)
 * 通过已配置 Provider 生成音乐；未配置时明确失败，不返回示例音频。
 */
@Service
public class AiMusicServiceImpl implements AiMusicService {

    private static final Logger log = LoggerFactory.getLogger(AiMusicServiceImpl.class);

    @Autowired(required = false)
    private List<AiMusicProvider> providers;

    @Override
    public AiMusicProvider.MusicGenerationResult generateBgm(
            String styleDescription,
            int durationSec,
            boolean instrumental) {
        AiMusicProvider p = getFirstAvailable();
        if (p == null) {
            throw new BusinessException(ErrorCode.AI_TASK_MODEL_NOT_CONFIGURED,
                    "BGM 生成暂未配置。请配置 SUNO_API_KEY 或 UDIO_API_KEY 后使用 AI 生成 BGM。");
        }
        AiMusicProvider.MusicGenerationRequest req = new AiMusicProvider.MusicGenerationRequest(
                styleDescription != null ? styleDescription : "cinematic background music",
                120,
                durationSec > 0 ? durationSec : 30,
                instrumental,
                null);
        return p.generateMusic(req);
    }

    @Override
    public List<String> getAvailableProviders() {
        if (providers == null) return List.of();
        return providers.stream()
                .filter(AiMusicProvider::isConfigured)
                .map(AiMusicProvider::name)
                .collect(Collectors.toList());
    }

    private AiMusicProvider getFirstAvailable() {
        if (providers == null) return null;
        for (AiMusicProvider p : providers) {
            if (p.isConfigured()) {
                return p;
            }
        }
        return null;
    }
}
