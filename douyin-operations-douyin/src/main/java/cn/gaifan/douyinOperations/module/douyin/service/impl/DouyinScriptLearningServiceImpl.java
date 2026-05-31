package cn.gaifan.douyinOperations.module.douyin.service.impl;

import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.douyin.entity.DouyinAccount;
import cn.gaifan.douyinOperations.module.douyin.entity.DouyinVideo;
import cn.gaifan.douyinOperations.module.douyin.repository.DouyinAccountRepository;
import cn.gaifan.douyinOperations.module.douyin.repository.DouyinVideoRepository;
import cn.gaifan.douyinOperations.module.douyin.service.DouyinScriptLearningService;
import cn.gaifan.douyinOperations.module.douyinapi.client.DouyinApiClient;
import cn.gaifan.douyinOperations.module.douyinapi.service.OAuthTokenService;
import cn.gaifan.douyinOperations.module.guiguiya.client.GuiguiyaHotClient;
import cn.gaifan.douyinOperations.module.script.service.ScriptLibraryService;
import cn.gaifan.douyinOperations.module.script.vo.ScriptSaveVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * P1-1: 抖音话术学习管线实现。
 * 数据流：鬼鬼鸭热榜/本地视频库 → 评论 API → LLM 提取话术模式 → script_library(source=douyin_learn)
 */
@Slf4j
@Service
public class DouyinScriptLearningServiceImpl implements DouyinScriptLearningService {

    private static final String SOURCE_DOUYIN_LEARN = "douyin_learn";
    private static final int MAX_VIDEOS_PER_RUN = 10;
    private static final int MAX_COMMENTS_PER_VIDEO = 20;

    private static final String EXTRACT_SYSTEM_PROMPT = """
            你是直播带货话术专家，擅长从抖音爆款内容中提取可复用的话术模式。
            请分析以下视频信息和用户评论，提取 3-5 条可复用的直播话术模式，注重：
            1. 引发互动的开场白或提问方式
            2. 产品卖点的描述角度
            3. 引发共鸣或评论的表达方式
            4. 高频用语和关键词
            
            输出 JSON 数组，每条格式：
            {"title": "话术类型标题", "content": "可直接使用的话术内容", "category": "互动/种草/促单/留人/情感"}
            只输出 JSON 数组，不要额外说明。
            """;

    @Autowired(required = false)
    private DouyinApiClient douyinApiClient;

    @Autowired(required = false)
    private GuiguiyaHotClient guiguiyaHotClient;

    @Autowired(required = false)
    private DouyinVideoRepository douyinVideoRepository;

    @Autowired(required = false)
    private DouyinAccountRepository douyinAccountRepository;

    @Autowired(required = false)
    private OAuthTokenService oauthTokenService;

    @Autowired(required = false)
    private ScriptLibraryService scriptLibraryService;

    @Autowired(required = false)
    private LlmClient llmClient;

    @Autowired(required = false)
    private AiModelRepository aiModelRepository;

    @Autowired(required = false)
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired(required = false)
    private Executor learningTaskExecutor;

    @Override
    public void runLearningPipeline() {
        log.info("[DouyinScriptLearning] 开始执行话术学习管线");

        // 步骤 1: 从鬼鬼鸭热榜获取热门话题关键词
        List<String> hotKeywords = fetchHotKeywords();

        // 步骤 2: 从本地视频库获取近期视频（不需要 accessToken）
        List<DouyinVideo> recentVideos = fetchRecentVideos();

        if (recentVideos.isEmpty() && hotKeywords.isEmpty()) {
            log.info("[DouyinScriptLearning] 无可学习内容，跳过");
            return;
        }

        // 步骤 3: 并行处理视频话术提取（P1-6 修复）
        int extracted = 0;
        if (!recentVideos.isEmpty()) {
            if (learningTaskExecutor != null) {
                // 并行处理
                List<CompletableFuture<Integer>> futures = recentVideos.stream()
                    .map(video -> CompletableFuture.supplyAsync(() -> {
                        try {
                            return extractAndSaveScriptPatterns(video, null, hotKeywords);
                        } catch (Exception e) {
                            log.warn("[DouyinScriptLearning] 视频话术提取失败: videoId={}, err={}", video.getId(), e.getMessage());
                            return 0;
                        }
                    }, learningTaskExecutor))
                    .collect(Collectors.toList());

                try {
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .get(5, TimeUnit.MINUTES);
                    extracted = futures.stream()
                        .map(CompletableFuture::join)
                        .mapToInt(Integer::intValue)
                        .sum();
                } catch (Exception e) {
                    log.error("[DouyinScriptLearning] 并行处理超时或失败", e);
                    // 收集已完成的结果
                    extracted = futures.stream()
                        .filter(CompletableFuture::isDone)
                        .map(f -> {
                            try {
                                return f.get();
                            } catch (Exception ex) {
                                return 0;
                            }
                        })
                        .mapToInt(Integer::intValue)
                        .sum();
                }
            } else {
                // 降级为串行处理
                log.warn("[DouyinScriptLearning] learningTaskExecutor 未配置，降级为串行处理");
                for (DouyinVideo video : recentVideos) {
                    try {
                        int count = extractAndSaveScriptPatterns(video, null, hotKeywords);
                        extracted += count;
                    } catch (Exception e) {
                        log.warn("[DouyinScriptLearning] 视频话术提取失败: videoId={}, err={}", video.getId(), e.getMessage());
                    }
                }
            }
        }

        // 步骤 4: 基于热点关键词生成通用话术模式（无需具体视频）
        if (!hotKeywords.isEmpty() && recentVideos.isEmpty()) {
            extracted += generatePatternFromHotKeywords(hotKeywords);
        }

        log.info("[DouyinScriptLearning] 本轮学习完成，共提取 {} 条话术模式", extracted);
    }

