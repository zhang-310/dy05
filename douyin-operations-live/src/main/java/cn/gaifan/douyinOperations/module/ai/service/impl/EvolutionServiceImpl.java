package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.*;
import cn.gaifan.douyinOperations.module.ai.repository.*;
import cn.gaifan.douyinOperations.module.ai.service.EvolutionService;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.service.VideoAnalysisService;
import cn.gaifan.douyinOperations.module.ai.vo.VideoCompareRequestVO;
import cn.gaifan.douyinOperations.module.ai.vo.VideoCompareResultVO;
import cn.gaifan.douyinOperations.contract.product.FeatureCode;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import cn.gaifan.douyinOperations.module.douyin.entity.DouyinVideo;
import cn.gaifan.douyinOperations.module.douyin.repository.DouyinVideoRepository;
import cn.gaifan.douyinOperations.module.platform.credit.CommercialProductChargeService;
import cn.gaifan.douyinOperations.module.platform.product.DeliveryProduct;
import cn.gaifan.douyinOperations.module.live.entity.LiveMonitor;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveMonitorRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** 进化能力实现（爆款拆解、直播复盘、视频对比等）。 */
@Service
public class EvolutionServiceImpl implements EvolutionService {

    private static final Logger log = LoggerFactory.getLogger(EvolutionServiceImpl.class);

    @Resource private AiViralAnalysisRepository viralRepo;
    @Resource private AiLiveReviewRepository liveReviewRepo;
    @Resource private AiIndexQueueRepository indexQueueRepo;
    @Resource private AiKnowledgeBaseRepository knowledgeBaseRepository;
    @Resource private AiModelRepository aiModelRepository;
    @Resource private LlmClient llmClient;
    @Resource private DouyinVideoRepository videoRepository;

    @Autowired(required = false)
    private CommercialProductChargeService commercialProductChargeService;
    @Resource private LiveSessionRepository sessionRepository;
    @Resource private LiveMonitorRepository monitorRepository;
    @Resource private LiveScriptRepository scriptRepository;
    @SuppressWarnings("unused")
    @Autowired(required = false) private VideoAnalysisService videoAnalysisService;

    // ─── 爆款拆解 ────────────────────────────────────────────────

    @Override
    public PageResultVO<Map<String, Object>> searchViralAnalysis(Long ownerId, Integer status, int page, int rows) {
        Specification<AiViralAnalysis> spec = (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (ownerId != null) predicates.add(cb.equal(root.get("ownerId"), ownerId));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<AiViralAnalysis> p = viralRepo.findAll(spec,
                PageRequest.of(page, rows, Sort.by(Sort.Direction.DESC, "createTime")));
        return PageResultVO.of(p.getTotalElements(),
                p.getContent().stream().map(this::viralToMap).toList(), page, rows);
    }

    @Override
    public Map<String, Object> getViralAnalysis(Long id, Long ownerId) {
        AiViralAnalysis e = viralRepo.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "爆款分析不存在"));
        if (ownerId != null && !ownerId.equals(e.getOwnerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该爆款分析");
        }
        Map<String, Object> m = viralToMap(e);
        videoRepository.findByIdAndDeleted(e.getVideoId(), 0).ifPresent(v -> m.put("videoTitle", v.getTitle()));
        return m;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long triggerViralAnalysis(Long videoId, Long ownerId, Long accountId) {
        return viralRepo.findByVideoIdAndDeleted(videoId, 0).map(AiViralAnalysis::getId).orElseGet(() -> {
            DouyinVideo video = videoRepository.findByIdAndDeleted(videoId, 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "视频不存在"));

            if (commercialProductChargeService != null) {
                commercialProductChargeService.charge(
                        CommercialProductChargeService.CommercialProductChargeCommand.of(
                                ProductCode.DOUYIN_OPS,
                                FeatureCode.DOUYIN_VIDEO_ANALYSIS,
                                "抖音视频分析 videoId=" + videoId,
                                DeliveryProduct.DOUYIN_OPS
                        ));
            }

            AiViralAnalysis entity = new AiViralAnalysis();
            entity.setVideoId(videoId);
            entity.setOwnerId(ownerId);
            entity.setAccountId(accountId);
            entity.setViewCount(video.getViewCount());
            entity.setStatus(0);
            long savedId = viralRepo.save(entity).getId();

            // 异步执行 AI 分析
            asyncViralAnalysis(savedId, video);
            return savedId;
        });
    }

