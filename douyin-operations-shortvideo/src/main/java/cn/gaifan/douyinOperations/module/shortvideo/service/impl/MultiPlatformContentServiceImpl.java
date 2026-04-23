package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.shortvideo.service.MultiPlatformContentService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MultiPlatformContentServiceImpl implements MultiPlatformContentService {

    private static final Logger log = LoggerFactory.getLogger(MultiPlatformContentServiceImpl.class);
    private static final Map<String, String> PLATFORM_RULES = Map.of(
            "douyin", "抖音：竖屏9:16，快节奏，3秒钩子，热门标签，时长15-60秒",
            "kuaishou", "快手：接地气风格，注重真实感，老铁文化，时长15-60秒",
            "xiaohongshu", "小红书：精致美学，种草风格，干货分享，图文结合，标题用emoji",
            "bilibili", "B站：深度内容，弹幕互动文化，时长可较长，专业科普向"
    );

    @Resource private LlmClient llmClient;
    @Resource private AiModelRepository modelRepository;

    @Override
    public Map<String, String> generateMultiPlatformVersions(Long userId, String originalScript, List<String> platforms) {
        Map<String, String> results = new LinkedHashMap<>();
        List<AiModel> models = modelRepository.findByStatusAndDeleted(1, 0).stream().limit(3).toList();
        if (models.isEmpty()) {
            platforms.forEach(p -> results.put(p, "无可用模型"));
            return results;
        }
        for (String platform : platforms) {
            String rule = PLATFORM_RULES.getOrDefault(platform, "通用平台");
            String prompt = String.format("""
                    将以下脚本改编为适合「%s」平台的版本。
                    
                    平台规则：%s
                    
                    原始脚本：
                    %s
                    
                    请输出改编后的完整脚本，保留核心卖点，适配平台风格。
                    """, platform, rule, originalScript);
            LlmClient.LlmResponse resp = llmClient.chatWithFallback(models, "你是全平台内容运营专家", prompt);
            results.put(platform, resp.success() ? resp.content() : "生成失败: " + resp.errorMsg());
        }
        return results;
    }
}