    @Override
    public void runLearningForAccount(Long accountId, String accessToken) {
        if (accountId == null || !StringUtils.hasText(accessToken)) {
            log.warn("[DouyinScriptLearning] accountId={} 参数校验失败", accountId);
            return;
        }
        if (douyinVideoRepository == null) return;

        List<DouyinVideo> videos = douyinVideoRepository
                .findByAccountIdAndDeleted(accountId, 0, PageRequest.of(0, MAX_VIDEOS_PER_RUN))
                .getContent();

        log.info("[DouyinScriptLearning] 开始账号 {} 的话术学习，共 {} 个视频", accountId, videos.size());

        List<String> hotKeywords = fetchHotKeywords();
        int extracted = 0;
        for (DouyinVideo video : videos) {
            try {
                // 获取视频评论
                List<String> comments = fetchComments(video.getVideoId(), accessToken);
                int count = extractAndSaveScriptPatternsWithComments(video, comments, hotKeywords);
                extracted += count;
            } catch (Exception e) {
                log.warn("[DouyinScriptLearning] 账号视频话术提取失败: videoId={}", video.getId(), e.getMessage());
            }
        }
        log.info("[DouyinScriptLearning] 账号 {} 话术学习完成，提取 {} 条", accountId, extracted);
    }

    // =========== 私有辅助方法 ===========

    private List<String> fetchHotKeywords() {
        List<String> keywords = new ArrayList<>();
        if (guiguiyaHotClient != null) {
            try {
                var hotList = guiguiyaHotClient.fetchDouyinHot();
                if (hotList != null) {
                    hotList.stream()
                            .limit(10)
                            .map(h -> h.getWord())
                            .filter(StringUtils::hasText)
                            .forEach(keywords::add);
                }
            } catch (Exception e) {
                log.debug("[DouyinScriptLearning] 获取热榜失败: {}", e.getMessage());
            }
        }
        return keywords;
    }

    private List<DouyinVideo> fetchRecentVideos() {
        if (douyinVideoRepository == null) return List.of();
        try {
            return douyinVideoRepository
                    .findByVideoTypeAndDeleted("normal", 0, PageRequest.of(0, MAX_VIDEOS_PER_RUN))
                    .getContent();
        } catch (Exception e) {
            log.debug("[DouyinScriptLearning] 获取视频列表失败: {}", e.getMessage());
            return List.of();
        }
    }