    @Async
    public void asyncViralAnalysis(Long analysisId, DouyinVideo video) {
        try {
            AiModel model = findAvailableModel();
            if (model == null) {
                log.warn("无可用 AI 模型，爆款分析 {} 标记失败", analysisId);
                viralRepo.completeAnalysis(analysisId, 2, "无可用 AI 模型", null, null, 0, 0L, null);
                return;
            }

            String system = "你是一位专业的短视频数据分析师。请严格用中文思考，且最终回复必须是合法 JSON（仅此一段，无 markdown、无解释）。";
            String desc = video.getDescription() != null ? video.getDescription() : "";
            String prompt = String.format("""
                    请仅输出一个 JSON 对象（不要 markdown 代码块，不要其它文字），字段如下：
                    {"viralScore":<0-100 整数>,"successFactors":"<3-5 条成功因素，分号或换行分隔>","replicableMethods":"<3-5 条可复制方法>","report":"<200-400 字综合报告>"}

                    【数据边界】标题、简介与互动数据来自抖音同步到本系统的元数据；系统未下载该视频音视频流，无 ASR 与逐帧画面。请勿编造具体台词或断言未看见的画面细节；report 首句须包含「本分析仅基于元数据」。

                    视频标题：%s
                    视频简介：%s
                    播放量：%d
                    点赞数：%d
                    评论数：%d
                    分享数：%d
                    下载数：%d
                    """,
                    video.getTitle(),
                    desc,
                    video.getViewCount(),
                    video.getLikeCount(),
                    video.getCommentCount(),
                    video.getShareCount(),
                    video.getDownloadCount());

            LlmClient.LlmResponse response = llmClient.chat(model, system, prompt);

            if (response.success() && response.content() != null) {
                String content = response.content();
                ViralLlmParsed parsed = parseViralLlmOutput(content);
                int score = parsed.score();
                String factors = parsed.factors();
                String methods = parsed.methods();
                String reportStored = parsed.reportContentForDb();

                viralRepo.completeAnalysis(analysisId, 1, reportStored, factors, methods,
                        score, response.tokensUsed(), model.getModelVersion());
                aiModelRepository.incrementQuotaUsed(model.getId(), response.tokensUsed());
                log.info("爆款分析完成: id={}, score={}, tokens={}", analysisId, score, response.tokensUsed());

                // P0 爆款拆解入库：质量≥50 推入索引队列，标记 source=viral_analysis
                if (score >= 50) {
                    AiViralAnalysis entity = viralRepo.findById(analysisId).orElse(null);
                    if (entity != null) {
                        Long targetKbId = resolveTargetKbId(entity.getOwnerId());
                        if (targetKbId != null) {
                            enqueueIndex("viral_analysis", analysisId, parsed.indexText(), 1, targetKbId);
                            log.info("爆款拆解报告已入索引队列: viralId={}, kbId={}", analysisId, targetKbId);
                        }
                    }
                }
            } else {
                viralRepo.completeAnalysis(analysisId, 2,
                        "AI 分析失败: " + response.errorMsg(), null, null, 0, 0L, null);
                log.error("爆款分析失败: id={}, error={}", analysisId, response.errorMsg());
            }
        } catch (Exception e) {
            log.error("爆款分析异常: id={}", analysisId, e);
            try {
                viralRepo.completeAnalysis(analysisId, 2, "分析异常: " + e.getMessage(), null, null, 0, 0L, null);
            } catch (Exception ex) {
                log.debug("爆款分析状态更新失败: id={}, {}", analysisId, ex.getMessage());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeViralAnalysis(Long id, String report, String successFactors,
                                      String replicableMethods, Integer qualityScore,
                                      Long tokensUsed, String modelUsed,
                                      Long requestUserId, boolean callbackSecretValid) {
        AiViralAnalysis e = viralRepo.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "爆款分析不存在"));
        if (!callbackSecretValid) {
            if (requestUserId == null || !requestUserId.equals(e.getOwnerId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "无权更新该爆款分析");
            }
        }
        viralRepo.completeAnalysis(id, 1, report, successFactors, replicableMethods,
                qualityScore != null ? qualityScore : 0, tokensUsed != null ? tokensUsed : 0L, modelUsed);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteViralAnalysis(Long id, Long ownerId) {
        AiViralAnalysis entity = viralRepo.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "爆款分析不存在"));
        if (ownerId == null || !ownerId.equals(entity.getOwnerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权删除该爆款分析");
        }
        entity.setDeleted(1);
        viralRepo.save(entity);
    }

    // ─── 直播复盘 ────────────────────────────────────────────────

    @Override
    public PageResultVO<Map<String, Object>> searchLiveReviews(Long ownerId, Integer status, int page, int rows) {
        Specification<AiLiveReview> spec = (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (ownerId != null) predicates.add(cb.equal(root.get("ownerId"), ownerId));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<AiLiveReview> p = liveReviewRepo.findAll(spec,
                PageRequest.of(page, rows, Sort.by(Sort.Direction.DESC, "createTime")));
        return PageResultVO.of(p.getTotalElements(),
                p.getContent().stream().map(this::liveReviewToMap).toList(), page, rows);
    }

    @Override
    public Map<String, Object> getLiveReview(Long id) {
        return liveReviewToMap(liveReviewRepo.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播复盘不存在")));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long triggerLiveReview(Long sessionId, Long ownerId, Long accountId) {
        return liveReviewRepo.findBySessionIdAndDeleted(sessionId, 0).map(AiLiveReview::getId).orElseGet(() -> {
            LiveSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));

            // 聚合监控数据
            List<LiveMonitor> monitors = monitorRepository.findBySessionId(sessionId);
            long peakViewers = monitors.stream().mapToLong(m -> m.getViewers() != null ? m.getViewers() : 0).max().orElse(0);
            long totalViewers = monitors.stream().mapToLong(m -> m.getViewers() != null ? m.getViewers() : 0).sum();

            // 获取话术数据
            List<LiveScript> scripts = scriptRepository.findBySessionIdAndDeleted(sessionId, 0);

            AiLiveReview entity = new AiLiveReview();
            entity.setSessionId(sessionId);
            entity.setOwnerId(ownerId);
            entity.setAccountId(accountId);
            entity.setPeakViewers(peakViewers);
            entity.setTotalViewers(totalViewers);
            entity.setStatus(0);
            long savedId = liveReviewRepo.save(entity).getId();

            // 异步执行 AI 复盘
            asyncLiveReview(savedId, session, monitors, scripts);
            return savedId;
        });
    }

    @Async
    public void asyncLiveReview(Long reviewId, LiveSession session, List<LiveMonitor> monitors, List<LiveScript> scripts) {
        try {
            AiModel model = findAvailableModel();
            if (model == null) {
                log.warn("无可用 AI 模型，直播复盘 {} 标记失败", reviewId);
                liveReviewRepo.completeReview(reviewId, 2, "无可用 AI 模型", null, null, 0L, null);
                return;
            }

            // 构建监控数据摘要
            int peakViewers = monitors.stream().mapToInt(m -> m.getViewers() != null ? m.getViewers() : 0).max().orElse(0);
            long totalLikes = monitors.stream().mapToLong(m -> m.getLikes() != null ? m.getLikes() : 0L).max().orElse(0);
            int totalComments = monitors.stream().mapToInt(m -> m.getComments() != null ? m.getComments() : 0).sum();
            int totalShares = monitors.stream().mapToInt(m -> m.getShares() != null ? m.getShares() : 0).sum();
            int dataPoints = monitors.size();

            // 构建话术摘要
            long executedScripts = scripts.stream().filter(s -> s.getExecuted() != null && s.getExecuted() == 1).count();
            String scriptSummary = scripts.stream()
                    .map(s -> String.format("- [%s] %s（%s）",
                            s.getScriptType() != null ? s.getScriptType() : "custom",
                            s.getScriptContent() != null && s.getScriptContent().length() > 50
                                    ? s.getScriptContent().substring(0, 50) + "..."
                                    : s.getScriptContent(),
                            s.getExecuted() != null && s.getExecuted() == 1 ? "已执行" : "未执行"))
                    .collect(Collectors.joining("\n"));

            String system = "你是一位专业的直播数据分析师，擅长复盘直播表现并给出改进建议。请用中文回答。";
            String prompt = String.format("""
                    请对以下直播场次进行全面复盘分析。

                    ## 直播基本信息
                    - 标题：%s
                    - 描述：%s
                    - 状态：%s

                    ## 监控数据摘要
                    - 峰值观众：%d
                    - 总点赞：%d
                    - 总评论：%d
                    - 总分享：%d
                    - 数据采集点：%d

                    ## 话术执行情况
                    - 总话术数：%d
                    - 已执行：%d
                    - 话术列表：
                    %s

                    请按以下格式输出：

                    ## 最佳话术 TOP3
                    分析哪些话术效果最好，为什么

                    ## 薄弱环节
                    指出直播中的不足之处

                    ## 改进建议
                    给出3-5条具体可执行的改进建议

                    ## 综合复盘报告
                    300-500字的综合分析
                    """,
                    session.getLiveTitle(),
                    session.getLiveDescription() != null ? session.getLiveDescription() : "无",
                    session.getStatus() == 2 ? "已结束" : "进行中",
                    peakViewers, totalLikes, totalComments, totalShares, dataPoints,
                    scripts.size(), executedScripts,
                    scriptSummary.isEmpty() ? "无话术记录" : scriptSummary);

            LlmClient.LlmResponse response = llmClient.chat(model, system, prompt);

            if (response.success() && response.content() != null) {
                String content = response.content();
                String topScripts = extractSection(content, "最佳话术");
                String weakPoints = extractSection(content, "薄弱环节");

                liveReviewRepo.completeReview(reviewId, 1, content, topScripts, weakPoints,
                        response.tokensUsed(), model.getModelVersion());
                aiModelRepository.incrementQuotaUsed(model.getId(), response.tokensUsed());
                log.info("直播复盘完成: id={}, tokens={}", reviewId, response.tokensUsed());

                // 直播复盘入库：报告≥300字推入索引队列
                if (content.length() >= 300) {
                    AiLiveReview entity = liveReviewRepo.findById(reviewId).orElse(null);
                    if (entity != null) {
                        Long targetKbId = resolveTargetKbId(entity.getOwnerId());
                        if (targetKbId != null) {
                            enqueueIndex("live_review", reviewId, content, 2, targetKbId);
                            log.info("直播复盘报告已入索引队列: reviewId={}, kbId={}", reviewId, targetKbId);
                        }
                    }
                }
            } else {
                liveReviewRepo.completeReview(reviewId, 2,
                        "AI 分析失败: " + response.errorMsg(), null, null, 0L, null);
                log.error("直播复盘失败: id={}, error={}", reviewId, response.errorMsg());
            }
        } catch (Exception e) {
            log.error("直播复盘异常: id={}", reviewId, e);
            try {
                liveReviewRepo.completeReview(reviewId, 2, "复盘异常: " + e.getMessage(), null, null, 0L, null);
            } catch (Exception ex) {
                log.debug("直播复盘状态更新失败: id={}, {}", reviewId, ex.getMessage());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeLiveReview(Long id, String report, String topScripts,
                                   String weakPoints, Long tokensUsed, String modelUsed) {
        liveReviewRepo.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播复盘不存在"));
        liveReviewRepo.completeReview(id, 1, report, topScripts, weakPoints,
                tokensUsed != null ? tokensUsed : 0L, modelUsed);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLiveReview(Long id) {
        AiLiveReview entity = liveReviewRepo.findByIdAndDeleted(id, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播复盘不存在"));
        entity.setDeleted(1);
        liveReviewRepo.save(entity);
    }

    // ─── 索引队列 ────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long enqueueIndex(String sourceType, Long sourceId, String content, Integer priority) {
        return enqueueIndex(sourceType, sourceId, content, priority, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long enqueueIndex(String sourceType, Long sourceId, String content, Integer priority, Long targetKbId) {
        AiIndexQueue queue = new AiIndexQueue();
        queue.setSourceType(sourceType);
        queue.setSourceId(sourceId);
        queue.setContent(content);
        queue.setPriority(priority != null ? priority : 5);
        queue.setTargetKbId(targetKbId);
        return indexQueueRepo.save(queue).getId();
    }

    @Override
    public Map<String, Object> getEvolutionStats(Long ownerId) {
        Specification<AiViralAnalysis> viralScope = (root, q, cb) -> {
            var p = new ArrayList<Predicate>();
            p.add(cb.equal(root.get("deleted"), 0));
            if (ownerId != null) {
                p.add(cb.equal(root.get("ownerId"), ownerId));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        Specification<AiViralAnalysis> viralDone = (root, q, cb) -> {
            var p = new ArrayList<Predicate>();
            p.add(cb.equal(root.get("deleted"), 0));
            p.add(cb.equal(root.get("status"), 1));
            if (ownerId != null) {
                p.add(cb.equal(root.get("ownerId"), ownerId));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        long totalViral = viralRepo.count(viralScope);
        long doneViral = viralRepo.count(viralDone);

        Specification<AiLiveReview> liveScope = (root, q, cb) -> {
            var p = new ArrayList<Predicate>();
            p.add(cb.equal(root.get("deleted"), 0));
            if (ownerId != null) {
                p.add(cb.equal(root.get("ownerId"), ownerId));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        Specification<AiLiveReview> liveDone = (root, q, cb) -> {
            var p = new ArrayList<Predicate>();
            p.add(cb.equal(root.get("deleted"), 0));
            p.add(cb.equal(root.get("status"), 1));
            if (ownerId != null) {
                p.add(cb.equal(root.get("ownerId"), ownerId));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        long totalLive = liveReviewRepo.count(liveScope);
        long doneLive = liveReviewRepo.count(liveDone);

        long pendingQueue = indexQueueRepo.findAll().stream()
                .filter(queue -> "pending".equals(queue.getStatus())).count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("viralAnalysisTotal", totalViral);
        stats.put("viralAnalysisDone", doneViral);
        stats.put("liveReviewTotal", totalLive);
        stats.put("liveReviewDone", doneLive);
        stats.put("indexQueuePending", pendingQueue);
        return stats;
    }

    // ─── 工具方法 ───────────────────────────────────────────────

    private AiModel findAvailableModel() {
        List<AiModel> models = aiModelRepository.findByStatusAndDeleted(1, 0);
        if (models.isEmpty()) return null;

        // 优先寻找高性能模型 (DeepSeek 或 Claude)
        var preferred = models.stream()
                .filter(m -> m.getQuotaLimit() == null || m.getQuotaLimit() == 0 || m.getQuotaUsed() < m.getQuotaLimit())
                .filter(m -> {
                    String name = m.getModelName().toLowerCase();
                    return name.contains("deepseek") || name.contains("claude");
                })
                .findFirst();

        if (preferred.isPresent()) return preferred.get();

        return models.stream()
                .filter(m -> m.getQuotaLimit() == null || m.getQuotaLimit() == 0 || m.getQuotaUsed() < m.getQuotaLimit())
                .findFirst()
                .orElse(models.get(0));
    }

    /** 解析目标知识库：取用户首个启用的 KB */
    private Long resolveTargetKbId(Long userId) {
        if (userId == null) return null;
        var list = knowledgeBaseRepository.findByUserIdAndDeletedOrderByCreateTimeDesc(userId, 0);
        return list.isEmpty() ? null : list.get(0).getId();
    }

    private record ViralLlmParsed(int score, String factors, String methods, String reportContentForDb, String indexText) {}

    /**
     * 优先解析 JSON；失败则回退到章节/正则（兼容旧模型输出）。
     */
    private ViralLlmParsed parseViralLlmOutput(String rawContent) {
        String cleaned = stripJsonCodeFence(rawContent.trim());
        try {
            JSONObject o = JSON.parseObject(cleaned);
            int score = 50;
            if (o.containsKey("viralScore")) {
                score = Math.min(100, Math.max(0, o.getIntValue("viralScore")));
            } else {
                score = extractScore(rawContent);
            }
            String factors = trimToNull(o.getString("successFactors"));
            String methods = trimToNull(o.getString("replicableMethods"));
            String report = trimToNull(o.getString("report"));
            if (factors == null) {
                factors = trimToNull(extractSection(rawContent, "成功因素"));
            }
            if (methods == null) {
                methods = trimToNull(extractSection(rawContent, "可复制方法"));
            }
            if (report == null) {
                report = cleaned;
            }
            o.put("viralScore", score);
            if (factors != null) {
                o.put("successFactors", factors);
            }
            if (methods != null) {
                o.put("replicableMethods", methods);
            }
            o.put("report", report);
            String canonical = JSON.toJSONString(o);
            return new ViralLlmParsed(score, factors, methods, canonical, report);
        } catch (Exception e) {
            log.debug("爆款 JSON 解析回退: {}", e.getMessage());
            int score = extractScore(rawContent);
            String factors = extractSection(rawContent, "成功因素");
            String methods = extractSection(rawContent, "可复制方法");
            return new ViralLlmParsed(score, factors, methods, rawContent, rawContent);
        }
    }

    private static String stripJsonCodeFence(String s) {
        String t = s.trim();
        if (!t.startsWith("```")) {
            return t;
        }
        int nl = t.indexOf('\n');
        if (nl > 0) {
            t = t.substring(nl + 1);
        }
        int end = t.lastIndexOf("```");
        if (end > 0) {
            t = t.substring(0, end).trim();
        }
        return t;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String x = s.trim();
        return x.isEmpty() ? null : x;
    }

    private int extractScore(String content) {
        try {
            var matcher = java.util.regex.Pattern.compile("(\\d{1,3})\\s*[/／分]").matcher(content);
            if (matcher.find()) return Math.min(Integer.parseInt(matcher.group(1)), 100);
            matcher = java.util.regex.Pattern.compile("评分[：:]?\\s*(\\d{1,3})").matcher(content);
            if (matcher.find()) return Math.min(Integer.parseInt(matcher.group(1)), 100);
        } catch (Exception e) {
            log.debug("评分提取失败，使用默认值: {}", e.getMessage());
        }
        return 50;
    }

    private String extractSection(String content, String sectionName) {
        if (content == null) return null;
        int start = content.indexOf(sectionName);
        if (start < 0) return null;
        int nextSection = content.indexOf("##", start + sectionName.length());
        String section = nextSection > 0 ? content.substring(start, nextSection) : content.substring(start);
        return section.trim();
    }

    // ─── toMap ───────────────────────────────────────────────────

    private Map<String, Object> viralToMap(AiViralAnalysis e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId()); m.put("videoId", e.getVideoId()); m.put("accountId", e.getAccountId());
        m.put("ownerId", e.getOwnerId()); m.put("viralScore", e.getViralScore());
        m.put("viewCount", e.getViewCount()); m.put("avgViewCount", e.getAvgViewCount());
        m.put("successFactors", e.getSuccessFactors()); m.put("replicableMethods", e.getReplicableMethods());
        m.put("reportContent", e.getReportContent()); m.put("qualityScore", e.getQualityScore());
        m.put("modelUsed", e.getModelUsed()); m.put("tokensUsed", e.getTokensUsed());
        m.put("status", e.getStatus()); m.put("createTime", e.getCreateTime());
        return m;
    }

    private Map<String, Object> liveReviewToMap(AiLiveReview e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId()); m.put("sessionId", e.getSessionId()); m.put("accountId", e.getAccountId());
        m.put("ownerId", e.getOwnerId()); m.put("totalViewers", e.getTotalViewers());
        m.put("totalGmv", e.getTotalGmv()); m.put("conversionRate", e.getConversionRate());
        m.put("peakViewers", e.getPeakViewers()); m.put("topScripts", e.getTopScripts());
        m.put("weakPoints", e.getWeakPoints()); m.put("reportContent", e.getReportContent());
        m.put("modelUsed", e.getModelUsed()); m.put("tokensUsed", e.getTokensUsed());
        m.put("status", e.getStatus()); m.put("createTime", e.getCreateTime());
        return m;
    }

    // ─── 视频对比分析 ────────────────────────────────────────────────

    @Override
    public VideoCompareResultVO compareVideos(VideoCompareRequestVO vo, Long userId) {
        if (vo.getVideoId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "主视频 ID 不能为空");
        }
        if (vo.getCompareVideoIds() == null || vo.getCompareVideoIds().isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "对比视频列表不能为空");
        }
        if (vo.getCompareVideoIds().size() > 5) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "最多支持对比 5 个视频");
        }

        long startTime = System.currentTimeMillis();

        // 获取主视频
        DouyinVideo mainVideo = videoRepository.findByIdAndDeleted(vo.getVideoId(), 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "主视频不存在"));

        // 获取对比视频
        List<DouyinVideo> compareVideos = new ArrayList<>();
        for (Long compareId : vo.getCompareVideoIds()) {
            videoRepository.findByIdAndDeleted(compareId, 0).ifPresent(compareVideos::add);
        }

        if (compareVideos.isEmpty()) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "对比视频不存在");
        }

        // 构建结果
        VideoCompareResultVO result = new VideoCompareResultVO();
        result.setMainVideo(buildVideoInfo(mainVideo));
        result.setCompareVideos(compareVideos.stream().map(this::buildVideoInfo).collect(Collectors.toList()));

        // 数据对比分析
        result.setDataComparison(buildDataComparison(mainVideo, compareVideos));

        // 内容结构对比
        result.setContentComparison(buildContentComparison(mainVideo, compareVideos));

        // AI 分析报告和建议
        generateAiAnalysis(result, mainVideo, compareVideos, vo.getCompareType());

        result.setGenerationTime(System.currentTimeMillis() - startTime);

        return result;
    }

    private VideoCompareResultVO.VideoInfo buildVideoInfo(DouyinVideo video) {
        VideoCompareResultVO.VideoInfo info = new VideoCompareResultVO.VideoInfo();
        info.setVideoId(video.getId());
        info.setTitle(video.getTitle());
        info.setCoverUrl(null);  // DouyinVideo 无 coverUrl 字段
        info.setPlayCount(video.getViewCount());
        info.setLikeCount(video.getLikeCount());
        info.setCommentCount(video.getCommentCount());
        info.setShareCount(video.getShareCount());

        // 计算完播率（DouyinVideo 无 finishCount，暂不计算）
        info.setCompletionRate(null);

        if (video.getPublishTime() != null) {
            info.setPublishTime(video.getPublishTime().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        info.setDuration(null);  // DouyinVideo 无 duration 字段
        return info;
    }

    private VideoCompareResultVO.DataComparison buildDataComparison(DouyinVideo mainVideo, List<DouyinVideo> compareVideos) {
        VideoCompareResultVO.DataComparison comparison = new VideoCompareResultVO.DataComparison();

        // 播放量对比
        comparison.setPlayCount(buildMetricComparison("播放量",
            mainVideo.getViewCount(),
            compareVideos.stream().map(DouyinVideo::getViewCount).filter(Objects::nonNull).collect(Collectors.toList())));

        // 点赞率对比
        comparison.setLikeRate(buildRateComparison("点赞率",
            mainVideo.getViewCount(), mainVideo.getLikeCount(),
            compareVideos));

        // 评论率对比
        comparison.setCommentRate(buildCommentRateComparison("评论率",
            mainVideo.getViewCount(), mainVideo.getCommentCount(),
            compareVideos));

        // 分享率对比
        comparison.setShareRate(buildShareRateComparison("分享率",
            mainVideo.getViewCount(), mainVideo.getShareCount(),
            compareVideos));

        // 完播率对比
        comparison.setCompletionRate(buildCompletionRateComparison("完播率",
            mainVideo, compareVideos));

        return comparison;
    }

    private VideoCompareResultVO.MetricComparison buildMetricComparison(String name, Long mainValue, List<Long> compareValues) {
        VideoCompareResultVO.MetricComparison metric = new VideoCompareResultVO.MetricComparison();
        metric.setMetricName(name);
        metric.setMainValue(mainValue != null ? mainValue.doubleValue() : 0.0);

        if (!compareValues.isEmpty()) {
            double avg = compareValues.stream().mapToLong(v -> v).average().orElse(0);
            double max = compareValues.stream().mapToLong(v -> v).max().orElse(0);
            double min = compareValues.stream().mapToLong(v -> v).min().orElse(0);

            metric.setAvgCompareValue(avg);
            metric.setMaxCompareValue(max);
            metric.setMinCompareValue(min);

            // 判断趋势
            if (metric.getMainValue() > avg * 1.2) {
                metric.setTrend("higher");
                metric.setAnalysis(String.format("%s显著高于对比视频平均值 %.0f%%", name, ((metric.getMainValue() / avg - 1) * 100)));
            } else if (metric.getMainValue() < avg * 0.8) {
                metric.setTrend("lower");
                metric.setAnalysis(String.format("%s低于对比视频平均值 %.0f%%", name, ((1 - metric.getMainValue() / avg) * 100)));
            } else {
                metric.setTrend("similar");
                metric.setAnalysis(String.format("%s与对比视频接近", name));
            }
        }

        return metric;
    }

    private VideoCompareResultVO.MetricComparison buildRateComparison(String name, Long playCount, Long likeCount, List<DouyinVideo> compareVideos) {
        VideoCompareResultVO.MetricComparison metric = new VideoCompareResultVO.MetricComparison();
        metric.setMetricName(name);

        double mainRate = (playCount != null && playCount > 0 && likeCount != null)
            ? (likeCount.doubleValue() / playCount * 100) : 0.0;
        metric.setMainValue(BigDecimal.valueOf(mainRate).setScale(2, RoundingMode.HALF_UP).doubleValue());

        List<Double> compareRates = compareVideos.stream()
            .filter(v -> v.getViewCount() != null && v.getViewCount() > 0 && v.getLikeCount() != null)
            .map(v -> (v.getLikeCount().doubleValue() / v.getViewCount() * 100))
            .collect(Collectors.toList());

        if (!compareRates.isEmpty()) {
            double avg = compareRates.stream().mapToDouble(r -> r).average().orElse(0);
            metric.setAvgCompareValue(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP).doubleValue());
            metric.setMaxCompareValue(compareRates.stream().mapToDouble(r -> r).max().orElse(0));
            metric.setMinCompareValue(compareRates.stream().mapToDouble(r -> r).min().orElse(0));

            if (mainRate > avg * 1.2) {
                metric.setTrend("higher");
                metric.setAnalysis(String.format("%s %.2f%% 高于平均 %.2f%%", name, mainRate, avg));
            } else if (mainRate < avg * 0.8) {
                metric.setTrend("lower");
                metric.setAnalysis(String.format("%s %.2f%% 低于平均 %.2f%%", name, mainRate, avg));
            } else {
                metric.setTrend("similar");
                metric.setAnalysis(String.format("%s %.2f%% 接近平均水平", name, mainRate));
            }
        }

        return metric;
    }

    private VideoCompareResultVO.MetricComparison buildCommentRateComparison(String name, Long playCount, Long commentCount, List<DouyinVideo> compareVideos) {
        VideoCompareResultVO.MetricComparison metric = new VideoCompareResultVO.MetricComparison();
        metric.setMetricName(name);

        double mainRate = (playCount != null && playCount > 0 && commentCount != null)
            ? (commentCount.doubleValue() / playCount * 100) : 0.0;
        metric.setMainValue(BigDecimal.valueOf(mainRate).setScale(2, RoundingMode.HALF_UP).doubleValue());

        List<Double> compareRates = compareVideos.stream()
            .filter(v -> v.getViewCount() != null && v.getViewCount() > 0 && v.getCommentCount() != null)
            .map(v -> (v.getCommentCount().doubleValue() / v.getViewCount() * 100))
            .collect(Collectors.toList());

        if (!compareRates.isEmpty()) {
            double avg = compareRates.stream().mapToDouble(r -> r).average().orElse(0);
            metric.setAvgCompareValue(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP).doubleValue());
            metric.setTrend(mainRate > avg ? "higher" : (mainRate < avg ? "lower" : "similar"));
            metric.setAnalysis(String.format("%s %.2f%%", name, mainRate));
        }

        return metric;
    }

    private VideoCompareResultVO.MetricComparison buildShareRateComparison(String name, Long playCount, Long shareCount, List<DouyinVideo> compareVideos) {
        VideoCompareResultVO.MetricComparison metric = new VideoCompareResultVO.MetricComparison();
        metric.setMetricName(name);

        double mainRate = (playCount != null && playCount > 0 && shareCount != null)
            ? (shareCount.doubleValue() / playCount * 100) : 0.0;
        metric.setMainValue(BigDecimal.valueOf(mainRate).setScale(2, RoundingMode.HALF_UP).doubleValue());

        List<Double> compareRates = compareVideos.stream()
            .filter(v -> v.getViewCount() != null && v.getViewCount() > 0 && v.getShareCount() != null)
            .map(v -> (v.getShareCount().doubleValue() / v.getViewCount() * 100))
            .collect(Collectors.toList());

        if (!compareRates.isEmpty()) {
            double avg = compareRates.stream().mapToDouble(r -> r).average().orElse(0);
            metric.setAvgCompareValue(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP).doubleValue());
            metric.setTrend(mainRate > avg ? "higher" : (mainRate < avg ? "lower" : "similar"));
            metric.setAnalysis(String.format("%s %.2f%%", name, mainRate));
        }

        return metric;
    }

    private VideoCompareResultVO.MetricComparison buildCompletionRateComparison(String name, DouyinVideo mainVideo, List<DouyinVideo> compareVideos) {
        VideoCompareResultVO.MetricComparison metric = new VideoCompareResultVO.MetricComparison();
        metric.setMetricName(name);

        // DouyinVideo 无 finishCount，完播率暂设为 0
        double mainRate = 0.0;
        metric.setMainValue(mainRate);

        List<Double> compareRates = new ArrayList<>();

        if (!compareRates.isEmpty()) {
            double avg = compareRates.stream().mapToDouble(r -> r).average().orElse(0);
            metric.setAvgCompareValue(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP).doubleValue());
            metric.setTrend(mainRate > avg ? "higher" : (mainRate < avg ? "lower" : "similar"));
            metric.setAnalysis(String.format("%s %.2f%%", name, mainRate));
        }

        return metric;
    }

    private VideoCompareResultVO.ContentComparison buildContentComparison(DouyinVideo mainVideo, List<DouyinVideo> compareVideos) {
        VideoCompareResultVO.ContentComparison comparison = new VideoCompareResultVO.ContentComparison();

        // 标题对比
        VideoCompareResultVO.TextComparison titleComp = new VideoCompareResultVO.TextComparison();
        titleComp.setMainText(mainVideo.getTitle());
        titleComp.setCompareTexts(compareVideos.stream().map(DouyinVideo::getTitle).collect(Collectors.toList()));
        titleComp.setAnalysis("标题长度: " + (mainVideo.getTitle() != null ? mainVideo.getTitle().length() : 0) + " 字");
        comparison.setTitle(titleComp);

        // 封面对比（DouyinVideo 无 coverUrl）
        comparison.setCoverAnalysis("封面 URL: 未设置");

        // 发布时间对比
        if (mainVideo.getPublishTime() != null) {
            int hour = mainVideo.getPublishTime().toLocalDateTime().getHour();
            String timeSlot = hour < 12 ? "上午" : (hour < 18 ? "下午" : "晚上");
            comparison.setPublishTimeAnalysis("发布时段: " + timeSlot + " " + hour + "点");
        }

        // 时长对比（DouyinVideo 无 duration）
        comparison.setDurationAnalysis("视频时长: 暂无");

        return comparison;
    }

    private void generateAiAnalysis(VideoCompareResultVO result, DouyinVideo mainVideo, List<DouyinVideo> compareVideos, String compareType) {
        AiModel model = findAvailableModel();
        if (model == null) {
            result.setAnalysisReport("AI 分析不可用：无可用模型");
            result.setSuggestions(new ArrayList<>());
            return;
        }

        String systemPrompt = """
                你是一位专业的短视频数据分析师，擅长通过数据对比发现问题并提供改进建议。

                任务：分析主视频与对比视频的差异，提供具体的改进建议。

                输出格式（严格 JSON）：
                {
                  "analysis_report": "综合分析报告（200-400字）",
                  "suggestions": [
                    {
                      "category": "title/cover/content/timing",
                      "suggestion": "具体建议",
                      "priority": 1-5,
                      "reason": "理由说明"
                    }
                  ]
                }
                """;

        String userPrompt = buildComparePrompt(mainVideo, compareVideos, compareType, result.getDataComparison());

        try {
            LlmClient.LlmResponse response = llmClient.chat(model, systemPrompt, userPrompt);

            if (response.success() && response.content() != null) {
                parseAiAnalysisResponse(response.content(), result);
                result.setTokenUsage((int) Math.min(response.tokensUsed(), Integer.MAX_VALUE));
                aiModelRepository.incrementQuotaUsed(model.getId(), response.tokensUsed());
                log.info("视频对比 AI 分析完成: mainVideo={}, tokens={}", mainVideo.getId(), response.tokensUsed());
            } else {
                result.setAnalysisReport("AI 分析失败: " + response.errorMsg());
                result.setSuggestions(new ArrayList<>());
            }
        } catch (Exception e) {
            log.error("视频对比 AI 分析异常", e);
            result.setAnalysisReport("AI 分析异常: " + e.getMessage());
            result.setSuggestions(new ArrayList<>());
        }
    }

    private String buildComparePrompt(DouyinVideo mainVideo, List<DouyinVideo> compareVideos, String compareType, VideoCompareResultVO.DataComparison dataComp) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("对比类型: ").append("similar".equals(compareType) ? "同类视频对比" : "历史视频对比").append("\n\n");

        prompt.append("主视频信息:\n");
        prompt.append("- 标题: ").append(mainVideo.getTitle()).append("\n");
        prompt.append("- 播放量: ").append(mainVideo.getViewCount()).append("\n");
        prompt.append("- 点赞数: ").append(mainVideo.getLikeCount()).append("\n");
        prompt.append("- 评论数: ").append(mainVideo.getCommentCount()).append("\n");
        prompt.append("- 分享数: ").append(mainVideo.getShareCount()).append("\n\n");

        prompt.append("对比视频平均数据:\n");
        prompt.append("- 播放量: ").append(String.format("%.0f", dataComp.getPlayCount().getAvgCompareValue())).append("\n");
        prompt.append("- 点赞率: ").append(String.format("%.2f%%", dataComp.getLikeRate().getAvgCompareValue())).append("\n");
        prompt.append("- 评论率: ").append(String.format("%.2f%%", dataComp.getCommentRate().getAvgCompareValue())).append("\n");
        prompt.append("- 分享率: ").append(String.format("%.2f%%", dataComp.getShareRate().getAvgCompareValue())).append("\n\n");

        prompt.append("请分析主视频的优势和不足，并提供 3-5 条具体的改进建议。");

        return prompt.toString();
    }

    private void parseAiAnalysisResponse(String content, VideoCompareResultVO result) {
        try {
            String jsonStr = content.trim();
            if (jsonStr.startsWith("```json")) jsonStr = jsonStr.substring(7);
            if (jsonStr.startsWith("```")) jsonStr = jsonStr.substring(3);
            if (jsonStr.endsWith("```")) jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
            jsonStr = jsonStr.trim();

            JSONObject json = JSON.parseObject(jsonStr);
            result.setAnalysisReport(json.getString("analysis_report"));

            List<VideoCompareResultVO.Suggestion> suggestions = new ArrayList<>();
            JSONArray suggestionsArray = json.getJSONArray("suggestions");

            if (suggestionsArray != null) {
                for (int i = 0; i < suggestionsArray.size(); i++) {
                    JSONObject item = suggestionsArray.getJSONObject(i);
                    VideoCompareResultVO.Suggestion suggestion = new VideoCompareResultVO.Suggestion();
                    suggestion.setCategory(item.getString("category"));
                    suggestion.setSuggestion(item.getString("suggestion"));
                    suggestion.setPriority(item.getInteger("priority"));
                    suggestion.setReason(item.getString("reason"));
                    suggestions.add(suggestion);
                }
            }

            result.setSuggestions(suggestions);
        } catch (Exception e) {
            log.error("解析 AI 分析响应失败: {}", content, e);
            result.setAnalysisReport(content);
            result.setSuggestions(new ArrayList<>());
        }
    }
}
