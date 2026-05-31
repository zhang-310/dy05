package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase;
import cn.gaifan.douyinOperations.module.ai.repository.AiKnowledgeBaseRepository;
import cn.gaifan.douyinOperations.module.ai.service.EvolutionDashboardService;
import cn.gaifan.douyinOperations.module.ai.service.OperationalStrategyKnowledgeService;
import cn.gaifan.douyinOperations.module.douyinapi.client.DouyinApiClient;
import cn.gaifan.douyinOperations.module.douyinapi.service.OAuthTokenService;
import cn.gaifan.douyinOperations.module.douyin.entity.DouyinAccount;
import cn.gaifan.douyinOperations.module.douyin.repository.DouyinAccountRepository;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvAccountCollectTask;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvAccountCollectTaskRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvViralVideoRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.AccountVideoCollectService;
import cn.gaifan.douyinOperations.module.shortvideo.integration.VideoInsightIntegrationBridge;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectTaskSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectTaskSearchVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectTaskVO;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 账号短视频采集核心编排（两阶段模式）：
 * 阶段一：采集视频列表 -> 创建 SvViralVideo -> 状态 collected（等待用户选择）
 * 阶段二：用户选中视频 -> 深度分析 -> 入库知识库
 */
@Slf4j
@Service
public class AccountVideoCollectServiceImpl implements AccountVideoCollectService {

    private static final int ANALYSIS_POLL_INTERVAL_MS = 5000;
    private static final int ANALYSIS_MAX_WAIT_ROUNDS = 360;

    @Resource
    private SvAccountCollectTaskRepository taskRepository;

    @Resource
    private SvViralVideoRepository viralVideoRepository;

    @Autowired(required = false)
    private DouyinApiClient douyinApiClient;

    @Autowired(required = false)
    private OAuthTokenService oauthTokenService;

    @Autowired(required = false)
    private DouyinAccountRepository douyinAccountRepository;

    @Autowired(required = false)
    private AccountVideoScraper accountVideoScraper;

    @Autowired(required = false)
    private VideoInsightIntegrationBridge videoInsightIntegrationBridge;

    @Autowired(required = false)
    private EvolutionDashboardService evolutionDashboardService;

    @Autowired(required = false)
    private AiKnowledgeBaseRepository knowledgeBaseRepository;

    @Autowired(required = false)
    private VideoBreakdownKbFormatter kbFormatter;

    @Autowired(required = false)
    private OperationalStrategyKnowledgeService operationalStrategyKnowledgeService;

    @Autowired(required = false)
    private ViralPatternKnowledgeFormatter viralPatternKnowledgeFormatter;

    @Autowired(required = false)
    private cn.gaifan.douyinOperations.module.shortvideo.service.SvAccountService svAccountService;

    @Autowired
    private ObjectProvider<AccountCollectAsyncRunner> asyncRunnerProvider;

    @Autowired
    private DouyinUrlResolver douyinUrlResolver;

    @Value("${app.shortvideo.account-collect.dispatch-immediately:false}")
    private boolean dispatchImmediately;

    @Value("${app.shortvideo.account-collect.default-max-count:100}")
    private int defaultMaxCount;

    @Value("${app.shortvideo.account-collect.hard-max-count:500}")
    private int hardMaxCount;