    private List<String> fetchComments(String videoId, String accessToken) {
        if (douyinApiClient == null || !StringUtils.hasText(videoId) || !StringUtils.hasText(accessToken)) {
            return List.of();
        }
        try {
            var response = douyinApiClient.getCommentList(videoId, accessToken, 0, MAX_COMMENTS_PER_VIDEO);
            if (response != null && response.comments() != null) {
                return response.comments().stream()
                        .map(DouyinApiClient.CommentInfo::content)
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.debug("[DouyinScriptLearning] 获取评论失败: videoId={}, err={}", videoId, e.getMessage());
        }
        return List.of();
    }

    private int extractAndSaveScriptPatterns(DouyinVideo video, String accessToken, List<String> hotKeywords) {
        List<String> comments = fetchComments(video.getVideoId(), accessToken);
        return extractAndSaveScriptPatternsWithComments(video, comments, hotKeywords);
    }

    private int extractAndSaveScriptPatternsWithComments(DouyinVideo video, List<String> comments, List<String> hotKeywords) {
        if (llmClient == null || aiModelRepository == null) {
            log.debug("[DouyinScriptLearning] LLM 不可用，跳过提取");
            return 0;
        }

        // 构建用户消息
        StringBuilder userMsg = new StringBuilder();
        userMsg.append("视频标题：").append(video.getTitle()).append("\n");
        if (StringUtils.hasText(video.getDescription())) {
            userMsg.append("视频描述：").append(video.getDescription()).append("\n");
        }
        if (!hotKeywords.isEmpty()) {
            userMsg.append("当前热点关键词：").append(String.join("、", hotKeywords)).append("\n");
        }
        if (!comments.isEmpty()) {
            userMsg.append("\n用户评论（高频关注点）：\n");
            comments.stream().limit(15).forEach(c -> userMsg.append("- ").append(c).append("\n"));
        }

        try {
            var models = aiModelRepository.findByStatusAndDeleted(1, 0);
            if (models.isEmpty()) return 0;

            var response = llmClient.chatWithFallback(models, EXTRACT_SYSTEM_PROMPT, userMsg.toString());
            if (!response.success() || !StringUtils.hasText(response.content())) return 0;

            return saveExtractedPatterns(response.content(), resolveOwnerUserId(video.getAccountId()));
        } catch (Exception e) {
            log.warn("[DouyinScriptLearning] LLM 提取失败: {}", e.getMessage());
            return 0;
        }
    }

    private int generatePatternFromHotKeywords(List<String> hotKeywords) {
        if (llmClient == null || aiModelRepository == null || scriptLibraryService == null) return 0;
        try {
            var models = aiModelRepository.findByStatusAndDeleted(1, 0);
            if (models.isEmpty()) return 0;

            String userMsg = "当前抖音热门话题：" + String.join("、", hotKeywords)
                    + "\n请基于这些热点话题，生成适合护肤/彩妆直播间的话术模式。";
            var response = llmClient.chatWithFallback(models, EXTRACT_SYSTEM_PROMPT, userMsg);
            if (!response.success() || !StringUtils.hasText(response.content())) return 0;

            // 热点话术无特定账号归属，使用系统管理员 userId（后续可改为调度任务配置的 userId）
            return saveExtractedPatterns(response.content(), null);
        } catch (Exception e) {
            log.warn("[DouyinScriptLearning] 热点话术生成失败: {}", e.getMessage());
            return 0;
        }
    }

    private int saveExtractedPatterns(String llmResult, Long userId) {
        if (scriptLibraryService == null || userId == null) return 0;
        int saved = 0;
        try {
            // 提取 JSON 数组
            int start = llmResult.indexOf('[');
            int end = llmResult.lastIndexOf(']');
            if (start < 0 || end <= start) return 0;
            String jsonArray = llmResult.substring(start, end + 1);

            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var nodes = mapper.readTree(jsonArray);
            if (!nodes.isArray()) return 0;

            // P2-7: 批量收集后统一保存，减少数据库往返
            List<ScriptSaveVO> batchSaveList = new ArrayList<>();
            for (var node : nodes) {
                String title = node.path("title").asText("抖音学习话术");
                String content = node.path("content").asText();
                String category = node.path("category").asText("互动");

                if (!StringUtils.hasText(content) || content.length() < 10) continue;

                ScriptSaveVO saveVO = new ScriptSaveVO();
                saveVO.setUserId(userId);
                saveVO.setTitle("[抖音学习] " + title);
                saveVO.setContent(content);
                saveVO.setCategory(category);
                saveVO.setSource(SOURCE_DOUYIN_LEARN);
                saveVO.setStatus(1);
                batchSaveList.add(saveVO);
            }

            // 批量保存
            for (ScriptSaveVO saveVO : batchSaveList) {
                try {
                    scriptLibraryService.save(saveVO);
                    saved++;
                } catch (Exception e) {
                    log.debug("[DouyinScriptLearning] 保存话术模式失败: {}", e.getMessage());
                }
            }

            // 同步写入 huashu 知识库
            if (saved > 0 && knowledgeBaseService != null) {
                try {
                    Long kbId = knowledgeBaseService.resolveKbIdByName(userId, "huashu");
                    if (kbId != null) {
                        String kbContent = llmResult.substring(start, end + 1);
                        knowledgeBaseService.uploadDocument(kbId, "[抖音学习话术] 批量导入",
                                kbContent, "text", userId, "douyin_learn");
                    }
                } catch (Exception e) {
                    log.debug("[DouyinScriptLearning] 知识库写入失败: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("[DouyinScriptLearning] 解析 LLM 结果失败: {}", e.getMessage());
        }
        return saved;
    }

    /** 通过 accountId（DouyinVideo 的外键）查找账号所属 ownerId，找不到则返回 null */
    private Long resolveOwnerUserId(Long accountId) {
        if (douyinAccountRepository == null || accountId == null) return null;
        return douyinAccountRepository.findByIdAndDeleted(accountId, 0)
                .map(DouyinAccount::getOwnerId)
                .orElse(null);
    }
}
