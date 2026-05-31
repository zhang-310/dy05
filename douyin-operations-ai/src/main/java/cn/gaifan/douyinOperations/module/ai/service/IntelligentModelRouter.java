package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.module.ai.domain.QualityLevel;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 内容感知智能路由视频生成服务
 *
 * 路由策略 (优先级从高到低):
 * 1. 内容感知路由: 分析场景描述 → 提取内容类型 → 选择最优模型
 * 2. 质量级别降级链 (fallback): 按固定优先级尝试
 *
 * Phase 1 仅 Kling，附录 A.2: CinematicKnowledgeService 可选 (Phase 5 实现)
 */
@Service
public class IntelligentModelRouter {

    private static final Logger log = LoggerFactory.getLogger(IntelligentModelRouter.class);
    private static final String CACHE_KEY_PREFIX = "video:gen:";
    private static final long CACHE_TTL_DAYS = 7;

    @Resource
    private List<AiVideoProvider> providers;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Value("${app.video-analysis.video-cache.enabled:true}")
    private boolean cacheEnabled;

    /** 内容类型关键词映射 */
    private static final Map<String, List<String>> CONTENT_TYPE_KEYWORDS = Map.of(
        "portrait",  List.of("特写", "对话", "表情", "人脸", "人物", "说话", "微笑", "close-up", "face"),
        "panorama",  List.of("全景", "远景", "风景", "城市", "建筑", "鸟瞰", "panorama", "landscape"),
        "action",    List.of("打斗", "追逐", "奔跑", "舞蹈", "动作", "跳跃", "fight", "chase", "dance"),
        "vfx",       List.of("特效", "魔法", "爆炸", "粒子", "光效", "变形", "VFX", "magic", "explosion"),
        "realistic", List.of("写实", "真实", "纪录", "实拍", "photorealistic", "documentary"),
        "anime",     List.of("动漫", "卡通", "二次元", "漫画", "anime", "cartoon", "illustration")
    );

    /** 内容类型 → 最佳模型优先级 */
    private static final Map<String, List<String>> CONTENT_MODEL_PRIORITY = Map.of(
        "portrait",  List.of("kling3", "kling", "seedance2", "minimax", "veo"),
        "panorama",  List.of("seedance2", "veo", "kling3", "kling", "runway"),
        "action",    List.of("kling3", "kling", "minimax", "runway", "seedance2"),
        "vfx",       List.of("runway", "luma", "seedance2", "kling", "minimax"),
        "realistic", List.of("veo", "seedance2", "kling3", "kling", "minimax"),
        "anime",     List.of("pika", "wan", "minimax", "runway"),
        "default",   List.of("kling3", "kling", "seedance2", "minimax", "veo", "runway", "luma", "wan", "pika")
    );

