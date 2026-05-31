package cn.gaifan.douyinOperations.module.tianapi.service.impl;

import cn.gaifan.douyinOperations.module.copy.entity.CopyLibrary;
import cn.gaifan.douyinOperations.module.copy.repository.CopyLibraryRepository;
import cn.gaifan.douyinOperations.module.tianapi.entity.TianApiImportCategoryStat;
import cn.gaifan.douyinOperations.module.tianapi.entity.TianApiImportRun;
import cn.gaifan.douyinOperations.module.tianapi.config.TianApiProperties;
import cn.gaifan.douyinOperations.module.tianapi.exception.TianApiQuotaExceededException;
import cn.gaifan.douyinOperations.module.tianapi.exception.TianApiUnavailableException;
import cn.gaifan.douyinOperations.module.tianapi.repository.TianApiImportCategoryStatRepository;
import cn.gaifan.douyinOperations.module.tianapi.repository.TianApiImportRunRepository;
import cn.gaifan.douyinOperations.module.tianapi.service.TianApiMaterialImportService;
import cn.gaifan.douyinOperations.module.tianapi.service.TianApiService;
import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.ai.util.ContentFingerprintUtil;
import jakarta.annotation.Resource;
import org.springframework.data.domain.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TianAPI 文案素材自动入库实现
 * 支持同步入库到文案库（copy_library）和 AI 知识库（可选）
 * 配额说明：每类每日限制约 1 万次，类目总数 × 1万 = 账号当日总可用次数。某类触发 code 150 时，仅跳过该分类，继续采集其他类目。
 */
@Service
public class TianApiMaterialImportServiceImpl implements TianApiMaterialImportService {

    private static final Logger log = LoggerFactory.getLogger(TianApiMaterialImportServiceImpl.class);
    private static final int TITLE_MAX_LEN = 80;
    private static final String SOURCE_TYPE_TIANAPI = "tianapi";
    private static final long STALE_RUNNING_INACTIVITY_MS = 2L * 60 * 60 * 1000;
    private static final long STALE_RUNNING_MAX_AGE_MS = 18L * 60 * 60 * 1000;

    @Resource
    private TianApiService tianApiService;
    @Resource
    private CopyLibraryRepository copyLibraryRepository;
    @Resource
    private TianApiProperties properties;
    @Resource
    private TianApiImportRunRepository importRunRepository;
    @Resource
    private TianApiImportCategoryStatRepository categoryStatRepository;
    @Resource
    private AiKbDocumentRepository aiKbDocumentRepository;
    @Autowired(required = false)
    private KnowledgeBaseService knowledgeBaseService;

