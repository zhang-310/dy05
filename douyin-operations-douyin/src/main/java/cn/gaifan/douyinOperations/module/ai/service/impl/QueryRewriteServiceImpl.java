package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.entity.AiTaskModelConfig;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiTaskModelConfigRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.service.QueryRewriteService;
import cn.gaifan.douyinOperations.module.config.service.ConfigService;
import cn.gaifan.douyinOperations.module.douyin.entity.DouyinAccount;
import cn.gaifan.douyinOperations.module.douyin.repository.DouyinAccountRepository;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 查询改写：结合用户抖音画像将原始查询改写为 2-3 个更精准子查询。
 * 模型链路由 ai_task_model_config.query_rewrite 配置，支持主/备模型三层回退；
 * 缓存策略：Redis 1 小时 TTL，key=cache:kb:rewrite:{sha256(query:accountId)}。
 */
@Service
public class QueryRewriteServiceImpl implements QueryRewriteService {

    private static final Logger log = LoggerFactory.getLogger(QueryRewriteServiceImpl.class);
    private static final String CACHE_PREFIX = "cache:kb:rewrite:";
    private static final long CACHE_TTL_HOURS = 1;
    private static final int MAX_QUERY_LENGTH = 500;
    private static final int MAX_SUB_QUERY_LENGTH = 200;

    @Resource
    private DouyinAccountRepository accountRepository;

    @Resource
    private AiModelRepository modelRepository;

    @Resource
    private AiTaskModelConfigRepository taskModelConfigRepository;

    @Resource
    private LlmClient llmClient;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Autowired(required = false)
    private ConfigService configService;

    @Value("${app.ai.query-rewrite.enabled:true}")
    private boolean rewriteEnabled;

    @Value("${app.ai.query-rewrite.sub-queries:3}")
    private int maxSubQueries;

    @Override
    public List<String> rewrite(Long userId, String query) {
        if (userId == null || query == null || query.isBlank()) {
            return Collections.singletonList(query != null ? query : "");
        }
        String trimmed = query.trim();
        if (trimmed.length() > MAX_QUERY_LENGTH) {
            trimmed = trimmed.substring(0, MAX_QUERY_LENGTH);
        }
        if (!rewriteEnabled) return Collections.singletonList(trimmed);

        List<DouyinAccount> accounts = accountRepository.findByOwnerIdAndDeleted(userId, 0, PageRequest.of(0, 1)).getContent();
        if (accounts.isEmpty()) return Collections.singletonList(trimmed);

        DouyinAccount account = accounts.get(0);

        // 优先使用配置「ai.query-rewrite.profile_override」，覆盖抖音账号数据（进化/知识检索通用）
        String profileOverride = configOr("ai.query-rewrite.profile_override", null);
        String profile = (profileOverride != null && !profileOverride.isBlank())
                ? profileOverride
                : String.format("粉丝数 %d，账号描述：%s",
                        account.getFanCount() != null ? account.getFanCount() : 0,
                        account.getDescription() != null ? account.getDescription() : "无");

        String cacheKey = CACHE_PREFIX + sha256(trimmed + ":" + account.getId() + ":" + profile);

        if (stringRedisTemplate != null) {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null && !cached.isBlank()) {
                List<String> parsed = Arrays.stream(cached.split("\n")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
                if (!parsed.isEmpty()) return parsed;
            }
        }

        List<AiModel> models = resolveQueryRewriteModels();
        if (models.isEmpty()) return Collections.singletonList(trimmed);

        String prompt = String.format("用户画像：%s。请将用户查询「%s」改写为 %d 个更精准的检索子查询，每行一个，不要编号，直接输出查询内容。", profile, trimmed, maxSubQueries);
        try {
            LlmClient.LlmResponse resp = llmClient.chatWithFallback(models, "你是检索查询改写专家，输出简洁的子查询。", prompt);
            if (!resp.success() || resp.content() == null || resp.content().isBlank()) {
                return Collections.singletonList(trimmed);
            }
            List<String> subQueries = Arrays.stream(resp.content().split("\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && s.length() <= MAX_SUB_QUERY_LENGTH)
                    .limit(maxSubQueries)
                    .collect(Collectors.toList());
            if (subQueries.isEmpty()) return Collections.singletonList(trimmed);

            if (stringRedisTemplate != null) {
                stringRedisTemplate.opsForValue().set(cacheKey, String.join("\n", subQueries), CACHE_TTL_HOURS, TimeUnit.HOURS);
            }
            return subQueries;
        } catch (Exception e) {
            log.warn("查询改写失败，降级为原查询: {}", e.getMessage());
            return Collections.singletonList(trimmed);
        }
    }

    /** 从 ai_task_model_config.query_rewrite 解析模型链，无配置时回退到任意可用模型 */
    private List<AiModel> resolveQueryRewriteModels() {
        var config = taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted("query_rewrite", 1, 0);
        if (config.isPresent()) {
            AiTaskModelConfig tc = config.get();
            List<AiModel> result = new ArrayList<>();
            for (Long modelId : java.util.Arrays.asList(tc.getPrimaryModelId(), tc.getFallbackModelId(), tc.getFallback2ModelId())) {
                if (modelId == null) continue;
                modelRepository.findById(modelId).filter(m -> m.getStatus() == 1 && m.getDeleted() == 0)
                        .ifPresent(result::add);
            }
            if (!result.isEmpty()) return result;
        }
        return modelRepository.findByStatusAndDeleted(1, 0).stream().limit(3).toList();
    }

    private String configOr(String key, String fallback) {
        if (configService == null) return fallback;
        String v = configService.getRawValueByKey(key);
        return (v != null && !v.isBlank()) ? v.trim() : fallback;
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