    /**
     * 智能路由视频生成
     * Phase 4.2: Redis 缓存 - 相同输入命中缓存时秒返回，减少重复 API 调用
     */
    public AiVideoProvider.VideoGenerationResult generateWithSmartRouting(
            AiVideoProvider.VideoGenerationRequest request,
            QualityLevel quality,
            String sceneDescription
    ) {
        // 0. 缓存查询 (Phase 4.2)
        if (cacheEnabled && stringRedisTemplate != null) {
            String cacheKey = buildCacheKey(request, quality);
            if (StringUtils.hasText(cacheKey)) {
                try {
                    String cached = stringRedisTemplate.opsForValue().get(cacheKey);
                    if (StringUtils.hasText(cached)) {
                        log.info("视频生成缓存命中: key={}", cacheKey);
                        return new AiVideoProvider.VideoGenerationResult(cached, "cache", 0, true, false);
                    }
                } catch (Exception e) {
                    log.debug("视频缓存读取失败: {}", e.getMessage());
                }
            }
        }

        // 1. 分析内容类型
        String contentType = analyzeContentType(sceneDescription);
        log.info("场景内容分析: contentType={}, scene={}", contentType,
                sceneDescription != null ? sceneDescription.substring(0, Math.min(50, sceneDescription.length())) : "null");

        // 2. 构建智能路由链
        List<AiVideoProvider> chain = buildSmartChain(contentType, quality, request.aspectRatio());

        if (chain.isEmpty()) {
            throw new cn.gaifan.douyinOperations.module.ai.exception.VideoGenerationException("router", "无可用的 AI 视频生成模型，请检查配置");
        }

        // 3. 逐个尝试
        List<String> errors = new ArrayList<>();
        for (AiVideoProvider provider : chain) {
            try {
                log.info("智能路由选择 {} (contentType={}, quality={})",
                        provider.name(), contentType, quality.getCode());

                AiVideoProvider.VideoGenerationResult result = provider.generateVideo(request);

                log.info("{} 生成成功", provider.name());

                // 缓存写入 (Phase 4.2)
                if (cacheEnabled && stringRedisTemplate != null && !"cache".equals(result.provider())
                        && StringUtils.hasText(result.videoUrl())) {
                    String cacheKey = buildCacheKey(request, quality);
                    if (StringUtils.hasText(cacheKey)) {
                        try {
                            stringRedisTemplate.opsForValue().set(cacheKey, result.videoUrl(),
                                    CACHE_TTL_DAYS, TimeUnit.DAYS);
                        } catch (Exception e) {
                            log.debug("视频缓存写入失败: {}", e.getMessage());
                        }
                    }
                }
                return result;
            } catch (Exception e) {
                errors.add(provider.name() + ": " + e.getMessage());
                log.warn("{} 生成失败, 尝试下一个: {}", provider.name(), e.getMessage());
            }
        }

        throw new cn.gaifan.douyinOperations.module.ai.exception.VideoGenerationException("router", "所有 AI 模型均失败: " + String.join(" → ", errors));
    }

    /** 供 AudioVideoJointService 等获取已配置的 Provider 列表 */
    public List<AiVideoProvider> getProviders() {
        return providers;
    }

    /**
     * 兼容旧版: 带降级的视频生成
     */
    public AiVideoProvider.VideoGenerationResult generateWithFallback(
            AiVideoProvider.VideoGenerationRequest request,
            QualityLevel quality
    ) {
        return generateWithSmartRouting(request, quality, request.prompt());
    }

    private String analyzeContentType(String sceneDescription) {
        if (sceneDescription == null) return "default";
        String desc = sceneDescription.toLowerCase();
        int maxScore = 0;
        String bestType = "default";
        for (Map.Entry<String, List<String>> entry : CONTENT_TYPE_KEYWORDS.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (desc.contains(keyword.toLowerCase())) score++;
            }
            if (score > maxScore) {
                maxScore = score;
                bestType = entry.getKey();
            }
        }
        return bestType;
    }

    private List<AiVideoProvider> buildSmartChain(String contentType, QualityLevel quality, String aspectRatio) {
        List<String> priorityOrder = CONTENT_MODEL_PRIORITY.getOrDefault(contentType,
                CONTENT_MODEL_PRIORITY.get("default"));

        List<AiVideoProvider> chain = new ArrayList<>();
        for (String name : priorityOrder) {
            for (AiVideoProvider p : providers) {
                if (p.name().equalsIgnoreCase(name) && p.isConfigured()
                        && p.supportsAspectRatio(aspectRatio)) {
                    chain.add(p);
                }
            }
        }

        // 补充 fallback: 添加不在优先列表中但已配置的提供者
        for (AiVideoProvider p : providers) {
            if (p.isConfigured() && !chain.contains(p) && p.supportsAspectRatio(aspectRatio)) {
                chain.add(p);
            }
        }

        return chain;
    }

    private String buildCacheKey(AiVideoProvider.VideoGenerationRequest request, QualityLevel quality) {
        if (!StringUtils.hasText(request.imageUrl())) return null;
        String s = request.imageUrl() + "|" + (request.endFrameUrl() != null ? request.endFrameUrl() : "")
                + "|" + (request.prompt() != null ? request.prompt() : "")
                + "|" + quality.getCode() + "|" + (request.aspectRatio() != null ? request.aspectRatio() : "");
        try {
            byte[] hash = MessageDigest.getInstance("MD5").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return CACHE_KEY_PREFIX + sb;
        } catch (Exception e) {
            return null;
        }
    }
}
