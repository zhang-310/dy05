package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.service.AiPromptConfigService;
import cn.gaifan.douyinOperations.module.config.service.ConfigService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI Prompt 配置服务实现：从 sys_config 读取，支持 {{varName}} 占位符替换
 */
@Service
public class AiPromptConfigServiceImpl implements AiPromptConfigService {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([^}]+)}}");

    @Resource
    private ConfigService configService;

    @Override
    public String getPrompt(String configKey, String defaultPrompt) {
        return getPrompt(configKey, null, defaultPrompt);
    }

    @Override
    public String getPrompt(String configKey, Map<String, Object> vars, String defaultPrompt) {
        String raw = configService.getRawValueByKey(configKey);
        String base = (raw != null && !raw.isBlank()) ? raw : defaultPrompt;
        if (base == null) return "";
        if (vars == null || vars.isEmpty()) return base;
        Matcher m = PLACEHOLDER.matcher(base);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1).trim();
            Object val = vars.get(key);
            m.appendReplacement(sb, Matcher.quoteReplacement(val != null ? val.toString() : ""));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
