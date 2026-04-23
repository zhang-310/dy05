package cn.gaifan.douyinOperations.module.script.service.impl;

import cn.gaifan.douyinOperations.module.script.service.AiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class DeepSeekAiServiceImpl implements AiService {
    private static final Logger logger = LoggerFactory.getLogger(DeepSeekAiServiceImpl.class);

    @Value("${ai.deepseek.api-key:}")
    private String apiKey;

    @Value("${ai.deepseek.api-url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${ai.deepseek.model:deepseek-chat}")
    private String model;

    @Value("${ai.deepseek.enabled:false}")
    private Boolean enabled;

    private final RestTemplate restTemplate;

    public DeepSeekAiServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String generateScript(String prompt) {
        if (!enabled || apiKey == null || apiKey.isEmpty()) {
            logger.warn("DeepSeek AI 未启用或 API Key 未配置，使用模拟数据");
            return generateMockScript(prompt);
        }

        try {
            Map<String, Object> request = buildRequest(prompt);
            Map<String, Object> response = restTemplate.postForObject(apiUrl, request, Map.class);
            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
        } catch (Exception e) {
            logger.error("调用 DeepSeek API 失败: {}", e.getMessage());
        }

        return generateMockScript(prompt);
    }

    @Override
    public List<String> generateScriptBatch(List<String> prompts) {
        List<String> results = new ArrayList<>();
        for (String prompt : prompts) {
            results.add(generateScript(prompt));
        }
        return results;
    }

    @Override
    public String optimizeScript(String content, String style) {
        String optimizePrompt = String.format(
            "请优化以下直播话术，风格为 %s:\n\n%s\n\n要求:\n1. 保持原意\n2. 增强 %s 风格\n3. 控制在 200-300 字",
            style, content, style
        );
        return generateScript(optimizePrompt);
    }

    @Override
    public Double scoreScript(String content) {
        String scorePrompt = String.format(
            "请评分以下直播话术 (1-10 分)，只返回数字:\n\n%s",
            content
        );
        
        try {
            String response = generateScript(scorePrompt);
            Double score = Double.parseDouble(response.trim());
            return Math.min(Math.max(score, 1.0), 10.0);
        } catch (Exception e) {
            logger.warn("评分失败，使用默认评分");
            return 8.5;
        }
    }

    private Map<String, Object> buildRequest(String prompt) {
        Map<String, Object> request = new HashMap<>();
        request.put("model", model);
        request.put("temperature", 0.7);
        request.put("max_tokens", 1000);

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);

        request.put("messages", messages);
        return request;
    }

    private String generateMockScript(String prompt) {
        // 模拟数据，用于开发和测试
        String[] templates = {
            "亲爱的各位观众，欢迎来到我们的直播间！今天为大家介绍一款超级棒的产品——它采用天然成分，获得欧盟认证，适合各种肤质。现在下单享受限时优惠，数量有限，先到先得！",
            "大家好！我是您的主播。今天给大家带来一个必买的好东西。这款产品经过严格测试，用户满意度高达98%。现在购买还有额外赠品，机会难得，不要错过！",
            "各位朋友，感谢大家的陪伴。今天的重磅推荐来了！这是一款集品质、效果、价格于一身的产品。限时特价进行中，今天下单立享优惠。心动不如行动，赶快加入购物车吧！"
        };
        
        int index = prompt.hashCode() % templates.length;
        return templates[Math.abs(index)];
    }
}