    // ─── 阶段一：采集列表 ──────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountCollectTaskVO startCollect(AccountCollectTaskSaveVO vo, Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        String rawInput = vo.getEffectiveInput();
        if (!StringUtils.hasText(rawInput)) {
            if ("keyword_video".equalsIgnoreCase(StringUtils.trimWhitespace(vo.getCollectMode()))) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "请输入搜索关键词");
            }
            throw new BusinessException(ErrorCode.VALIDATION_FAIL,
                    "请输入抖音视频链接、账号主页链接或抖音号");
        }

        DouyinUrlResolver.ResolveResult resolved;
        if ("keyword_video".equalsIgnoreCase(StringUtils.trimWhitespace(vo.getCollectMode()))) {
            String keyword = rawInput.trim();
            if (keyword.length() > 200) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL, "搜索关键词过长（最多 200 字）");
            }
            String enc = UriUtils.encodePathSegment(keyword, StandardCharsets.UTF_8);
            String searchUrl = "https://www.douyin.com/search/" + enc + "?type=video";
            String label = keyword.length() > 80 ? keyword.substring(0, 80) + "…" : keyword;
            resolved = new DouyinUrlResolver.ResolveResult("search_video", searchUrl, "搜索:" + label, null, null);
        } else {
            resolved = douyinUrlResolver.resolve(rawInput, userId);
            if ("unknown".equals(resolved.inputType())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL,
                        "无法识别输入内容，请粘贴抖音视频链接、账号主页链接或抖音号");
            }
        }

        // AC-07: 复采防重 —— 同一账号（secUid）已有未完成/未失败的任务时拒绝重复提交
        if ("search_video".equals(resolved.inputType())) {
            String orig = rawInput.length() > 1024 ? rawInput.substring(0, 1024) : rawInput;
            boolean hasPendingSearch = taskRepository.findAll((root, query, cb) -> cb.and(
                    cb.equal(root.get("ownerId"), userId),
                    cb.equal(root.get("inputType"), "search_video"),
                    cb.equal(root.get("originalInput"), orig),
                    cb.notEqual(root.get("status"), "failed"),
                    cb.notEqual(root.get("status"), "completed"),
                    cb.equal(root.get("deleted"), 0)
            )).stream().findFirst().isPresent();
            if (hasPendingSearch) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL,
                        "该关键词已有进行中的采集任务，请等待完成后再次提交");
            }
        } else if (StringUtils.hasText(resolved.secUid())) {
            boolean hasPending = taskRepository.findAll((root, query, cb) -> cb.and(
                    cb.equal(root.get("ownerId"), userId),
                    cb.equal(root.get("secUid"), resolved.secUid()),
                    cb.notEqual(root.get("status"), "failed"),
                    cb.notEqual(root.get("status"), "completed"),
                    cb.equal(root.get("deleted"), 0)
            )).stream().findFirst().isPresent();
            if (hasPending) {
                throw new BusinessException(ErrorCode.VALIDATION_FAIL,
                        "该账号已有进行中的采集任务，请等待完成后再次提交");
            }
        }

        // 创建或查找账号记录
        cn.gaifan.douyinOperations.module.shortvideo.entity.SvAccount svAccount = null;
        if (svAccountService != null && StringUtils.hasText(resolved.secUid())) {
            String sourceType = "search_video".equals(resolved.inputType()) ? "keyword_search" : "manual";
            String sourceKeyword = "search_video".equals(resolved.inputType()) ? rawInput : null;
            svAccount = svAccountService.findOrCreateAccount(
                resolved.secUid(),
                resolved.accountName(),
                sourceType,
                sourceKeyword,
                null,  // sourceTaskId 稍后更新
                userId
            );
        }

        SvAccountCollectTask task = new SvAccountCollectTask();
        task.setOwnerId(userId);
        task.setAccountId(vo.getAccountId());
        task.setSvAccountId(svAccount != null ? svAccount.getId() : null);
        task.setOriginalInput(rawInput.length() > 1024 ? rawInput.substring(0, 1024) : rawInput);
        task.setInputType(resolved.inputType());
        task.setStatus("pending");
        task.setMaxCount(resolveMaxCount(vo.getMaxCount()));

        if (StringUtils.hasText(resolved.accountUrl())) {
            task.setAccountUrl(resolved.accountUrl());
        }
        if (StringUtils.hasText(resolved.secUid())) {
            task.setSecUid(resolved.secUid());
        }
        if (StringUtils.hasText(resolved.accountName())) {
            task.setAccountName(resolved.accountName());
        }

        if (vo.getAccountId() != null && douyinAccountRepository != null) {
            douyinAccountRepository.findById(vo.getAccountId()).ifPresent(account -> {
                if (!StringUtils.hasText(task.getAccountName())) {
                    task.setAccountName(account.getAccountName());
                }
            });
        }

        Long targetKbId = vo.getTargetKbId();
        if (targetKbId == null) {
            targetKbId = resolveDefaultKbId(userId);
        }
        task.setTargetKbId(targetKbId);

        taskRepository.save(task);
        log.info("账号采集任务已创建: taskId={}, inputType={}, accountUrl={}",
                task.getId(), resolved.inputType(), task.getAccountUrl());

        maybeDispatchImmediately(task.getId(), userId);

        return toVO(task);
    }

    // ─── 阶段二：分析用户选中的视频 ──────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountCollectTaskVO analyzeSelected(Long taskId, List<Long> viralVideoIds, Long userId) {
        SvAccountCollectTask task = taskRepository.findByIdAndDeleted(taskId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "任务不存在"));
        if (!task.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        if (!"collected".equals(task.getStatus()) && !"completed".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL,
                    "当前状态不支持分析，请等待视频列表采集完成");
        }
        if (viralVideoIds == null || viralVideoIds.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "请至少选择一个视频");
        }

        task.setStatus("analyzing");
        task.setErrorMessage(null);
        task.setAnalyzedVideos(0);
        task.setIndexedVideos(0);
        taskRepository.save(task);

        asyncRunnerProvider.getObject().runAnalyzeAsync(taskId, viralVideoIds, userId);

        return toVO(task);
    }

    // ─── 查询类接口 ──────────────────────────────────────

    @Override
    public PageResultVO<AccountCollectTaskVO> listTasks(AccountCollectTaskSearchVO vo, Long userId) {
        vo.validateParams();
        Specification<SvAccountCollectTask> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("ownerId"), userId));
            if (StringUtils.hasText(vo.getStatus())) {
                predicates.add(cb.equal(root.get("status"), vo.getStatus()));
            }
            if (StringUtils.hasText(vo.getAccountName())) {
                predicates.add(cb.like(cb.lower(root.get("accountName")),
                        "%" + vo.getAccountName().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<SvAccountCollectTask> page = taskRepository.findAll(spec,
                PageRequest.of(vo.getPage(), vo.getRows(), Sort.by(Sort.Direction.DESC, "createTime")));
        List<AccountCollectTaskVO> list = page.getContent().stream().map(this::toVO).collect(Collectors.toList());
        return PageResultVO.of(page.getTotalElements(), list, vo.getPage(), vo.getRows());
    }

    @Override
    public AccountCollectTaskVO getTaskStatus(Long taskId, Long userId) {
        SvAccountCollectTask task = taskRepository.findByIdAndDeleted(taskId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "任务不存在"));
        if (!task.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        return toVO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTask(Long taskId, Long userId) {
        SvAccountCollectTask task = taskRepository.findByIdAndDeleted(taskId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "任务不存在"));
        if (!task.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        if ("completed".equals(task.getStatus()) || "failed".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "任务已结束，无法取消");
        }
        task.setStatus("failed");
        task.setErrorMessage("用户取消");
        task.setLeaseUntil(null);
        task.setFinishedAt(new Timestamp(System.currentTimeMillis()));
        taskRepository.save(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountCollectTaskVO retryTask(Long taskId, Long userId) {
        SvAccountCollectTask task = taskRepository.findByIdAndDeleted(taskId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "任务不存在"));
        if (!task.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        if (!"failed".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "仅失败任务可重试");
        }
        task.setStatus("pending");
        task.setErrorMessage(null);
        task.setWorkerId(null);
        task.setWorkerRegion(null);
        task.setLeaseUntil(null);
        task.setClaimedAt(null);
        task.setFinishedAt(null);
        task.setNextRunAt(new Timestamp(System.currentTimeMillis()));
        task.setRetryCount(0);
        taskRepository.save(task);

        maybeDispatchImmediately(task.getId(), userId);
        return toVO(task);
    }

    @Override
    public PageResultVO<Map<String, Object>> listTaskVideos(Long taskId, Long userId, int page, int rows) {
        SvAccountCollectTask task = taskRepository.findByIdAndDeleted(taskId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "任务不存在"));
        if (!task.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
        }

        Specification<SvViralVideo> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("collectTaskId"), taskId),
                cb.equal(root.get("ownerId"), userId)
        );
        Page<SvViralVideo> videoPage = viralVideoRepository.findAll(spec,
                PageRequest.of(page, rows, Sort.by(Sort.Direction.DESC, "createTime")));

        List<Map<String, Object>> list = videoPage.getContent().stream().map(v -> {
            ViralEvidenceHelper.EvidenceSnapshot evidence = ViralEvidenceHelper.resolveEvidence(
                    v.getDeepAnalysisResult(), v.getTranscript(), v.getSceneDescriptions());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", v.getId());
            m.put("title", v.getTitle());
            m.put("coverUrl", v.getCoverUrl());
            m.put("coverBosUrl", v.getCoverBosUrl());
            m.put("videoUrl", v.getVideoUrl());
            m.put("videoBosUrl", v.getVideoBosUrl());
            m.put("authorName", v.getAuthorName());
            m.put("viewCount", v.getViewCount());
            m.put("likeCount", v.getLikeCount());
            m.put("commentCount", v.getCommentCount());
            m.put("shareCount", v.getShareCount());
            m.put("favoriteCount", v.getFavoriteCount());
            m.put("viralScore", v.getViralScore());
            m.put("remakeStatus", v.getRemakeStatus());
            m.put("videoDuration", v.getVideoDuration());
            m.put("deepAnalyzeStatus", v.getDeepAnalyzeStatus());
            m.put("publishTime", v.getPublishTime());
            m.put("transcript", truncateTranscript(v.getTranscript(), 200));
            if (evidence.hasDisclosure()) {
                m.put("evidenceLevel", evidence.overallLevel());
                m.put("transcriptEvidenceLevel", evidence.transcriptLevel());
                m.put("sceneEvidenceLevel", evidence.sceneLevel());
                m.put("transcriptLabel", transcriptDisplayLabel(evidence.transcriptLevel()));
                m.put("sceneLabel", sceneDisplayLabel(evidence.sceneLevel()));
                m.put("inferenceRisk", evidence.hasInferenceRisk());
            }
            return m;
        }).collect(Collectors.toList());

        return PageResultVO.of(videoPage.getTotalElements(), list, page, rows);
    }

    // ─── 阶段一管线（由 AsyncRunner 调用）──────────────────────────────────────

    public void runCollectPipeline(Long taskId, Long userId) {
        try {
            doRunCollectPipeline(taskId, userId);
        } catch (Exception e) {
            log.error("账号采集管线异常: taskId={}", taskId, e);
            try {
                handleCollectFailure(taskId, e);
            } catch (Exception ignored) {
                // 任务状态更新失败，已记录主异常日志
            }
        }
    }

    private void maybeDispatchImmediately(Long taskId, Long userId) {
        if (dispatchImmediately) {
            asyncRunnerProvider.getObject().runCollectAsync(taskId, userId);
        }
    }

    private void handleCollectFailure(Long taskId, Exception e) {
        String msg = truncateError(e.getMessage());
        SvAccountCollectTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return;
        }
        int retryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
        int maxRetryCount = task.getMaxRetryCount() == null ? 3 : task.getMaxRetryCount();
        if ("failed".equals(task.getStatus()) && "用户取消".equals(task.getErrorMessage())) {
            log.info("账号采集任务已被用户取消，不再重试: taskId={}", taskId);
            return;
        }
        if (isNonRetryableCollectFailure(msg)) {
            taskRepository.markFailedTerminal(taskId, truncateError("采集失败且不可自动重试: " + msg));
            log.warn("账号采集任务遇到不可重试错误，已终止: taskId={}, error={}", taskId, msg);
            return;
        }
        if (retryCount < maxRetryCount) {
            long backoffSeconds = Math.min(3600L, (long) Math.pow(2, retryCount) * 60L);
            Timestamp nextRunAt = new Timestamp(System.currentTimeMillis() + backoffSeconds * 1000L);
            taskRepository.releaseForRetry(taskId, nextRunAt,
                    truncateError("采集失败，已等待重试(" + (retryCount + 1) + "/" + maxRetryCount + "): " + msg));
            log.warn("账号采集任务将退避重试: taskId={}, retry={}/{}, nextRunAt={}",
                    taskId, retryCount + 1, maxRetryCount, nextRunAt);
            return;
        }
        taskRepository.markFailedTerminal(taskId,
                truncateError("采集失败且已达到最大重试次数(" + maxRetryCount + "): " + msg));
    }

    private boolean isNonRetryableCollectFailure(String msg) {
        if (!StringUtils.hasText(msg)) {
            return false;
        }
        return msg.contains("未找到有效抖音 Cookie")
                || msg.contains("请更新有效 Cookie")
                || msg.contains("登录/安全验证/风控页")
                || msg.contains("验证码")
                || msg.contains("安全验证")
                || msg.toLowerCase(Locale.ROOT).contains("captcha");
    }

    private void doRunCollectPipeline(Long taskId, Long userId) {
        SvAccountCollectTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            log.warn("账号采集管线跳过: taskId={} 在库中不存在（常见原因：异步早于事务提交执行，或任务已删除）", taskId);
            return;
        }

        taskRepository.updateStatus(taskId, "collecting", null);

        // video_url 输入且无 accountUrl 时：回退为单条视频采集
        if ("video_url".equals(task.getInputType()) && !StringUtils.hasText(task.getAccountUrl())) {
            handleSingleVideoFallback(task, taskId, userId);
            return;
        }

        List<VideoInfo> videoInfos = collectVideoList(task, userId);
        if (isTaskCancelled(taskId)) {
            log.info("账号采集任务已取消，停止写入结果: taskId={}", taskId);
            return;
        }
        videoInfos = applyMaxCount(videoInfos, task.getMaxCount());

        if (videoInfos.isEmpty()) {
            boolean anyChannelAvailable = isAnyCollectChannelAvailable(task, userId);
            if (anyChannelAvailable) {
                taskRepository.updateStatus(taskId, "collected", "该账号暂无公开作品");
            } else {
                taskRepository.updateStatus(taskId, "failed", "采集通道均不可用，请检查账号链接或 OAuth 授权");
            }
            return;
        }

        List<SvViralVideo> createdVideos = createViralVideos(videoInfos, taskId, userId);
        task = taskRepository.findById(taskId).orElse(null);
        if (task == null) return;
        task.setTotalVideos(videoInfos.size());
        task.setCollectedVideos(createdVideos.size());
        task.setStatus("collected");
        task.setErrorMessage(null);
        taskRepository.save(task);

        log.info("账号视频列表采集完成，等待用户选择: taskId={}, total={}", taskId, videoInfos.size());
    }

    private int resolveMaxCount(Integer requested) {
        int fallback = defaultMaxCount > 0 ? defaultMaxCount : 100;
        int hardLimit = hardMaxCount > 0 ? hardMaxCount : 500;
        int value = requested != null && requested > 0 ? requested : fallback;
        return Math.max(1, Math.min(value, hardLimit));
    }

    private List<VideoInfo> applyMaxCount(List<VideoInfo> videos, Integer maxCount) {
        if (videos == null || videos.isEmpty()) {
            return Collections.emptyList();
        }
        int limit = maxCount != null && maxCount > 0 ? Math.min(maxCount, videos.size()) : videos.size();
        if (limit >= videos.size()) {
            return videos;
        }
        return new ArrayList<>(videos.subList(0, limit));
    }

    private void handleSingleVideoFallback(SvAccountCollectTask task, Long taskId, Long userId) {
        String videoUrl = task.getOriginalInput();
        if (!StringUtils.hasText(videoUrl)) {
            taskRepository.updateStatus(taskId, "failed", "无法获取视频链接");
            return;
        }
        // 从 originalInput 中提取视频 URL
        var resolved = cn.gaifan.douyinOperations.module.shortvideo.util.DouyinSharePasteParser
                .resolveCollectVideoUrl(videoUrl);
        String url = resolved.orElse(videoUrl);

        VideoInfo vi = new VideoInfo();
        vi.videoUrl = url;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("/video/(\\d{15,25})").matcher(url);
        if (m.find()) vi.videoId = m.group(1);

        List<SvViralVideo> created = createViralVideos(List.of(vi), taskId, userId);
        task = taskRepository.findById(taskId).orElse(null);
        if (task == null) return;
        task.setTotalVideos(1);
        task.setCollectedVideos(created.size());
        task.setStatus("collected");
        task.setErrorMessage("单条视频采集（未找到作者主页，仅采集此视频）");
        taskRepository.save(task);
        log.info("单条视频回退采集完成: taskId={}, videoUrl={}", taskId, url);
    }

    // ─── 阶段二管线（由 AsyncRunner 调用）──────────────────────────────────────

    public void runAnalyzePipeline(Long taskId, List<Long> viralVideoIds, Long userId) {
        try {
            doRunAnalyzePipeline(taskId, viralVideoIds, userId);
        } catch (Exception e) {
            log.error("视频分析管线异常: taskId={}", taskId, e);
            try {
                taskRepository.updateStatus(taskId, "failed", truncateError(e.getMessage()));
            } catch (Exception ignored) {
                // 任务状态更新失败，已记录主异常日志
            }
        }
    }

    private void doRunAnalyzePipeline(Long taskId, List<Long> viralVideoIds, Long userId) {
        List<SvViralVideo> selectedVideos = viralVideoRepository.findByIdInAndDeleted(viralVideoIds, 0);
        selectedVideos = selectedVideos.stream()
                .filter(v -> userId.equals(v.getOwnerId()))
                .collect(Collectors.toList());

        if (selectedVideos.isEmpty()) {
            taskRepository.updateStatus(taskId, "completed", "无有效视频可分析");
            return;
        }

        SvAccountCollectTask task = taskRepository.findById(taskId).orElse(null);
        if (task != null) {
            task.setTotalVideos(selectedVideos.size());
            taskRepository.save(task);
        }

        analyzeVideos(selectedVideos, taskId, userId);

        if (isTaskCancelled(taskId)) return;

        taskRepository.updateStatus(taskId, "indexing", null);
        indexToKnowledgeBase(selectedVideos, taskId, userId);

        taskRepository.updateStatus(taskId, "completed", null);
        log.info("视频分析任务完成: taskId={}, analyzed={}", taskId, selectedVideos.size());

        // 更新账号统计数据
        if (svAccountService != null && task != null && task.getSvAccountId() != null) {
            try {
                svAccountService.updateAccountStatistics(task.getSvAccountId());
                log.info("账号统计已更新: accountId={}", task.getSvAccountId());
            } catch (Exception e) {
                log.warn("更新账号统计失败: accountId={}, error={}", task.getSvAccountId(), e.getMessage());
            }
        }
    }

    // ─── 采集视频列表 ──────────────────────────────────────

    private boolean isAnyCollectChannelAvailable(SvAccountCollectTask task, Long userId) {
        boolean openApiAvailable = task.getAccountId() != null
                && douyinApiClient != null && oauthTokenService != null;
        boolean scraperAvailable = accountVideoScraper != null
                && accountVideoScraper.isAvailable()
                && StringUtils.hasText(task.getAccountUrl());
        return openApiAvailable || scraperAvailable;
    }

    private List<VideoInfo> collectVideoList(SvAccountCollectTask task, Long userId) {
        List<VideoInfo> videos = new ArrayList<>();

        // 纯抖音号：提交时若未解析出主页（Playwright 瞬时失败等），在采集阶段再尝试搜索一次并写回 accountUrl
        if ("douyin_id".equals(task.getInputType()) && !StringUtils.hasText(task.getAccountUrl())
                && StringUtils.hasText(task.getOriginalInput())
                && accountVideoScraper != null && accountVideoScraper.isAvailable()) {
            String clean = task.getOriginalInput().trim().replaceAll("[\\s@#]", "");
            if (StringUtils.hasText(clean)) {
                try {
                    String home = accountVideoScraper.searchDouyinAccount(clean, userId);
                    if (StringUtils.hasText(home)) {
                        task.setAccountUrl(home);
                        String sec = accountVideoScraper.extractSecUid(home);
                        if (StringUtils.hasText(sec)) {
                            task.setSecUid(sec);
                        }
                        taskRepository.save(task);
                        log.info("采集阶段补全账号主页: taskId={}, url={}", task.getId(), home);
                    }
                } catch (Exception e) {
                    log.warn("采集阶段抖音号转主页失败: taskId={}, err={}", task.getId(), e.getMessage());
                }
            }
        }

        // 误解析为 /user/self（浏览器「我的主页」占位）时，用抖音号重新搜索真实 sec_uid 主页
        if ("douyin_id".equals(task.getInputType())
                && StringUtils.hasText(task.getAccountUrl())
                && task.getAccountUrl().contains("/user/self")
                && StringUtils.hasText(task.getOriginalInput())
                && accountVideoScraper != null && accountVideoScraper.isAvailable()) {
            String clean = task.getOriginalInput().trim().replaceAll("[\\s@#]", "");
            if (StringUtils.hasText(clean)) {
                try {
                    String home = accountVideoScraper.searchDouyinAccount(clean, userId);
                    if (StringUtils.hasText(home) && !home.contains("/user/self")) {
                        task.setAccountUrl(home);
                        String sec = accountVideoScraper.extractSecUid(home);
                        if (StringUtils.hasText(sec)) {
                            task.setSecUid(sec);
                        }
                        taskRepository.save(task);
                        log.info("采集阶段修正 /user/self 占位链接: taskId={}, url={}", task.getId(), home);
                    }
                } catch (Exception e) {
                    log.warn("采集阶段修正 self 主页失败: taskId={}, err={}", task.getId(), e.getMessage());
                }
            }
        }

        if (task.getAccountId() != null && douyinApiClient != null && oauthTokenService != null) {
            try {
                String accessToken = oauthTokenService.getValidAccessToken(userId, "douyin");
                if (StringUtils.hasText(accessToken)) {
                    String openId = null;
                    if (douyinAccountRepository != null) {
                        DouyinAccount account = douyinAccountRepository.findById(task.getAccountId()).orElse(null);
                        if (account != null) openId = account.getAccountId();
                    }
                    if (StringUtils.hasText(openId)) {
                        videos = fetchViaOpenApi(openId, accessToken);
                        if (!videos.isEmpty()) {
                            log.info("Open API 采集到 {} 条视频", videos.size());
                            return videos;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Open API 采集失败，将使用 Playwright 兜底: {}", e.getMessage());
            }
        }

        if (accountVideoScraper != null && accountVideoScraper.isAvailable()
                && StringUtils.hasText(task.getAccountUrl())) {
            try {
                AccountVideoScraper.ScrapeResult result;
                if ("search_video".equals(task.getInputType())) {
                    result = accountVideoScraper.scrapeSearchVideos(task.getAccountUrl(), userId);
                } else {
                    result = accountVideoScraper.scrapeAccountVideos(task.getAccountUrl(), userId);
                }
                if (result != null) {
                    if (StringUtils.hasText(result.getAccountName()) && !StringUtils.hasText(task.getAccountName())) {
                        task.setAccountName(result.getAccountName());
                    }
                    if (StringUtils.hasText(result.getSecUid()) && !StringUtils.hasText(task.getSecUid())) {
                        task.setSecUid(result.getSecUid());
                    }
                    taskRepository.save(task);

                    for (AccountVideoScraper.ScrapedVideo sv : result.getVideos()) {
                        VideoInfo vi = new VideoInfo();
                        vi.videoId = sv.getVideoId();
                        vi.videoUrl = sv.getVideoUrl();
                        vi.title = sv.getTitle();
                        vi.coverUrl = sv.getCoverUrl();
                        vi.viewCount = sv.getViewCount();
                        vi.likeCount = sv.getLikeCount();
                        vi.commentCount = sv.getCommentCount();
                        vi.shareCount = sv.getShareCount();
                        vi.favoriteCount = sv.getFavoriteCount();
                        videos.add(vi);
                    }
                    log.info("Playwright 采集到 {} 条视频 (inputType={})", videos.size(), task.getInputType());
                }
            } catch (Exception e) {
                log.error("Playwright 采集失败: {}", e.getMessage(), e);
                throw new BusinessException(ErrorCode.SYNC_FAILED, "Playwright 采集失败: " + e.getMessage());
            }
        }

        return videos;
    }

    private List<VideoInfo> fetchViaOpenApi(String openId, String accessToken) {
        List<VideoInfo> all = new ArrayList<>();
        long cursor = 0;
        boolean hasMore = true;
        while (hasMore) {
            DouyinApiClient.VideoListResponse resp = douyinApiClient.getVideoList(openId, accessToken, cursor, 20);
            if (resp == null || resp.videos() == null || resp.videos().isEmpty()) break;
            for (DouyinApiClient.VideoItem item : resp.videos()) {
                VideoInfo vi = new VideoInfo();
                vi.videoId = item.itemId();
                vi.videoUrl = "https://www.douyin.com/video/" + item.itemId();
                vi.title = item.title();
                vi.coverUrl = item.coverUrl();
                vi.viewCount = item.playCount();
                vi.likeCount = item.diggCount();
                vi.commentCount = item.commentCount();
                vi.shareCount = item.shareCount();
                vi.favoriteCount = item.collectCount();
                vi.duration = item.duration();
                vi.createTime = item.createTime() > 0
                        ? new Timestamp(item.createTime() * 1000L)
                        : null;
                all.add(vi);
            }
            cursor = resp.cursor();
            hasMore = resp.hasMore();
        }
        return all;
    }

    // ─── 创建 SvViralVideo ──────────────────────────────────────

    private List<SvViralVideo> createViralVideos(List<VideoInfo> videoInfos, Long taskId, Long userId) {
        // 获取任务的 svAccountId
        Long svAccountId = null;
        if (taskId != null) {
            SvAccountCollectTask task = taskRepository.findById(taskId).orElse(null);
            if (task != null) {
                svAccountId = task.getSvAccountId();
            }
        }

        List<SvViralVideo> created = new ArrayList<>();
        for (VideoInfo vi : videoInfos) {
            if (StringUtils.hasText(vi.videoId)) {
                Optional<SvViralVideo> existing = viralVideoRepository
                        .findByOwnerIdAndDouyinVideoIdAndDeleted(userId, vi.videoId, 0);
                if (existing.isPresent()) {
                    SvViralVideo ex = existing.get();
                    if (ex.getCollectTaskId() == null) {
                        ex.setCollectTaskId(taskId);
                    }
                    if (ex.getSvAccountId() == null && svAccountId != null) {
                        ex.setSvAccountId(svAccountId);
                    }
                    viralVideoRepository.save(ex);
                    created.add(ex);
                    continue;
                }
            }

            SvViralVideo viral = new SvViralVideo();
            viral.setOwnerId(userId);
            viral.setDouyinVideoId(vi.videoId);
            viral.setTitle(vi.title);
            viral.setCoverUrl(vi.coverUrl);
            viral.setVideoUrl(vi.videoUrl);
            viral.setViewCount(vi.viewCount);
            viral.setLikeCount(vi.likeCount);
            viral.setCommentCount(vi.commentCount);
            viral.setShareCount(vi.shareCount);
            viral.setFavoriteCount(vi.favoriteCount);
            viral.setVideoDuration(vi.duration);
            viral.setPublishTime(vi.createTime);
            viral.setAutoCollected(true);
            viral.setCollectSource("account_collect");
            viral.setCollectTaskId(taskId);
            viral.setSvAccountId(svAccountId);  // ← 关联账号
            viralVideoRepository.save(viral);
            created.add(viral);
        }
        return created;
    }

    // ─── 深度分析 ──────────────────────────────────────

    /**
     * AC-08: 改为"触发即计入"模式，消除 30 分钟同步等待阻塞。
     * 每条视频发起 startDeepAnalyze 后立即计入 analyzedVideos，
     * 实际分析结果可通过爆款库 /viral/deep-analyze/status 或 SSE 查看。
     */
    private void analyzeVideos(List<SvViralVideo> videos, Long taskId, Long userId) {
        if (videoInsightIntegrationBridge == null) {
            log.warn("VideoInsightIntegrationBridge 不可用，跳过深度分析");
            return;
        }
        for (SvViralVideo viral : videos) {
            if (isTaskCancelled(taskId)) {
                log.info("任务已被取消，停止分析: taskId={}", taskId);
                return;
            }
            if ("completed".equals(viral.getDeepAnalyzeStatus())) {
                taskRepository.incrementAnalyzedVideos(taskId);
                continue;
            }
            try {
                videoInsightIntegrationBridge.requestDeepAnalyzeFromShortvideo(
                        viral.getId(), userId, null, "collect-task-" + taskId + "-" + viral.getId());
                // 触发即计入，无需阻塞等待完成
                taskRepository.incrementAnalyzedVideos(taskId);
                log.debug("已发起深度分析 viralId={}", viral.getId());
            } catch (Exception e) {
                log.warn("深度分析启动失败 viralId={}: {}", viral.getId(), e.getMessage());
            }
        }
    }

    @SuppressWarnings("unused")
    private void waitForAnalysisComplete(Long viralId, Long taskId) {
        // 保留兼容性，已不在主流程调用
        for (int i = 0; i < ANALYSIS_MAX_WAIT_ROUNDS; i++) {
            try {
                Thread.sleep(ANALYSIS_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (isTaskCancelled(taskId)) return;
            SvViralVideo viral = viralVideoRepository.findById(viralId).orElse(null);
            if (viral == null) return;
            String st = viral.getDeepAnalyzeStatus();
            if ("completed".equals(st) || "failed".equals(st)) {
                return;
            }
        }
        log.warn("深度分析等待超时({}分钟) viralId={}",
                (ANALYSIS_POLL_INTERVAL_MS * ANALYSIS_MAX_WAIT_ROUNDS) / 60000, viralId);
    }

    // ─── 入库知识库 ──────────────────────────────────────

    private void indexToKnowledgeBase(List<SvViralVideo> videos, Long taskId, Long userId) {
        if (evolutionDashboardService == null || kbFormatter == null) {
            log.warn("知识库入库组件不可用，跳过入库");
            return;
        }
        SvAccountCollectTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null) return;
        Long targetKbId = task.getTargetKbId();
        if (targetKbId == null) {
            targetKbId = resolveDefaultKbId(userId);
            if (targetKbId == null) {
                log.warn("未找到目标知识库，跳过入库 taskId={}", taskId);
                return;
            }
        }
        for (SvViralVideo viral : videos) {
            SvViralVideo latest = viralVideoRepository.findById(viral.getId()).orElse(null);
            if (latest == null) continue;
            if (!StringUtils.hasText(latest.getTranscript()) && !StringUtils.hasText(latest.getDeepAnalysisResult())) {
                continue;
            }
            try {
                String content = kbFormatter.format(latest);
                evolutionDashboardService.enqueueIndex(
                        "account_video_breakdown", latest.getId(), content, 3, targetKbId);
                writeViralPatternKnowledge(latest, content, userId);
                taskRepository.incrementIndexedVideos(taskId);
                log.debug("视频拆解已入队知识库: viralId={}", latest.getId());
            } catch (Exception e) {
                log.warn("入库知识库失败 viralId={}: {}", latest.getId(), e.getMessage());
            }
        }
    }

    private void writeViralPatternKnowledge(SvViralVideo viral, String breakdownContent, Long userId) {
        if (operationalStrategyKnowledgeService == null || viralPatternKnowledgeFormatter == null
                || viral == null || userId == null || userId <= 0
                || !StringUtils.hasText(breakdownContent)) {
            return;
        }
        try {
            ViralPatternKnowledgeFormatter.PatternDocument doc = viralPatternKnowledgeFormatter.build(
                    viral, "account_collect", breakdownContent);
            operationalStrategyKnowledgeService.writeViralPatternKnowledge(
                    userId,
                    doc.title(),
                    doc.content(),
                    doc.metadata()
            );
        } catch (Exception e) {
            log.debug("爆款模式知识库沉淀跳过 viralId={}, err={}", viral.getId(), e.getMessage());
        }
    }

    // ─── 工具方法 ──────────────────────────────────────

    private boolean isTaskCancelled(Long taskId) {
        SvAccountCollectTask current = taskRepository.findById(taskId).orElse(null);
        return current != null && "failed".equals(current.getStatus());
    }

    private Long resolveDefaultKbId(Long userId) {
        if (knowledgeBaseRepository == null) return null;
        List<AiKnowledgeBase> kbs = knowledgeBaseRepository
                .findByUserIdAndDeletedOrderByCreateTimeDesc(userId, 0);
        for (AiKnowledgeBase kb : kbs) {
            if ("huashu".equals(kb.getKbName()) || "huashu".equals(kb.getKbType())) {
                return kb.getId();
            }
        }
        return kbs.isEmpty() ? null : kbs.get(0).getId();
    }

    private AccountCollectTaskVO toVO(SvAccountCollectTask task) {
        AccountCollectTaskVO vo = new AccountCollectTaskVO();
        BeanUtils.copyProperties(task, vo);
        return vo;
    }

    private static String truncateError(String msg) {
        if (msg == null) return null;
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }

    private static String truncateTranscript(String transcript, int maxLen) {
        if (transcript == null) return null;
        if (transcript.length() <= maxLen) return transcript;
        return transcript.substring(0, maxLen) + "...";
    }

    private String transcriptDisplayLabel(String level) {
        return switch (ViralEvidenceHelper.normalizeEvidenceLevel(level)) {
            case "empirical" -> "实证转写";
            case "inferred" -> "推演口播稿（非 ASR 实录）";
            case "missing" -> "缺失";
            default -> "口播文案";
        };
    }

    private String sceneDisplayLabel(String level) {
        return switch (ViralEvidenceHelper.normalizeEvidenceLevel(level)) {
            case "empirical" -> "实证场景拆解";
            case "inferred" -> "推演场景（非真实抽帧）";
            case "missing" -> "缺失";
            default -> "场景描述";
        };
    }

    static class VideoInfo {
        String videoId;
        String videoUrl;
        String title;
        String coverUrl;
        Long viewCount;
        Long likeCount;
        Long commentCount;
        Long shareCount;
        Long favoriteCount;
        Integer duration;
        Timestamp createTime;
    }

    // ─── AC-06: 删除任务 ──────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long taskId, Long userId) {
        SvAccountCollectTask task = taskRepository.findByIdAndDeleted(taskId, 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "任务不存在"));
        if (!task.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        if ("collecting".equals(task.getStatus()) || "analyzing".equals(task.getStatus())
                || "indexing".equals(task.getStatus()) || "pending".equals(task.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "进行中的任务无法删除，请先取消");
        }
        task.setDeleted(1);
        taskRepository.save(task);
        log.info("账号采集任务已删除: taskId={}, userId={}", taskId, userId);
    }
}
