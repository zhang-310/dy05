package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.contract.ai.AiGenerationPort;
import cn.gaifan.douyinOperations.contract.ai.KbSearchPort;
import cn.gaifan.douyinOperations.contract.ai.VideoAnalysisPort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Contract SPI 实现 — AI 生成 Port
 *
 * Sprint 5: 核心流程优化，contract 端口收口
 */
@Service
public class AiGenerationPortImpl implements AiGenerationPort {

    @Override
    public String generateLiveScript(String productInfo, String audienceProfile, String style) {
        return "[AI 生成的直播话术] 产品: " + productInfo
                + ", 受众: " + audienceProfile + ", 风格: " + style;
    }

    @Override
    public String generateShortVideoCopy(String productName, String platform, String tone) {
        return "[AI 生成的短视频文案] " + productName + " (" + platform + ", " + tone + ")";
    }

    @Override
    public List<String> batchGenerateScripts(List<String> prompts) {
        return prompts.stream()
                .map(p -> "[AI 批量生成] " + p.substring(0, Math.min(p.length(), 50)))
                .toList();
    }
}

@Service
class KbSearchPortImpl implements KbSearchPort {
    @Override
    public String search(String query, String kbName) {
        return "[RAG 检索结果] 查询: " + query + " (知识库: " + kbName + ")";
    }
    @Override
    public List<String> findSimilar(String text, int limit) {
        return List.of("[相似文档 1] " + text.substring(0, Math.min(text.length(), 30)),
                       "[相似文档 2]");
    }
}

@Service
class VideoAnalysisPortImpl implements VideoAnalysisPort {
    @Override
    public VideoAnalysisResult analyze(String videoUrl) {
        return new VideoAnalysisResult(videoUrl, "开场介绍→产品展示→促单转化",
                "痛点+解决方案+信任背书", 0.75,
                List.of("前3秒抓住注意力", "第15秒展示产品", "结尾CTA明确"));
    }
    @Override
    public List<VideoAnalysisResult> batchAnalyze(List<String> videoUrls) {
        return videoUrls.stream().map(this::analyze).toList();
    }
}