    @Override
    public synchronized Map<String, Object> runImport() {
        Optional<TianApiImportRun> latest = importRunRepository.findTopByOrderByCreateTimeDesc();
        if (latest.isPresent() && "running".equalsIgnoreCase(latest.get().getStatus())) {
            TianApiImportRun running = latest.get();
            if (isStaleRunning(running)) {
                recoverStaleRunningRun(running);
            } else {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("skipped", true);
                result.put("skipReason", "TianAPI 素材入库已有任务运行中");
                result.put("runningRunId", running.getId());
                result.put("startedAt", running.getStartedAt());
                result.put("lastProgressAt", running.getUpdateTime());
                result.put("totalCalls", running.getTotalCalls());
                result.put("totalImported", running.getTotalImported());
                log.warn("TianAPI 素材入库跳过：已有运行中任务 runId={}", running.getId());
                return result;
            }
        }
        TianApiImportRun run = beginRun("manual_or_scheduled");
        Map<String, Object> result = new HashMap<>();
        result.put("totalImported", 0);
        result.put("totalSkipped", 0);
        result.put("totalCalls", 0);
        result.put("totalKbImported", 0);
        result.put("kbImportEnabled", properties.isKbImportEnabled());
        result.put("kbName", properties.getKbName());
        result.put("byCategory", new HashMap<String, Map<String, Integer>>());

        String skipReason = resolveSkipReason();
        if (skipReason != null) {
            result.put("skipped", true);
            result.put("skipReason", skipReason);
            finishSkipped(run, skipReason);
            log.warn("TianAPI 素材入库跳过: {}", skipReason);
            return result;
        }

        try {
            Long userId = properties.getMaterialImportUserId();
            int delayMs = Math.max(50, Math.min(2000, properties.getMaterialImportDelayMs()));
            Integer apiCap = properties.getMaterialImportApiCallsPerCategory();
            final int batchHttpLoops;
            final int singleHttpLoops;
            if (apiCap != null && apiCap > 0) {
                int v = Math.max(1, Math.min(10_000, apiCap));
                batchHttpLoops = v;
                singleHttpLoops = v;
                result.put("mode", "apiCallsPerCategory");
                result.put("configuredApiCallsPerCategory", v);
                run.setMode("apiCallsPerCategory");
                run.setConfiguredApiCallsPerCategory(v);
                log.info("TianAPI 素材入库使用「按类 HTTP 次数」模式: 每类最多 {} 次请求（批量/单条一致）", v);
            } else {
                int c = Math.max(1, Math.min(10_000, properties.getMaterialImportCallsPerCategory()));
                batchHttpLoops = Math.max(1, (c + 9) / 10);
                singleHttpLoops = Math.max(1, c);
                result.put("mode", "legacyItemsPerCategory");
                result.put("configuredCallsPerCategory", c);
                run.setMode("legacyItemsPerCategory");
                run.setConfiguredCallsPerCategory(c);
            }
            int estimatedMaxHttpCalls = (8 * batchHttpLoops) + (21 * singleHttpLoops);
            result.put("estimatedMaxHttpCalls", estimatedMaxHttpCalls);
            run.setEstimatedMaxHttpCalls(estimatedMaxHttpCalls);
            importRunRepository.save(run);

            AtomicInteger totalImported = new AtomicInteger(0);
            AtomicInteger totalSkipped = new AtomicInteger(0);
            AtomicInteger totalCalls = new AtomicInteger(0);
            AtomicInteger totalKbImported = new AtomicInteger(0);

            // 先跑批量类（每次 10 条，API 利用率高），再跑单条类，避免单条类先耗尽配额导致批量类无机会
            Map<String, Integer> godreplyStat = runGodReplyCategory(run.getId(), userId, batchHttpLoops, delayMs, totalImported, totalSkipped, totalCalls, totalKbImported);
            ((Map<String, Map<String, Integer>>) result.get("byCategory")).put("tianapi_godreply", godreplyStat);

            Map<String, Integer> hotwordStat = runHotWordCategory(run.getId(), userId, batchHttpLoops, delayMs, totalImported, totalSkipped, totalCalls, totalKbImported);
            ((Map<String, Map<String, Integer>>) result.get("byCategory")).put("tianapi_hotword", hotwordStat);

            Map<String, Integer> dictumStat = runDictumCategory(run.getId(), userId, batchHttpLoops, delayMs, totalImported, totalSkipped, totalCalls, totalKbImported);
            ((Map<String, Map<String, Integer>>) result.get("byCategory")).put("tianapi_dictum", dictumStat);

            Map<String, Integer> mingyanStat = runMingyanCategory(run.getId(), userId, batchHttpLoops, delayMs, totalImported, totalSkipped, totalCalls, totalKbImported);
            ((Map<String, Map<String, Integer>>) result.get("byCategory")).put("tianapi_mingyan", mingyanStat);

            Map<String, Integer> jokeStat = runJokeCategory(run.getId(), userId, batchHttpLoops, delayMs, totalImported, totalSkipped, totalCalls, totalKbImported);
            ((Map<String, Map<String, Integer>>) result.get("byCategory")).put("tianapi_joke", jokeStat);

            Map<String, Integer> xiehouStat = runXiehouyuCategory(run.getId(), userId, batchHttpLoops, delayMs, totalImported, totalSkipped, totalCalls, totalKbImported);
            ((Map<String, Map<String, Integer>>) result.get("byCategory")).put("tianapi_xiehouyu", xiehouStat);

            Map<String, Integer> msdlStat = runMsDuilianCategory(run.getId(), userId, batchHttpLoops, delayMs, totalImported, totalSkipped, totalCalls, totalKbImported);
            ((Map<String, Map<String, Integer>>) result.get("byCategory")).put("tianapi_msdl", msdlStat);

            Map<String, Integer> flmjStat = runFlMingjuCategory(run.getId(), userId, batchHttpLoops, delayMs, totalImported, totalSkipped, totalCalls, totalKbImported);
            ((Map<String, Map<String, Integer>>) result.get("byCategory")).put("tianapi_flmj", flmjStat);

        // 单条类：每次调用 1 条，限制次数避免耗尽配额
        List<ContentFetcher> singleFetchers = List.of(
                new ContentFetcher("tianapi_pyqwenan", "朋友圈文案", () -> tianApiService.pyqWenan(), 1),
                new ContentFetcher("tianapi_dgryl", "打工人语录", () -> tianApiService.dagongrenYulu(), 1),
                new ContentFetcher("tianapi_saylor", "土味情话", () -> tianApiService.tuweiQinghua(), 1),
                new ContentFetcher("tianapi_dujitang", "毒鸡汤", () -> tianApiService.duJitang(), 1),
                new ContentFetcher("tianapi_caihongpi", "彩虹屁", () -> tianApiService.caihongPi(), 1),
                new ContentFetcher("tianapi_zhanan", "渣男语录", () -> tianApiService.zhananYulu(), 1),
                new ContentFetcher("tianapi_zaoan", "早安心语", () -> tianApiService.zaoAnXinyu(), 1),
                new ContentFetcher("tianapi_wanan", "晚安心语", () -> tianApiService.wanAnXinyu(), 1),
                new ContentFetcher("tianapi_tiangou", "舔狗日记", () -> tianApiService.tiangouRiji(), 1),
                new ContentFetcher("tianapi_moodpoetry", "情绪诗句", () -> tianApiService.moodPoetry(), 1),
                new ContentFetcher("tianapi_zmsc", "最美宋词", () -> tianApiService.zuiMeiSongci(), 1),
                new ContentFetcher("tianapi_gjmj", "古籍名句", () -> tianApiService.guJiMingju(), 1),
                new ContentFetcher("tianapi_lzmy", "励志古言", () -> tianApiService.liZhiGuyan(), 1),
                new ContentFetcher("tianapi_hotreview", "云音乐热评", () -> tianApiService.hotReview(), 1),
                new ContentFetcher("tianapi_mnpara", "小段子", () -> tianApiService.xiaoDuanzi(), 1),
                new ContentFetcher("tianapi_skl", "顺口溜", () -> tianApiService.shunKouliu(), 1),
                new ContentFetcher("tianapi_sentence", "精美句子", () -> tianApiService.jingMeiJuzi(), 1),
                new ContentFetcher("tianapi_qingshi", "古代情诗", () -> tianApiService.guDaiQingshi(), 1),
                new ContentFetcher("tianapi_hsjz", "失恋分手", () -> tianApiService.shiLianFenshou(), 1),
                new ContentFetcher("tianapi_raokouling", "绕口令", () -> tianApiService.raoKouling(), 1)
        );

            for (ContentFetcher f : singleFetchers) {
                Map<String, Integer> stat = runSingleCategory(run.getId(), userId, f, singleHttpLoops, delayMs, totalImported, totalSkipped, totalCalls, totalKbImported);
                ((Map<String, Map<String, Integer>>) result.get("byCategory")).put(f.category, stat);
            }

            // 经典台词：每次 1 条，同单条类
            Map<String, Integer> dialogueStat = runDialogueCategory(run.getId(), userId, singleHttpLoops, delayMs, totalImported, totalSkipped, totalCalls, totalKbImported);
            ((Map<String, Map<String, Integer>>) result.get("byCategory")).put("tianapi_dialogue", dialogueStat);

            result.put("totalImported", totalImported.get());
            result.put("totalSkipped", totalSkipped.get());
            result.put("totalCalls", totalCalls.get());
            result.put("totalKbImported", totalKbImported.get());
            finishRun(run, "completed", null, totalImported.get(), totalSkipped.get(), totalCalls.get(), totalKbImported.get());

            if (totalImported.get() == 0 && totalCalls.get() == 0) {
                result.put("degraded", true);
                result.put("degradeReason", "TianAPI 未返回可入库数据，请确认 API Key 与接口权限");
                log.info("TianAPI 素材入库未获得真实数据，未写入演示样本");
            } else {
                log.info("TianAPI 素材入库完成: imported={}, skipped={}, calls={}, kbImported={}", totalImported.get(), totalSkipped.get(), totalCalls.get(), totalKbImported.get());
            }
            result.put("runId", run.getId());
            return result;
        } catch (Exception e) {
            finishRunWithCurrentProgress(run, "failed", e.getMessage());
            throw e;
        }
    }

