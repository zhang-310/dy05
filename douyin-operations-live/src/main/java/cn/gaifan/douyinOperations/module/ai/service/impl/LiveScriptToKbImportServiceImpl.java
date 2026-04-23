package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.util.ContentFingerprintUtil;
import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiKnowledgeBaseRepository;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.ai.service.LiveScriptToKbImportService;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 话术→知识库管道：将高效 live_script 入库 huashu，供 RAG 检索。
 * 依赖 uploadDocument 的内容指纹去重，重复内容自动跳过。
 */
@Service
public class LiveScriptToKbImportServiceImpl implements LiveScriptToKbImportService {

    private static final Logger log = LoggerFactory.getLogger(LiveScriptToKbImportServiceImpl.class);
    private static final String SOURCE_TYPE_LIVE_SCRIPT = "live_script";

    @Value("${app.ai.evolution.live-script-import-threshold:80}")
    private int liveScriptImportThreshold;

    @Resource
    private LiveScriptRepository liveScriptRepository;
    @Resource
    private AiKnowledgeBaseRepository knowledgeBaseRepository;
    @Resource
    private AiKbDocumentRepository documentRepository;
    @Resource
    private KnowledgeBaseService knowledgeBaseService;
    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Value("${app.ai.live-script-to-kb.enabled:true}")
    private boolean enabled;

    private Counter importedCounter;
    private Counter skippedCounter;

    @PostConstruct
    void initMetrics() {
        if (meterRegistry != null) {
            importedCounter = Counter.builder("ai.livescript_to_kb.imported")
                    .description("话术入库成功数")
                    .register(meterRegistry);
            skippedCounter = Counter.builder("ai.livescript_to_kb.skipped")
                    .description("话术入库跳过数（重复或无效）")
                    .register(meterRegistry);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importForUser(Long userId, int maxPerRun) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported", 0);
        result.put("skipped", 0);
        result.put("errors", new java.util.ArrayList<String>());

        if (!enabled) {
            result.put("reason", "话术→知识库管道已禁用");
            return result;
        }

        Long kbId = knowledgeBaseService.resolveKbIdByName(userId, "huashu");
        if (kbId == null) {
            result.put("reason", "用户无 huashu 知识库");
            return result;
        }

        List<LiveScript> scripts = liveScriptRepository.findByUserIdAndEffectivenessScoreGreaterThanEqual(userId, BigDecimal.valueOf(liveScriptImportThreshold));
        if (scripts.isEmpty()) {
            result.put("reason", "无符合条件的高效话术");
            return result;
        }

        int imported = 0;
        int skipped = 0;
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.get("errors");

        for (LiveScript s : scripts) {
            if (imported >= maxPerRun) break;

            String content = s.getScriptContent();
            if (content == null || content.trim().length() < 50) {
                skipped++;
                if (skippedCounter != null) skippedCounter.increment();
                continue;
            }

            String title = buildTitle(s);
            Map<String, String> metadata = Map.of("source_script_id", String.valueOf(s.getId()), "script_type", s.getScriptType() != null ? s.getScriptType() : "custom");

            try {
                String fingerprint = ContentFingerprintUtil.compute(content);
                boolean alreadyExists = fingerprint != null
                        && documentRepository.findFirstByKbIdAndContentFingerprintAndDeletedOrderByIdDesc(kbId, fingerprint, 0).isPresent();
                if (alreadyExists) {
                    skipped++;
                    if (skippedCounter != null) skippedCounter.increment();
                    continue;
                }

                knowledgeBaseService.uploadDocument(kbId, title, content, "md", userId, SOURCE_TYPE_LIVE_SCRIPT, "script", metadata);
                imported++;
                if (importedCounter != null) importedCounter.increment();
                log.debug("话术入库 huashu: scriptId={}, kbId={}", s.getId(), kbId);
            } catch (Exception e) {
                errors.add("scriptId=" + s.getId() + ": " + e.getMessage());
                log.warn("话术入库失败 scriptId={}: {}", s.getId(), e.getMessage());
            }
        }

        result.put("imported", imported);
        result.put("skipped", skipped);
        return result;
    }

    @Override
    public Map<String, Object> importForAllUsers(int maxPerUser) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalImported", 0);
        summary.put("totalSkipped", 0);
        summary.put("usersProcessed", 0);

        if (!enabled) {
            summary.put("reason", "话术→知识库管道已禁用");
            return summary;
        }

        List<Long> userIds = knowledgeBaseRepository.findDistinctUserIds();
        int usersProcessed = 0;
        int totalImported = 0;
        int totalSkipped = 0;
        for (Long userId : userIds) {
            if (knowledgeBaseService.resolveKbIdByName(userId, "huashu") == null) continue;
            Map<String, Object> r = importForUser(userId, maxPerUser);
            usersProcessed++;
            totalImported += (Integer) r.getOrDefault("imported", 0);
            totalSkipped += (Integer) r.getOrDefault("skipped", 0);
        }
        summary.put("usersProcessed", usersProcessed);
        summary.put("totalImported", totalImported);
        summary.put("totalSkipped", totalSkipped);

        return summary;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importScriptsByIds(Long userId, List<Long> scriptIds) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported", 0);
        result.put("skipped", 0);
        result.put("errors", new java.util.ArrayList<String>());

        if (scriptIds == null || scriptIds.isEmpty()) {
            return result;
        }
        if (!enabled) {
            result.put("reason", "话术→知识库管道已禁用");
            return result;
        }

        Long kbId = knowledgeBaseService.resolveKbIdByName(userId, "huashu");
        if (kbId == null) {
            result.put("reason", "用户无 huashu 知识库");
            return result;
        }

        List<LiveScript> scripts = liveScriptRepository.findByIdInAndUserId(scriptIds, userId);
        if (scripts.isEmpty()) {
            result.put("reason", "无符合条件的归属话术");
            return result;
        }

        int imported = 0;
        int skipped = 0;
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) result.get("errors");

