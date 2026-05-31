package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.service.EvolutionService;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.live.service.LiveAiModelResolver;
import cn.gaifan.douyinOperations.module.douyin.entity.DouyinVideo;
import cn.gaifan.douyinOperations.module.douyin.repository.DouyinVideoRepository;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralFavorite;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvViralFavoriteRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvViralVideoRepository;
import cn.gaifan.douyinOperations.contract.product.FeatureCode;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import cn.gaifan.douyinOperations.module.platform.credit.CommercialProductChargeService;
import cn.gaifan.douyinOperations.module.platform.product.DeliveryProduct;
import cn.gaifan.douyinOperations.module.shortvideo.integration.VideoInsightIntegrationBridge;
import cn.gaifan.douyinOperations.module.shortvideo.service.ViralVideoService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.ViralCollectVO;
import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class ViralVideoServiceImpl implements ViralVideoService {

    private static final Logger log = LoggerFactory.getLogger(ViralVideoServiceImpl.class);

    @Resource
    private SvViralVideoRepository viralVideoRepository;

    @Resource
    private SvViralFavoriteRepository viralFavoriteRepository;

    @Resource
    private DouyinVideoRepository douyinVideoRepository;

    @Resource
    private EvolutionService evolutionService;

    @Resource
    private LlmClient llmClient;

    @Resource
    private LiveAiModelResolver liveAiModelResolver;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    @Autowired(required = false)
    private VideoInsightIntegrationBridge videoInsightIntegrationBridge;

    @Autowired(required = false)
    private CommercialProductChargeService commercialProductChargeService;

    private static final String CACHE_KEY_PREFIX = "shortvideo:viral:recommended:";
    private static final long CACHE_TTL_MINUTES = 60;

    @Override
    public List<SvViralVideo> listViralVideos(Long userId, String mode, String category, String sortBy, Integer page, Integer rows) {
        int r = rows != null && rows > 0 ? Math.min(rows, 100) : 20;
        int p = page != null && page >= 0 ? page : 0;
        Sort sort = Sort.by(Sort.Direction.DESC, sortBy != null ? sortBy : "viralScore");
        PageRequest pageRequest = PageRequest.of(p, r, sort);

        if ("platform".equalsIgnoreCase(mode)) {
            return viralVideoRepository.findByOwnerIdAndDeleted(0L, 0, pageRequest).getContent();
        }
        if ("my".equalsIgnoreCase(mode)) {
            List<SvViralFavorite> favorites = viralFavoriteRepository.findByUserIdOrderByCreateTimeDesc(userId);
            List<Long> favoriteIds = favorites.stream().map(SvViralFavorite::getViralVideoId).toList();
            List<SvViralVideo> fromFavorites = favoriteIds.isEmpty() ? List.of()
                : viralVideoRepository.findByIdInAndDeleted(favoriteIds, 0);
            List<SvViralVideo> own = viralVideoRepository.findByOwnerIdAndDeleted(userId, 0, PageRequest.of(0, 500, sort)).getContent();
            java.util.Map<Long, SvViralVideo> merged = new java.util.LinkedHashMap<>();
            for (SvViralVideo v : fromFavorites) merged.putIfAbsent(v.getId(), v);
            for (SvViralVideo v : own) merged.putIfAbsent(v.getId(), v);
            List<SvViralVideo> all = new java.util.ArrayList<>(merged.values());
            all.sort((a, b) -> (b.getCreateTime() != null && a.getCreateTime() != null)
                ? b.getCreateTime().compareTo(a.getCreateTime()) : 0);
            int from = p * r;
            int to = Math.min(from + r, all.size());
            return from < all.size() ? all.subList(from, to) : List.of();
        }
        return viralVideoRepository.findByOwnerIdAndDeleted(userId, 0, pageRequest).getContent();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long collectViralVideo(ViralCollectVO vo, Long userId) {
        if (vo.getViralVideoId() != null) {
            SvViralVideo platform = viralVideoRepository.findByIdAndDeleted(vo.getViralVideoId(), 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.HOT_VIDEO_NOT_FOUND, "爆款视频不存在"));
            if (platform.getOwnerId() != null && platform.getOwnerId() != 0) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "仅可收藏平台爆款");
            }
            if (viralFavoriteRepository.existsByUserIdAndViralVideoId(userId, vo.getViralVideoId())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "该视频已收藏");
            }
            SvViralFavorite fav = new SvViralFavorite();
            fav.setUserId(userId);
            fav.setViralVideoId(vo.getViralVideoId());
            viralFavoriteRepository.save(fav);
            return vo.getViralVideoId();
        }

        if (vo.getDouyinVideoId() != null) {
            SvViralVideo existing = viralVideoRepository
                    .findByOwnerIdAndDouyinVideoIdAndDeleted(userId, vo.getDouyinVideoId(), 0)
                    .orElse(null);
            if (existing != null) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "该视频已收藏");
            }
        }

        SvViralVideo viral = new SvViralVideo();
        viral.setOwnerId(userId);
        viral.setDouyinVideoId(vo.getDouyinVideoId());
        viral.setTitle(vo.getTitle());
        viral.setCoverUrl(vo.getCoverUrl());
        viral.setVideoUrl(vo.getVideoUrl());
        viral.setAuthorName(vo.getAuthorName());
        viral.setViewCount(vo.getViewCount());
        viral.setLikeCount(vo.getLikeCount());
        viral.setShareCount(vo.getShareCount());
        viral.setTags(vo.getTags());
        viral.setViralScore(calculateViralScore(vo.getViewCount(), vo.getLikeCount(), vo.getShareCount()));
        return viralVideoRepository.save(viral).getId();
    }

    @Override
    public SvViralVideo getViralVideo(Long id, Long userId) {
        SvViralVideo viral = viralVideoRepository.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.HOT_VIDEO_NOT_FOUND, "爆款视频不存在"));
        if (viral.getOwnerId() == null || viral.getOwnerId() == 0) return viral;
        if (viral.getOwnerId().equals(userId)) return viral;
        if (viralFavoriteRepository.existsByUserIdAndViralVideoId(userId, id)) return viral;
        throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权限访问");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteViralVideo(Long id, Long userId) {
        SvViralVideo viral = viralVideoRepository.findByIdAndDeleted(id, 0)
            .orElseThrow(() -> new BusinessException(ErrorCode.HOT_VIDEO_NOT_FOUND, "爆款视频不存在"));
        if (viral.getOwnerId() != null && viral.getOwnerId() == 0) {
            viralFavoriteRepository.deleteByUserIdAndViralVideoId(userId, id);
            return;
        }
        if (!viral.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权限访问");
        }
        viral.setDeleted(1);
        viralVideoRepository.save(viral);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void triggerAnalysis(Long id, Long userId) {
        SvViralVideo viral = getViralVideo(id, userId);

        // 若仍关联站内 DouyinVideo（旧链路），可走进化引擎爆款拆解
        if (viral.getSourceVideoId() != null) {
            DouyinVideo video = douyinVideoRepository.findById(viral.getSourceVideoId()).orElse(null);
            if (video != null) {
                evolutionService.triggerViralAnalysis(viral.getSourceVideoId(), userId, video.getAccountId());
                log.info("触发爆款分析(进化引擎): viralId={}, sourceVideoId={}", id, viral.getSourceVideoId());
                return;
            }
        }

        chargeViralAnalysisIfNeeded(id);

        // 账号采集 / 爆款库主体：经互调网关走 video-insight 深度拆解
        if (videoInsightIntegrationBridge != null) {
            videoInsightIntegrationBridge.requestDeepAnalyzeFromShortvideo(
                    id, userId, null, "shortvideo-trigger-" + id);
            log.info("已提交短视频深度拆解: viralId={}", id);
            return;
        }

        analyzeViralWithLlm(viral);
    }

    private void chargeViralAnalysisIfNeeded(Long viralVideoId) {
        if (commercialProductChargeService == null) {
            return;
        }
        commercialProductChargeService.charge(
                CommercialProductChargeService.CommercialProductChargeCommand.of(
                        ProductCode.VIDEO_INSIGHT,
                        FeatureCode.VIDEO_VIRAL_ANALYSIS,
                        "爆款分析触发 viralVideoId=" + viralVideoId,
                        DeliveryProduct.VIDEO_INSIGHT
                ));
    }

    @Override
    public String replicateViral(Long id, Long userId) {
        SvViralVideo viral = getViralVideo(id, userId);

        // 使用 LLM 生成复刻方案
        String prompt = String.format("""
                请基于以下爆款视频，生成一个可复刻的创作方案：

                标题：%s
                作者：%s
                播放量：%d
                点赞数：%d
                分享数：%d
                标签：%s
                爆款评分：%d

                %s

                请生成：
                1. 核心创意点
                2. 可复制的拍摄方案
                3. 文案脚本建议
                4. 注意事项
                """,
                viral.getTitle(),
                viral.getAuthorName(),
                viral.getViewCount(),
                viral.getLikeCount(),
                viral.getShareCount(),
                viral.getTags(),
                viral.getViralScore(),
                viral.getAnalysisResult() != null ? "AI分析结果：\n" + viral.getAnalysisResult() : ""
        );

        try {
            AiModel model = liveAiModelResolver.resolveModel(null, "copy_processing");
            if (model == null) {
                throw new BusinessException(ErrorCode.AI_TASK_MODEL_NOT_CONFIGURED,
                        "未配置 copy_processing 任务所用 AI 模型，请在后台绑定后重试");
            }
            LlmClient.LlmResponse response = llmClient.chat(
                    model,
                    "你是专业的短视频创作顾问",
                    prompt
            );

            if (response.success()) {
                return response.content();
            } else {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成复刻方案失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成复刻方案失败: viralId={}", id, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成复刻方案失败");
        }
    }

    @Override
    public List<SvViralVideo> getRecommendedVirals(Long userId, Integer limit) {
        int lim = limit != null && limit > 0 ? Math.min(limit, 50) : 10;
        String cacheKey = CACHE_KEY_PREFIX + lim;
        if (stringRedisTemplate != null) {
            try {
                String cached = stringRedisTemplate.opsForValue().get(cacheKey);
                if (cached != null && !cached.isEmpty()) {
                    return JSON.parseArray(cached, SvViralVideo.class);
                }
            } catch (Exception e) {
                log.debug("爆款推荐缓存读取失败: {}", e.getMessage());
            }
        }
        PageRequest pageRequest = PageRequest.of(0, lim, Sort.by(Sort.Direction.DESC, "viralScore"));
        List<SvViralVideo> list = viralVideoRepository.findByOwnerIdAndDeleted(0L, 0, pageRequest).getContent();
        if (stringRedisTemplate != null && !list.isEmpty()) {
            try {
                stringRedisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(list), CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.debug("爆款推荐缓存写入失败: {}", e.getMessage());
            }
        }
        return list;
    }

    // ─── 工具方法 ──────────────────────────────────────

    /**
     * 计算爆款评分（0-100）
     */
    private int calculateViralScore(Long viewCount, Long likeCount, Long shareCount) {
        if (viewCount == null || viewCount == 0) return 0;

        // 简单算法：点赞率 * 40 + 分享率 * 60
        double likeRate = likeCount != null ? (double) likeCount / viewCount : 0;
        double shareRate = shareCount != null ? (double) shareCount / viewCount : 0;

        int score = (int) (likeRate * 40 * 100 + shareRate * 60 * 100);
        return Math.min(100, Math.max(0, score));
    }

    /**
     * 使用 LLM 分析爆款
     */
    private void analyzeViralWithLlm(SvViralVideo viral) {
        long views = Optional.ofNullable(viral.getViewCount()).orElse(0L);
        long likes = Optional.ofNullable(viral.getLikeCount()).orElse(0L);
        long shares = Optional.ofNullable(viral.getShareCount()).orElse(0L);
        String prompt = String.format("""
                请分析以下爆款视频的成功因素：

                标题：%s
                作者：%s
                播放量：%d
                点赞数：%d
                分享数：%d
                标签：%s

                请分析：
                1. 爆款因素
                2. 内容特点
                3. 可复制方法
                """,
                viral.getTitle(),
                viral.getAuthorName() != null ? viral.getAuthorName() : "",
                views,
                likes,
                shares,
                viral.getTags() != null ? viral.getTags() : ""
        );

        try {
            AiModel model = liveAiModelResolver.resolveModel(null, "copy_processing");
            if (model == null) {
                log.warn("爆款轻量分析跳过: 未配置 copy_processing 任务模型 viralId={}", viral.getId());
                throw new BusinessException(ErrorCode.AI_TASK_MODEL_NOT_CONFIGURED,
                        "未配置 copy_processing 任务所用 AI 模型，请在后台绑定后重试");
            }
            LlmClient.LlmResponse response = llmClient.chat(
                    model,
                    "你是专业的短视频分析师",
                    prompt
            );

            if (response.success()) {
                viral.setAnalysisResult(response.content());
                viralVideoRepository.save(viral);
                log.info("爆款分析完成: viralId={}", viral.getId());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("爆款分析失败: viralId={}", viral.getId(), e);
        }
    }
}