    @Override
    public Map<String, Object> latestStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        Optional<TianApiImportRun> latest = importRunRepository.findTopByOrderByCreateTimeDesc();
        if (latest.isEmpty()) {
            result.put("hasRun", false);
            result.put("reason", "尚无 TianAPI 素材导入记录");
            return result;
        }
        TianApiImportRun run = latest.get();
        result.put("hasRun", true);
        result.put("runId", run.getId());
        result.put("status", run.getStatus());
        result.put("mode", run.getMode());
        result.put("configuredCallsPerCategory", run.getConfiguredCallsPerCategory());
        result.put("configuredApiCallsPerCategory", run.getConfiguredApiCallsPerCategory());
        result.put("estimatedMaxHttpCalls", run.getEstimatedMaxHttpCalls());
        result.put("totalImported", run.getTotalImported());
        result.put("totalSkipped", run.getTotalSkipped());
        result.put("totalCalls", run.getTotalCalls());
        result.put("totalKbImported", run.getTotalKbImported());
        if (run.getEstimatedMaxHttpCalls() != null && run.getEstimatedMaxHttpCalls() > 0) {
            result.put("progressPercent", Math.min(100, Math.round(run.getTotalCalls() * 100.0 / run.getEstimatedMaxHttpCalls())));
        }
        result.put("skipReason", run.getSkipReason());
        result.put("errorMessage", run.getErrorMessage());
        result.put("startedAt", run.getStartedAt());
        result.put("finishedAt", run.getFinishedAt());
        List<Map<String, Object>> categories = new ArrayList<>();
        for (TianApiImportCategoryStat stat : categoryStatRepository.findByRunIdOrderByIdAsc(run.getId())) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("category", stat.getCategory());
            item.put("displayName", stat.getDisplayName());
            item.put("plannedCalls", stat.getPlannedCalls());
            item.put("calls", stat.getCalls());
            item.put("imported", stat.getImported());
            item.put("skipped", stat.getSkipped());
            item.put("emptyResponses", stat.getEmptyResponses());
            item.put("failedCalls", stat.getFailedCalls());
            item.put("quotaExhausted", stat.getQuotaExhausted());
            item.put("stopReason", stat.getStopReason());
            item.put("lastError", stat.getLastError());
            categories.add(item);
        }
        result.put("categories", categories);
        result.put("completedCategories", categories.size());
        return result;
    }

    @Override
    public synchronized Map<String, Object> backfillKnowledgeBase(int limit, Long afterId) {
        Map<String, Object> result = new LinkedHashMap<>();
        int cappedLimit = Math.max(1, Math.min(2000, limit));
        long startAfterId = afterId != null && afterId > 0 ? afterId : 0L;
        result.put("limit", cappedLimit);
        result.put("afterId", startAfterId);
        result.put("kbImportEnabled", properties.isKbImportEnabled());
        result.put("kbName", properties.getKbName());
        result.put("totalCandidates", 0);
        result.put("imported", 0);
        result.put("skipped", 0);
        result.put("failed", 0);

        String skipReason = resolveKbBackfillSkipReason();
        if (skipReason != null) {
            result.put("skippedRun", true);
            result.put("skipReason", skipReason);
            return result;
        }

        Long userId = properties.getMaterialImportUserId();
        Long kbId = knowledgeBaseService.resolveKbIdByName(userId, properties.getKbName());
        if (kbId == null) {
            result.put("skippedRun", true);
            result.put("skipReason", "目标知识库不存在: " + properties.getKbName());
            return result;
        }
        result.put("kbId", kbId);

        int imported = 0;
        int skipped = 0;
        int failed = 0;
        int processed = 0;
        long cursor = startAfterId;
        Long lastScannedId = null;
        while (processed < cappedLimit) {
            List<CopyLibrary> copies = copyLibraryRepository.findTianApiCopiesAfterId(
                    userId, cursor, PageRequest.of(0, Math.min(500, cappedLimit - processed)));
            if (copies.isEmpty()) {
                break;
            }
            for (CopyLibrary copy : copies) {
                if (processed >= cappedLimit) {
                    break;
                }
                processed++;
                cursor = copy.getId();
                lastScannedId = copy.getId();
                result.put("totalCandidates", ((Integer) result.get("totalCandidates")) + 1);
                if (!StringUtils.hasText(copy.getContent())) {
                    skipped++;
                    continue;
                }
                String content = buildKbContent(copy);
                String title = buildKbTitle(copy);
                String fingerprint = ContentFingerprintUtil.compute(content);
                if (fingerprint != null && aiKbDocumentRepository.existsByKbIdAndContentFingerprintAndDeleted(kbId, fingerprint, 0)) {
                    skipped++;
                    continue;
                }
                if (aiKbDocumentRepository.existsByKbIdAndTitleAndDeleted(kbId, title, 0)) {
                    skipped++;
                    continue;
                }
                try {
                    knowledgeBaseService.uploadDocument(
                            kbId,
                            title,
                            content,
                            "md",
                            userId,
                            SOURCE_TYPE_TIANAPI,
                            copy.getCategory(),
                            buildKbMetadata(copy));
                    imported++;
                } catch (Exception e) {
                    if (isDuplicateOrTooShortUploadFailure(e)) {
                        skipped++;
                        continue;
                    }
                    failed++;
                    log.warn("TianAPI 历史素材补同步知识库失败: copyId={}, err={}", copy.getId(), e.getMessage());
                }
            }
        }

        result.put("imported", imported);
        result.put("skipped", skipped);
        result.put("failed", failed);
        result.put("processed", processed);
        result.put("lastScannedCopyId", lastScannedId);
        return result;
    }

    private TianApiImportRun beginRun(String triggerType) {
        TianApiImportRun run = new TianApiImportRun();
        run.setTriggerType(triggerType);
        run.setStatus("running");
        return importRunRepository.save(run);
    }

    private void finishSkipped(TianApiImportRun run, String skipReason) {
        run.setStatus("skipped");
        run.setSkipReason(skipReason);
        run.setFinishedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        importRunRepository.save(run);
    }

    private void finishRun(TianApiImportRun run, String status, String errorMessage,
                           int imported, int skipped, int calls, int kbImported) {
        run.setStatus(status);
        run.setErrorMessage(errorMessage);
        run.setTotalImported(imported);
        run.setTotalSkipped(skipped);
        run.setTotalCalls(calls);
        run.setTotalKbImported(kbImported);
        run.setFinishedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        importRunRepository.save(run);
    }

    private void finishRunWithCurrentProgress(TianApiImportRun run, String status, String errorMessage) {
        ProgressSnapshot snapshot = calculateProgressSnapshot(run.getId());
        finishRun(run, status, errorMessage, snapshot.imported(), snapshot.skipped(), snapshot.calls(), run.getTotalKbImported());
    }

    private void saveCategoryStat(Long runId, String category, String displayName, int plannedCalls,
                                  int calls, int imported, int skipped, int emptyResponses,
                                  int failedCalls, boolean quotaExhausted, String stopReason, String lastError) {
        if (runId == null) {
            return;
        }
        TianApiImportCategoryStat stat = new TianApiImportCategoryStat();
        stat.setRunId(runId);
        stat.setCategory(category);
        stat.setDisplayName(displayName);
        stat.setPlannedCalls(plannedCalls);
        stat.setCalls(calls);
        stat.setImported(imported);
        stat.setSkipped(skipped);
        stat.setEmptyResponses(emptyResponses);
        stat.setFailedCalls(failedCalls);
        stat.setQuotaExhausted(quotaExhausted);
        stat.setStopReason(stopReason);
        stat.setLastError(lastError);
        categoryStatRepository.save(stat);
    }

    private void updateRunProgress(Long runId) {
        if (runId == null) {
            return;
        }
        try {
            Optional<TianApiImportRun> runOpt = importRunRepository.findById(runId);
            if (runOpt.isEmpty()) {
                return;
            }
            ProgressSnapshot snapshot = calculateProgressSnapshot(runId);
            TianApiImportRun run = runOpt.get();
            run.setTotalImported(snapshot.imported());
            run.setTotalSkipped(snapshot.skipped());
            run.setTotalCalls(snapshot.calls());
            importRunRepository.save(run);
        } catch (Exception e) {
            log.debug("TianAPI 导入进度刷新失败: runId={}, err={}", runId, e.getMessage());
        }
    }

    private boolean isStaleRunning(TianApiImportRun run) {
        long now = System.currentTimeMillis();
        long lastProgressAt = timestampMillis(run.getUpdateTime(), timestampMillis(run.getStartedAt(), timestampMillis(run.getCreateTime(), now)));
        long startedAt = timestampMillis(run.getStartedAt(), timestampMillis(run.getCreateTime(), now));
        return now - lastProgressAt > STALE_RUNNING_INACTIVITY_MS || now - startedAt > STALE_RUNNING_MAX_AGE_MS;
    }

    private void recoverStaleRunningRun(TianApiImportRun run) {
        ProgressSnapshot snapshot = calculateProgressSnapshot(run.getId());
        run.setStatus("failed");
        run.setTotalImported(snapshot.imported());
        run.setTotalSkipped(snapshot.skipped());
        run.setTotalCalls(snapshot.calls());
        run.setErrorMessage("检测到陈旧 running 任务，已按已有分类统计自动收口，允许下一轮 TianAPI 采集继续执行");
        run.setFinishedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        importRunRepository.save(run);
        log.warn("TianAPI 陈旧运行任务已自动收口: runId={}, calls={}, imported={}, skipped={}",
                run.getId(), snapshot.calls(), snapshot.imported(), snapshot.skipped());
    }

    private ProgressSnapshot calculateProgressSnapshot(Long runId) {
        int totalImported = 0;
        int totalSkipped = 0;
        int totalCalls = 0;
        if (runId != null) {
            for (TianApiImportCategoryStat stat : categoryStatRepository.findByRunIdOrderByIdAsc(runId)) {
                totalImported += stat.getImported() != null ? stat.getImported() : 0;
                totalSkipped += stat.getSkipped() != null ? stat.getSkipped() : 0;
                totalCalls += stat.getCalls() != null ? stat.getCalls() : 0;
            }
        }
        return new ProgressSnapshot(totalImported, totalSkipped, totalCalls);
    }

    private static long timestampMillis(java.sql.Timestamp timestamp, long fallback) {
        return timestamp != null ? timestamp.getTime() : fallback;
    }

    private String resolveSkipReason() {
        if (properties == null || !properties.isEnabled()) {
            return "tianapi.enabled=false";
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            return "TIANAPI_API_KEY 未配置";
        }
        if (!properties.isMaterialImportEnabled()) {
            return "tianapi.material-import-enabled=false";
        }
        if (properties.getMaterialImportUserId() == null || properties.getMaterialImportUserId() <= 0) {
            return "TIANAPI_MATERIAL_IMPORT_USER_ID 未配置";
        }
        return null;
    }

    private String resolveKbBackfillSkipReason() {
        String base = resolveSkipReason();
        if (base != null) {
            return base;
        }
        if (!properties.isKbImportEnabled()) {
            return "tianapi.kb-import-enabled=false";
        }
        if (knowledgeBaseService == null) {
            return "KnowledgeBaseService 不可用";
        }
        if (!StringUtils.hasText(properties.getKbName())) {
            return "TIANAPI_KB_NAME 未配置";
        }
        return null;
    }

    private String buildKbTitle(CopyLibrary copy) {
        String display = displayNameFromCategory(copy.getCategory());
        String title = StringUtils.hasText(copy.getTitle()) ? copy.getTitle() : copy.getContent();
        return truncate(display + ": " + title, TITLE_MAX_LEN);
    }

    private String buildKbContent(CopyLibrary copy) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(buildKbTitle(copy)).append("\n\n");
        sb.append(copy.getContent() != null ? copy.getContent() : "");
        return sb.toString();
    }

    private Map<String, String> buildKbMetadata(CopyLibrary copy) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", SOURCE_TYPE_TIANAPI);
        metadata.put("source_type", SOURCE_TYPE_TIANAPI);
        metadata.put("source_copy_id", String.valueOf(copy.getId()));
        metadata.put("category", copy.getCategory() != null ? copy.getCategory() : "");
        metadata.put("display_name", displayNameFromCategory(copy.getCategory()));
        return metadata;
    }

    private boolean isDuplicateOrTooShortUploadFailure(Exception e) {
        String message = e.getMessage();
        if (!StringUtils.hasText(message)) {
            return false;
        }
        return message.contains("无可索引分块") || message.contains("chunk 级去重全部被跳过") || message.contains("内容过短");
    }

    private String displayNameFromCategory(String category) {
        if (category == null) {
            return "TianAPI素材";
        }
        return switch (category) {
            case "tianapi_godreply" -> "神回复";
            case "tianapi_hotword" -> "网络流行语";
            case "tianapi_dictum" -> "名言警句";
            case "tianapi_mingyan" -> "名人名言";
            case "tianapi_joke" -> "雷人笑话";
            case "tianapi_xiehouyu" -> "歇后语";
            case "tianapi_msdl" -> "民俗对联";
            case "tianapi_flmj" -> "分类名句";
            case "tianapi_pyqwenan" -> "朋友圈文案";
            case "tianapi_dgryl" -> "打工人语录";
            case "tianapi_saylor" -> "土味情话";
            case "tianapi_dujitang" -> "毒鸡汤";
            case "tianapi_caihongpi" -> "彩虹屁";
            case "tianapi_zhanan" -> "渣男语录";
            case "tianapi_zaoan" -> "早安心语";
            case "tianapi_wanan" -> "晚安心语";
            case "tianapi_tiangou" -> "舔狗日记";
            case "tianapi_moodpoetry" -> "情绪诗句";
            case "tianapi_zmsc" -> "最美宋词";
            case "tianapi_gjmj" -> "古籍名句";
            case "tianapi_lzmy" -> "励志古言";
            case "tianapi_hotreview" -> "云音乐热评";
            case "tianapi_mnpara" -> "小段子";
            case "tianapi_skl" -> "顺口溜";
            case "tianapi_sentence" -> "精美句子";
            case "tianapi_qingshi" -> "古代情诗";
            case "tianapi_hsjz" -> "失恋分手";
            case "tianapi_raokouling" -> "绕口令";
            case "tianapi_dialogue" -> "经典台词";
            default -> category;
        };
    }

    private Map<String, Integer> runSingleCategory(Long runId, Long userId, ContentFetcher fetcher, int numHttpCalls,
                                                   int delayMs, AtomicInteger totalImported, AtomicInteger totalSkipped, AtomicInteger totalCalls,
                                                   AtomicInteger totalKbImported) {
        AtomicInteger imported = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger calls = new AtomicInteger(0);
        AtomicInteger emptyResponses = new AtomicInteger(0);
        AtomicInteger failedCalls = new AtomicInteger(0);
        boolean quotaExhausted = false;
        String stopReason = "planned_calls_completed";
        String lastError = null;

        for (int i = 0; i < numHttpCalls; i++) {
            sleep(delayMs);
            try {
                calls.incrementAndGet();
                String content = fetcher.fetch().get();
                if (!StringUtils.hasText(content)) {
                    emptyResponses.incrementAndGet();
                    continue;
                }

                if (copyLibraryRepository.countByUserIdAndCategoryAndContentAndDeleted(userId, fetcher.category, content) > 0) {
                    skipped.incrementAndGet();
                    continue;
                }
                saveCopy(userId, fetcher.category, fetcher.displayName, content, content, totalKbImported);
                imported.incrementAndGet();
            } catch (TianApiQuotaExceededException e) {
                quotaExhausted = true;
                stopReason = "quota_exhausted";
                lastError = e.getMessage();
                log.info("TianAPI 分类 {} 今日配额已用尽（每类约1万次/天），已跳过该分类，继续其他类目", fetcher.displayName);
                break;
            } catch (Exception e) {
                failedCalls.incrementAndGet();
                lastError = e.getMessage();
                log.warn("TianAPI 素材拉取失败: category={}, err={}", fetcher.category, e.getMessage());
            }
        }

        totalImported.addAndGet(imported.get());
        totalSkipped.addAndGet(skipped.get());
        totalCalls.addAndGet(calls.get());
        saveCategoryStat(runId, fetcher.category, fetcher.displayName, numHttpCalls, calls.get(), imported.get(),
                skipped.get(), emptyResponses.get(), failedCalls.get(), quotaExhausted, stopReason, lastError);
        updateRunProgress(runId);
        return Map.of("imported", imported.get(), "skipped", skipped.get(), "calls", calls.get());
    }

    private Map<String, Integer> runDialogueCategory(Long runId, Long userId, int numHttpCalls, int delayMs,
                                                     AtomicInteger totalImported, AtomicInteger totalSkipped, AtomicInteger totalCalls,
                                                     AtomicInteger totalKbImported) {
        AtomicInteger imported = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger calls = new AtomicInteger(0);
        AtomicInteger emptyResponses = new AtomicInteger(0);
        AtomicInteger failedCalls = new AtomicInteger(0);
        boolean quotaExhausted = false;
        String stopReason = "planned_calls_completed";
        String lastError = null;

        for (int i = 0; i < numHttpCalls; i++) {
            sleep(delayMs);
            try {
                calls.incrementAndGet();
                Map<String, String> m = tianApiService.classicDialogue();
                if (m == null || m.isEmpty()) {
                    emptyResponses.incrementAndGet();
                    continue;
                }
                String dialogue = m.getOrDefault("dialogue", "");
                String source = m.getOrDefault("source", "");
                if (!StringUtils.hasText(dialogue)) {
                    emptyResponses.incrementAndGet();
                    continue;
                }

                String content = dialogue;
                if (StringUtils.hasText(m.get("english"))) {
                    content = dialogue + "\n" + m.get("english");
                }
                if (copyLibraryRepository.countByUserIdAndCategoryAndContentAndDeleted(userId, "tianapi_dialogue", content) > 0) {
                    skipped.incrementAndGet();
                    continue;
                }
                String title = StringUtils.hasText(source) ? source : truncate(dialogue, TITLE_MAX_LEN);
                saveCopy(userId, "tianapi_dialogue", "经典台词", title, content, totalKbImported);
                imported.incrementAndGet();
            } catch (TianApiQuotaExceededException e) {
                quotaExhausted = true;
                stopReason = "quota_exhausted";
                lastError = e.getMessage();
                log.info("TianAPI 分类 经典台词 今日配额已用尽，已跳过该分类");
                break;
            } catch (TianApiUnavailableException e) {
                stopReason = "api_unavailable";
                lastError = e.getMessage();
                log.warn("TianAPI 分类 经典台词 接口不可用，停止该分类: {}", e.getMessage());
                break;
            } catch (Exception e) {
                failedCalls.incrementAndGet();
                lastError = e.getMessage();
                log.warn("TianAPI 经典台词拉取失败: err={}", e.getMessage());
            }
        }

        totalImported.addAndGet(imported.get());
        totalSkipped.addAndGet(skipped.get());
        totalCalls.addAndGet(calls.get());
        saveCategoryStat(runId, "tianapi_dialogue", "经典台词", numHttpCalls, calls.get(), imported.get(),
                skipped.get(), emptyResponses.get(), failedCalls.get(), quotaExhausted, stopReason, lastError);
        updateRunProgress(runId);
        return Map.of("imported", imported.get(), "skipped", skipped.get(), "calls", calls.get());
    }

    private Map<String, Integer> runGodReplyCategory(Long runId, Long userId, int numHttpCalls, int delayMs,
                                                     AtomicInteger totalImported, AtomicInteger totalSkipped, AtomicInteger totalCalls,
                                                     AtomicInteger totalKbImported) {
        AtomicInteger imported = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger calls = new AtomicInteger(0);
        return runBatchCategory(runId, userId, "tianapi_godreply", "神回复", numHttpCalls, delayMs,
                () -> tianApiService.godReply(10), "title", "content",
                totalImported, totalSkipped, totalCalls, totalKbImported);
    }

    private static final String[] HOT_WORDS = {"一哥", "YYDS", "内卷", "摆烂", "绝绝子", "栓Q", "芭比Q", "emo", "天花板", "破防", "卷", "躺平", "拿捏", "整活", "上头"};

    private Map<String, Integer> runHotWordCategory(Long runId, Long userId, int numHttpCalls, int delayMs,
                                                    AtomicInteger totalImported, AtomicInteger totalSkipped, AtomicInteger totalCalls,
                                                    AtomicInteger totalKbImported) {
        AtomicInteger imported = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger calls = new AtomicInteger(0);
        return runBatchCategoryWithIndex(runId, userId, "tianapi_hotword", "网络流行语", numHttpCalls, delayMs,
                b -> tianApiService.hotWord(HOT_WORDS[b % HOT_WORDS.length], 10), "title", "content",
                totalImported, totalSkipped, totalCalls, totalKbImported);
    }

    private Map<String, Integer> runDictumCategory(Long runId, Long userId, int numHttpCalls, int delayMs,
                                                   AtomicInteger totalImported, AtomicInteger totalSkipped, AtomicInteger totalCalls,
                                                   AtomicInteger totalKbImported) {
        AtomicInteger imported = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger calls = new AtomicInteger(0);
        return runBatchCategory(runId, userId, "tianapi_dictum", "名言警句", numHttpCalls, delayMs,
                () -> tianApiService.dictum(10), "mrname", "content",
                totalImported, totalSkipped, totalCalls, totalKbImported);
    }

    private Map<String, Integer> runMingyanCategory(Long runId, Long userId, int numHttpCalls, int delayMs,
                                                    AtomicInteger totalImported, AtomicInteger totalSkipped, AtomicInteger totalCalls,
                                                    AtomicInteger totalKbImported) {
        AtomicInteger imported = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger calls = new AtomicInteger(0);
        return runBatchCategoryWithIndex(runId, userId, "tianapi_mingyan", "名人名言", numHttpCalls, delayMs,
                b -> tianApiService.mingyan(10, (b % 24) + 1), "author", "content",
                totalImported, totalSkipped, totalCalls, totalKbImported);
    }

    /** 雷人笑话：每次 10 条 */
    private Map<String, Integer> runJokeCategory(Long runId, Long userId, int numHttpCalls, int delayMs,
                                                  AtomicInteger totalImported, AtomicInteger totalSkipped, AtomicInteger totalCalls,
                                                  AtomicInteger totalKbImported) {
        return runBatchCategory(runId, userId, "tianapi_joke", "雷人笑话", numHttpCalls, delayMs,
                () -> tianApiService.joke(10), "title", "content",
                totalImported, totalSkipped, totalCalls, totalKbImported);
    }

    /** 歇后语：quest + result 组合为 content */
    private Map<String, Integer> runXiehouyuCategory(Long runId, Long userId, int numHttpCalls, int delayMs,
                                                    AtomicInteger totalImported, AtomicInteger totalSkipped, AtomicInteger totalCalls,
                                                    AtomicInteger totalKbImported) {
        return runBatchCategory(runId, userId, "tianapi_xiehouyu", "歇后语", numHttpCalls, delayMs,
                () -> tianApiService.xiehouyu(10), "quest", "content",
                totalImported, totalSkipped, totalCalls, totalKbImported);
    }

    /** 民俗对联：shanglian+xialian+hengpi 组合 */
    private Map<String, Integer> runMsDuilianCategory(Long runId, Long userId, int numHttpCalls, int delayMs,
                                                      AtomicInteger totalImported, AtomicInteger totalSkipped, AtomicInteger totalCalls,
                                                      AtomicInteger totalKbImported) {
        String[] fenleiArr = {"春联", "婚联", "寿联", "挽联", ""};
        return runBatchCategoryWithIndex(runId, userId, "tianapi_msdl", "民俗对联", numHttpCalls, delayMs,
                b -> tianApiService.msDuilian(10, fenleiArr[b % fenleiArr.length]), "fenlei", "content",
                totalImported, totalSkipped, totalCalls, totalKbImported);
    }

    private static final String[] FL_MINGJU_TYPES = {"春天", "秋天", "冬天", "夏天", "写雨", "中秋节", "春节", "爱情", "友情", "励志"};

    /** 分类名句：按 type 轮询 */
    private Map<String, Integer> runFlMingjuCategory(Long runId, Long userId, int numHttpCalls, int delayMs,
                                                     AtomicInteger totalImported, AtomicInteger totalSkipped, AtomicInteger totalCalls,
                                                     AtomicInteger totalKbImported) {
        return runBatchCategoryWithType(runId, userId, "tianapi_flmj", "分类名句", numHttpCalls, delayMs,
                FL_MINGJU_TYPES, 10, tianApiService::flMingju, "source", "content",
                totalImported, totalSkipped, totalCalls, totalKbImported);
    }

    /** 通用批量采集：title+content 结构 */
    private Map<String, Integer> runBatchCategory(Long runId, Long userId, String category, String displayName, int numHttpCalls, int delayMs,
                                                   java.util.function.Supplier<List<Map<String, String>>> fetcher,
                                                   String titleKey, String contentKey,
                                                   AtomicInteger totalImported, AtomicInteger totalSkipped, AtomicInteger totalCalls,
                                                   AtomicInteger totalKbImported) {
        AtomicInteger imported = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger calls = new AtomicInteger(0);
        AtomicInteger emptyResponses = new AtomicInteger(0);
        AtomicInteger failedCalls = new AtomicInteger(0);
        boolean quotaExhausted = false;
        String stopReason = "planned_calls_completed";
        String lastError = null;

        for (int b = 0; b < numHttpCalls; b++) {
            sleep(delayMs);
            try {
                calls.incrementAndGet();
                List<Map<String, String>> list = fetcher.get();
                if (list == null || list.isEmpty()) {
                    emptyResponses.incrementAndGet();
                    continue;
                }

                for (Map<String, String> item : list) {
                    String title = item.getOrDefault(titleKey, "");
                    String content = item.getOrDefault(contentKey, "");
                    if (!StringUtils.hasText(content)) continue;
                    String fullContent = StringUtils.hasText(title) ? title + "\n" + content : content;
                    if (copyLibraryRepository.countByUserIdAndCategoryAndContentAndDeleted(userId, category, fullContent) > 0) {
                        skipped.incrementAndGet();
                        continue;
                    }
                    saveCopy(userId, category, displayName, truncate(title, TITLE_MAX_LEN), fullContent, totalKbImported);
                    imported.incrementAndGet();
                }
            } catch (TianApiQuotaExceededException e) {
                quotaExhausted = true;
                stopReason = "quota_exhausted";
                lastError = e.getMessage();
                log.info("TianAPI 分类 {} 今日配额已用尽，已跳过该分类", displayName);
                break;
            } catch (TianApiUnavailableException e) {
                stopReason = "api_unavailable";
                lastError = e.getMessage();
                log.warn("TianAPI 分类 {} 接口不可用，停止该分类: {}", displayName, e.getMessage());
                break;
            } catch (Exception e) {
                failedCalls.incrementAndGet();
                lastError = e.getMessage();
                log.warn("TianAPI {} 拉取失败: err={}", displayName, e.getMessage());
            }
        }

        totalImported.addAndGet(imported.get());
        totalSkipped.addAndGet(skipped.get());
        totalCalls.addAndGet(calls.get());
        saveCategoryStat(runId, category, displayName, numHttpCalls, calls.get(), imported.get(), skipped.get(),
                emptyResponses.get(), failedCalls.get(), quotaExhausted, stopReason, lastError);
        updateRunProgress(runId);
        return Map.of("imported", imported.get(), "skipped", skipped.get(), "calls", calls.get());
    }

    /** 通用批量采集：按 type 轮询（如分类名句） */
    private Map<String, Integer> runBatchCategoryWithType(Long runId, Long userId, String category, String displayName, int numHttpCalls, int delayMs,
                                                          String[] types, int batchSize,
                                                          java.util.function.BiFunction<String, Integer, List<Map<String, String>>> fetcher,
                                                          String titleKey, String contentKey,
                                                          AtomicInteger totalImported, AtomicInteger totalSkipped, AtomicInteger totalCalls,
                                                          AtomicInteger totalKbImported) {
        AtomicInteger imported = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger calls = new AtomicInteger(0);
        AtomicInteger emptyResponses = new AtomicInteger(0);
        AtomicInteger failedCalls = new AtomicInteger(0);
        boolean quotaExhausted = false;
        String stopReason = "planned_calls_completed";
        String lastError = null;

        for (int b = 0; b < numHttpCalls; b++) {
            sleep(delayMs);
            try {
                calls.incrementAndGet();
                String type = types[b % types.length];
                List<Map<String, String>> list = fetcher.apply(type, batchSize);
                if (list == null || list.isEmpty()) {
                    emptyResponses.incrementAndGet();
                    continue;
                }

                for (Map<String, String> item : list) {
                    String title = item.getOrDefault(titleKey, "");
                    String content = item.getOrDefault(contentKey, "");
                    if (!StringUtils.hasText(content)) continue;
                    String fullContent = StringUtils.hasText(title) ? title + "：" + content : content;
                    if (copyLibraryRepository.countByUserIdAndCategoryAndContentAndDeleted(userId, category, fullContent) > 0) {
                        skipped.incrementAndGet();
                        continue;
                    }
                    saveCopy(userId, category, displayName, truncate(title, TITLE_MAX_LEN), fullContent, totalKbImported);
                    imported.incrementAndGet();
                }
            } catch (TianApiQuotaExceededException e) {
                quotaExhausted = true;
                stopReason = "quota_exhausted";
                lastError = e.getMessage();
                log.info("TianAPI 分类 {} 今日配额已用尽，已跳过该分类", displayName);
                break;
            } catch (TianApiUnavailableException e) {
                stopReason = "api_unavailable";
                lastError = e.getMessage();
                log.warn("TianAPI 分类 {} 接口不可用，停止该分类: {}", displayName, e.getMessage());
                break;
            } catch (Exception e) {
                failedCalls.incrementAndGet();
                lastError = e.getMessage();
                log.warn("TianAPI {} 拉取失败: err={}", displayName, e.getMessage());
            }
        }

        totalImported.addAndGet(imported.get());
        totalSkipped.addAndGet(skipped.get());
        totalCalls.addAndGet(calls.get());
        saveCategoryStat(runId, category, displayName, numHttpCalls, calls.get(), imported.get(), skipped.get(),
                emptyResponses.get(), failedCalls.get(), quotaExhausted, stopReason, lastError);
        updateRunProgress(runId);
        return Map.of("imported", imported.get(), "skipped", skipped.get(), "calls", calls.get());
    }

    private Map<String, Integer> runBatchCategoryWithIndex(Long runId, Long userId, String category, String displayName,
                                                           int numHttpCalls, int delayMs,
                                                           java.util.function.IntFunction<List<Map<String, String>>> fetcher,
                                                           String titleKey, String contentKey,
                                                           AtomicInteger totalImported, AtomicInteger totalSkipped, AtomicInteger totalCalls,
                                                           AtomicInteger totalKbImported) {
        AtomicInteger imported = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger calls = new AtomicInteger(0);
        AtomicInteger emptyResponses = new AtomicInteger(0);
        AtomicInteger failedCalls = new AtomicInteger(0);
        boolean quotaExhausted = false;
        String stopReason = "planned_calls_completed";
        String lastError = null;

        for (int b = 0; b < numHttpCalls; b++) {
            sleep(delayMs);
            try {
                calls.incrementAndGet();
                List<Map<String, String>> list = fetcher.apply(b);
                if (list == null || list.isEmpty()) {
                    emptyResponses.incrementAndGet();
                    continue;
                }

                for (Map<String, String> item : list) {
                    String title = item.getOrDefault(titleKey, "");
                    String content = item.getOrDefault(contentKey, "");
                    if (!StringUtils.hasText(content)) continue;
                    String fullContent = StringUtils.hasText(title) ? title + "\n" + content : content;
                    if (copyLibraryRepository.countByUserIdAndCategoryAndContentAndDeleted(userId, category, fullContent) > 0) {
                        skipped.incrementAndGet();
                        continue;
                    }
                    saveCopy(userId, category, displayName, truncate(title, TITLE_MAX_LEN), fullContent, totalKbImported);
                    imported.incrementAndGet();
                }
            } catch (TianApiQuotaExceededException e) {
                quotaExhausted = true;
                stopReason = "quota_exhausted";
                lastError = e.getMessage();
                log.info("TianAPI 分类 {} 今日配额已用尽，已跳过该分类", displayName);
                break;
            } catch (TianApiUnavailableException e) {
                stopReason = "api_unavailable";
                lastError = e.getMessage();
                log.warn("TianAPI 分类 {} 接口不可用，停止该分类: {}", displayName, e.getMessage());
                break;
            } catch (Exception e) {
                failedCalls.incrementAndGet();
                lastError = e.getMessage();
                log.warn("TianAPI {} 拉取失败: err={}", displayName, e.getMessage());
            }
        }

        totalImported.addAndGet(imported.get());
        totalSkipped.addAndGet(skipped.get());
        totalCalls.addAndGet(calls.get());
        saveCategoryStat(runId, category, displayName, numHttpCalls, calls.get(), imported.get(), skipped.get(),
                emptyResponses.get(), failedCalls.get(), quotaExhausted, stopReason, lastError);
        updateRunProgress(runId);
        return Map.of("imported", imported.get(), "skipped", skipped.get(), "calls", calls.get());
    }

    private void saveCopy(Long userId, String category, String displayName, String title, String content,
                          AtomicInteger totalKbImported) {
        String finalTitle = StringUtils.hasText(title) ? truncate(title, TITLE_MAX_LEN) : truncate(content, TITLE_MAX_LEN);
        CopyLibrary e = new CopyLibrary();
        e.setUserId(userId);
        e.setTitle(finalTitle);
        e.setContent(content);
        e.setCategory(category);
        e.setTags("TianAPI," + displayName);
        e.setWordCount(content != null ? content.length() : 0);
        e.setUseCount(0);
        e.setStatus(1);
        copyLibraryRepository.save(e);

        // 同步入库到 AI 知识库（文案类适合进 huashu 话术库）
        if (properties.isKbImportEnabled() && knowledgeBaseService != null && totalKbImported != null) {
            try {
                Long kbId = knowledgeBaseService.resolveKbIdByName(userId, properties.getKbName());
                if (kbId == null) return;
                String kbTitle = displayName + ": " + finalTitle;
                String kbContent = "# " + kbTitle + "\n\n" + (content != null ? content : "");
                var doc = knowledgeBaseService.uploadDocument(kbId, kbTitle, kbContent, "md", userId, SOURCE_TYPE_TIANAPI, category, null);
                if (doc != null) {
                    totalKbImported.incrementAndGet();
                }
            } catch (Exception ex) {
                log.warn("TianAPI 知识库入库失败: category={}, err={}", category, ex.getMessage());
            }
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record ContentFetcher(String category, String displayName, ContentSupplier fetch, int itemsPerCall) {}

    private record ProgressSnapshot(int imported, int skipped, int calls) {}

    @FunctionalInterface
    private interface ContentSupplier {
        String get();
    }
}