        for (LiveScript s : scripts) {
            String content = s.getScriptContent();
            if (content == null || content.trim().length() < 50) {
                skipped++;
                if (skippedCounter != null) skippedCounter.increment();
                continue;
            }

            String title = buildTitle(s);
            Map<String, String> metadata = Map.of("source_script_id", String.valueOf(s.getId()), "script_type", s.getScriptType() != null ? s.getScriptType() : "custom");

            try {
                String fingerprint = ContentFingerprintUtil.compute(content);
                boolean alreadyExists = fingerprint != null
                        && documentRepository.findFirstByKbIdAndContentFingerprintAndDeletedOrderByIdDesc(kbId, fingerprint, 0).isPresent();
                if (alreadyExists) {
                    skipped++;
                    if (skippedCounter != null) skippedCounter.increment();
                    continue;
                }

                knowledgeBaseService.uploadDocument(kbId, title, content, "md", userId, SOURCE_TYPE_LIVE_SCRIPT, "script", metadata);
                imported++;
                if (importedCounter != null) importedCounter.increment();
                log.debug("话术入库 huashu: scriptId={}, kbId={}", s.getId(), kbId);
            } catch (Exception e) {
                errors.add("scriptId=" + s.getId() + ": " + e.getMessage());
                log.warn("话术入库失败 scriptId={}: {}", s.getId(), e.getMessage());
            }
        }

        result.put("imported", imported);
        result.put("skipped", skipped);
        return result;
    }

    private static String buildTitle(LiveScript s) {
        String type = s.getScriptType() != null ? s.getScriptType() : "custom";
        String preview = s.getScriptContent() != null && s.getScriptContent().length() > 30
                ? s.getScriptContent().substring(0, 30).replace("\n", " ") + "…"
                : (s.getScriptContent() != null ? s.getScriptContent() : "");
        return "话术-" + type + "-" + s.getId() + " " + preview;
    }

}
