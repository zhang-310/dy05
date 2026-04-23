package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase;
import cn.gaifan.douyinOperations.module.ai.entity.KbImportReport;
import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiKnowledgeBaseRepository;
import cn.gaifan.douyinOperations.module.ai.entity.KbImportCheckpoint;
import cn.gaifan.douyinOperations.module.ai.repository.KbImportCheckpointRepository;
import cn.gaifan.douyinOperations.module.ai.repository.KbImportReportRepository;
import cn.gaifan.douyinOperations.module.ai.service.DocArchiveService;
import cn.gaifan.douyinOperations.module.ai.service.ImportProgress;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseImportService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.ai.util.ContentFingerprintUtil;
import cn.gaifan.douyinOperations.module.ai.util.ContentSecurityScanner;
import cn.gaifan.douyinOperations.module.ai.util.DocumentParser;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 从本地路径导入知识库文档（MD/TXT）
 */
@Service
public class KnowledgeBaseImportServiceImpl implements KnowledgeBaseImportService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseImportServiceImpl.class);

    private static final int MIN_CONTENT_LEN = 20;
    private static final int PREVIEW_LEN = 400;

    @Value("${app.ai.kb.import-parallelism:6}")
    private int importParallelism;

    @Value("${app.ai.kb.import.max-depth:20}")
    private int maxDepth;

    @Value("${app.ai.kb.import.max-files:5000}")
    private int maxFiles;

    @Value("${app.ai.kb.security.scan-on-import:true}")
    private boolean securityScanOnImport;

    /** 匹配文件名中的日期，如 evolved_2026-02-15_0253 */
    private static final Pattern DATE_IN_FILENAME = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");

    private static final String[] DOUYIN_KEYWORDS = {
            "抖音", "短视频", "直播", "涨粉", "带货", "橱窗", "小店", "算法推荐", "播放量", "完播率",
            "达人", "MCN", "种草", "爆款", "话术", "人设", "账号", "运营", "转化率", "黄金3秒"
    };
    private static final String[] TECH_KEYWORDS = {
            "API", "接口", "Vue", "React", "Spring", "数据库", "SQL", "Docker", "Kubernetes",
            "微服务", "前端", "后端", "部署", "代码", "框架", "组件", "函数", "类", "算法", "Git"
    };

    @Resource
    private AiKnowledgeBaseRepository knowledgeBaseRepository;

    @Resource
    private AiKbDocumentRepository documentRepository;

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private DocArchiveService docArchiveService;

    @Resource
    private PlatformTransactionManager transactionManager;

    @Resource
    private KbImportReportRepository importReportRepository;

    @Resource
    private KbImportCheckpointRepository importCheckpointRepository;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ImportResult importFromPath(String sourcePath, Long kbId, String kbName, boolean autoClassify, Long userId, ImportProgress progress) {
        if (sourcePath == null || sourcePath.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "导入路径不能为空");
        }

        final Long resolvedKbId;
        final Long douyinKbId;
        final Long zhishiKbId;
        if (autoClassify) {
            douyinKbId = resolveKbId(null, "douyin", userId);
            zhishiKbId = resolveKbId(null, "zhishi", userId);
            resolvedKbId = null;
            if (douyinKbId == null && zhishiKbId == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "未找到 douyin 或 zhishi 知识库");
            }
        } else {
            Long r = resolveKbId(kbId, kbName, userId);
            if (r == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "未找到知识库");
            }
            resolvedKbId = r;
            douyinKbId = null;
            zhishiKbId = null;
        }

        Path dir = Path.of(sourcePath.trim());
        if (!Files.isDirectory(dir)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "路径不是有效目录: " + sourcePath);
        }

        long startMs = System.currentTimeMillis();
        Long reportKbId = resolvedKbId != null ? resolvedKbId : (douyinKbId != null ? douyinKbId : zhishiKbId);
        if (reportKbId == null) reportKbId = 0L;

        List<String> errors = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        Map<String, Integer> byKb = autoClassify ? new ConcurrentHashMap<>(Map.of("douyin", 0, "zhishi", 0)) : null;
        AtomicBoolean abort = new AtomicBoolean(false);

        List<Path> files;
        try (Stream<Path> walk = Files.walk(dir, maxDepth)) {
            files = walk.filter(Files::isRegularFile)
                    .filter(p -> {
                        String n = p.getFileName().toString().toLowerCase();
                        return n.endsWith(".md") || n.endsWith(".txt")
                                || n.endsWith(".doc") || n.endsWith(".docx") || n.endsWith(".pdf");
                    })
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .limit(maxFiles)
                    .toList();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "遍历目录失败: " + e.getMessage());
        }

        if (progress != null) {
            progress.setTotal(files.size());
            progress.addLog("开始导入，共 " + files.size() + " 个文件，并行度 " + importParallelism);
        }

        ExecutorService executor = Executors.newFixedThreadPool(importParallelism);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (Path file : files) {
                if (abort.get()) break;
                futures.add(executor.submit(() -> processOneFile(
                        file, dir, resolvedKbId, douyinKbId, zhishiKbId, autoClassify, userId,
                        success, failed, skipped, errors, byKb, progress, abort)));
            }
            for (Future<?> f : futures) {
                try {
                    f.get();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof BusinessException be) {
                        throw be;
                    }
                    log.error("导入任务异常", cause);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导入被中断");
                }
            }
        } finally {
            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }
        }

        int total = success.get() + failed.get() + skipped.get();
        List<String> consolidatedErrors = consolidateErrors(errors);
        String hint = null;
        if (success.get() == 0 && failed.get() > 0 && !consolidatedErrors.isEmpty()) {
            hint = "全部导入失败。请先启动 Milvus、Ollama、Elasticsearch 后再试。";
        }
        log.info("导入完成: 路径={}, 总数={}, 成功={}, 失败={}, 跳过={}, 分布={}", sourcePath, total, success.get(), failed.get(), skipped.get(), byKb);
        ImportResult result = new ImportResult(total, success.get(), failed.get(), skipped.get(), byKb, consolidatedErrors, hint);
        if (progress != null) {
            progress.done(result);
            progress.addLog("导入完成: 成功 " + result.success() + "，失败 " + result.failed() + (result.skipped() > 0 ? "，跳过 " + result.skipped() + "（已存在）" : ""));
        }
        persistImportReport(reportKbId, sourcePath, "directory", files.size(), result, startMs, userId);
        return result;
    }

    private void persistImportReport(Long kbId, String sourcePath, String importType, int totalFiles,
                                    ImportResult result, long startMs, Long userId) {
        try {
            KbImportReport report = new KbImportReport();
            report.setKbId(kbId);
            report.setSourcePath(sourcePath);
            report.setImportType(importType);
            report.setTotalFiles(totalFiles);
            report.setSuccessCount(result.success());
            report.setFailedCount(result.failed());
            report.setSkippedCount(result.skipped());
            report.setContentType("auto");
            report.setDurationMs(System.currentTimeMillis() - startMs);
            report.setUserId(userId);
            if (result.errors() != null && !result.errors().isEmpty()) {
                try {
                    report.setErrors(objectMapper.writeValueAsString(result.errors()));
                } catch (JsonProcessingException e) {
                    report.setErrors("[]");
                }
            }
            importReportRepository.save(report);
        } catch (Exception e) {
            log.warn("写入导入报告失败: {}", e.getMessage());
        }
    }

    @Override
    public ImportResult importIncremental(String sourcePath, Long kbId, Long userId, ImportProgress progress) {
        if (sourcePath == null || sourcePath.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "导入路径不能为空");
        }
        if (kbId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "增量导入需指定 kbId");
        }
        AiKnowledgeBase kb = knowledgeBaseRepository.findById(kbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));
        if (!kb.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限操作此知识库");
        }

        Path dir = Path.of(sourcePath.trim());
        if (!Files.isDirectory(dir)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "路径不是有效目录: " + sourcePath);
        }

        long lastImportMs = 0L;
        Optional<KbImportCheckpoint> cpOpt = importCheckpointRepository.findByKbIdAndSourcePathAndDeleted(kbId, sourcePath.trim(), 0);
        if (cpOpt.isPresent()) {
            lastImportMs = cpOpt.get().getLastImportTime().getTime();
        }

        List<Path> files;
        try (Stream<Path> walk = Files.walk(dir, maxDepth)) {
            final long threshold = lastImportMs;
            files = walk.filter(Files::isRegularFile)
                    .filter(p -> {
                        String n = p.getFileName().toString().toLowerCase();
                        return n.endsWith(".md") || n.endsWith(".txt")
                                || n.endsWith(".doc") || n.endsWith(".docx") || n.endsWith(".pdf");
                    })
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .filter(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toMillis() > threshold;
                        } catch (IOException e) {
                            return true;
                        }
                    })
                    .limit(maxFiles)
                    .toList();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "遍历目录失败: " + e.getMessage());
        }

        long startMs = System.currentTimeMillis();
        List<String> errors = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicBoolean abort = new AtomicBoolean(false);

        if (progress != null) {
            progress.setTotal(files.size());
            progress.addLog("增量导入，共 " + files.size() + " 个新/改文件（上次导入: " + (lastImportMs > 0 ? new Timestamp(lastImportMs) : "无") + "）");
        }

        ExecutorService executor = Executors.newFixedThreadPool(importParallelism);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (Path file : files) {
                if (abort.get()) break;
                futures.add(executor.submit(() -> processOneFile(
                        file, dir, kbId, null, null, false, userId,
                        success, failed, skipped, errors, null, progress, abort)));
            }
            for (Future<?> f : futures) {
                try {
                    f.get();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof BusinessException be) throw be;
                    log.error("增量导入任务异常", cause);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "导入被中断");
                }
            }
        } finally {
            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }
        }

        int total = success.get() + failed.get() + skipped.get();
        List<String> consolidatedErrors = consolidateErrors(errors);
        String hint = null;
        if (success.get() == 0 && failed.get() > 0 && !consolidatedErrors.isEmpty()) {
            hint = "全部导入失败。请先启动 Milvus、Ollama、Elasticsearch 后再试。";
        }
        ImportResult result = new ImportResult(total, success.get(), failed.get(), skipped.get(), null, consolidatedErrors, hint);
        if (progress != null) {
            progress.done(result);
            progress.addLog("增量导入完成: 成功 " + result.success() + "，失败 " + result.failed());
        }
        persistImportReport(kbId, sourcePath, "incremental", files.size(), result, startMs, userId);

        KbImportCheckpoint checkpoint = cpOpt.orElse(new KbImportCheckpoint());
        checkpoint.setKbId(kbId);
        checkpoint.setSourcePath(sourcePath.trim());
        checkpoint.setLastImportTime(new Timestamp(System.currentTimeMillis()));
        checkpoint.setFileCount(files.size());
        importCheckpointRepository.save(checkpoint);

        return result;
    }

    /** 从文件相对根目录的路径提取目录结构元数据（relativePath、dirCategory、dirSubCategory、dirDepth） */
    private Map<String, String> extractDirMetadata(Path file, Path rootDir) {
        Map<String, String> meta = new HashMap<>();
        try {
            Path relative = rootDir.relativize(file);
            meta.put("relativePath", relative.toString().replace('\\', '/'));
            meta.put("fileName", file.getFileName().toString());
            int depth = relative.getNameCount();
            if (depth > 1) meta.put("dirCategory", relative.getName(0).toString());
            if (depth > 2) meta.put("dirSubCategory", relative.getName(1).toString());
            meta.put("dirDepth", String.valueOf(depth - 1));
        } catch (IllegalArgumentException e) {
            log.debug("extractDirMetadata: file 不在 rootDir 下: {} / {}", file, rootDir);
        }
        return meta;
    }

    @SuppressWarnings("null")
    private void processOneFile(Path file, Path rootDir, Long resolvedKbId, Long douyinKbId, Long zhishiKbId, boolean autoClassify, Long userId,
                                AtomicInteger success, AtomicInteger failed, AtomicInteger skipped, List<String> errors, Map<String, Integer> byKb,
                                ImportProgress progress, AtomicBoolean abort) {
        if (abort.get()) return;

        String name = file.getFileName().toString().toLowerCase();
        String title = file.getFileName().toString();
        if (title.lastIndexOf('.') > 0) {
            title = title.substring(0, title.lastIndexOf('.'));
        }

        String content;
        try {
            content = DocumentParser.parse(file);
        } catch (IOException e) {
            log.warn("读取/解析文件失败: {} - {}", file, e.getMessage());
            errors.add(file + ": " + e.getMessage());
            failed.incrementAndGet();
            if (progress != null) {
                progress.incrementProcessed();
                progress.incrementFailed();
                progress.addLog("✗ 读取失败: " + file.getFileName());
            }
            return;
        }

        if (content == null || content.trim().length() < MIN_CONTENT_LEN) {
            log.debug("跳过过短文件: {}", file);
            return;
        }

        long targetKbId;
        String targetKbName;
        if (autoClassify) {
            String classify = classifyByContent(title, content);
            if ("douyin".equals(classify) && douyinKbId != null) {
                targetKbId = douyinKbId;
                targetKbName = "douyin";
            } else if ("zhishi".equals(classify) && zhishiKbId != null) {
                targetKbId = zhishiKbId;
                targetKbName = "zhishi";
            } else {
                targetKbId = douyinKbId != null ? douyinKbId : zhishiKbId;
                targetKbName = douyinKbId != null ? "douyin" : "zhishi";
            }
        } else {
            targetKbId = Objects.requireNonNull(resolvedKbId, "resolvedKbId");
            targetKbName = "target";
        }

        String fileType = DocumentParser.detectFileType(name);
        String finalTitle = title;
        String finalContent = content.trim();
        String finalTargetKbName = targetKbName;

        if (securityScanOnImport) {
            List<ContentSecurityScanner.SecurityWarning> warnings = ContentSecurityScanner.scan(finalContent, name);
            for (ContentSecurityScanner.SecurityWarning w : warnings) {
                log.warn("导入安全扫描 [{}]: {} - {}", name, w.type(), w.message());
            }
        }

        if (documentRepository.existsByKbIdAndTitleAndDeleted(targetKbId, finalTitle, 0)) {
            skipped.incrementAndGet();
            if (progress != null) {
                progress.incrementProcessed();
                progress.addLog("⊙ 已存在跳过: " + finalTitle);
            }
            return;
        }

        // 目录导入指纹预检：内容指纹已存在则跳过，避免重复入库
        String fingerprint = ContentFingerprintUtil.compute(finalContent);
        if (fingerprint != null && documentRepository.existsByKbIdAndContentFingerprintAndDeleted(targetKbId, fingerprint, 0)) {
            skipped.incrementAndGet();
            if (progress != null) {
                progress.incrementProcessed();
                progress.addLog("⊙ 内容已存在跳过: " + finalTitle);
            }
            return;
        }

        Map<String, String> dirMeta = extractDirMetadata(file, rootDir);
        try {
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            tx.executeWithoutResult(status -> {
                knowledgeBaseService.uploadDocument(targetKbId, finalTitle, finalContent, fileType, userId, null, "auto", dirMeta);
            });
            success.incrementAndGet();
            if (byKb != null && finalTargetKbName != null) {
                byKb.merge(finalTargetKbName, 1, Integer::sum);
            }
            LocalDate parsedDate = parseDateFromFilename(finalTitle);
            if (docArchiveService != null) {
                docArchiveService.archive(finalTitle, finalContent,
                        "target".equals(finalTargetKbName) ? null : finalTargetKbName, parsedDate);
            }
            log.info("导入成功: {} -> {}", finalTitle, finalTargetKbName);
            if (progress != null) {
                progress.incrementProcessed();
                progress.incrementSuccess();
                progress.addLog("✓ " + finalTitle + " -> " + finalTargetKbName);
            }
        } catch (Exception e) {
            String msg = e.getMessage();
            log.warn("导入失败: {} - {}", file, msg);
            errors.add(finalTitle + ": " + msg);
            failed.incrementAndGet();
            if (progress != null) {
                progress.incrementProcessed();
                progress.incrementFailed();
                progress.addLog("✗ " + finalTitle + ": " + msg);
            }
            if (isSystemicError(msg)) {
                abort.set(true);
                String hint = "首次导入失败，环境异常。请先启动 Redis、Milvus、Ollama、Elasticsearch 后重试。";
                if (progress != null) {
                    progress.error(hint + " " + msg);
                }
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, hint + " 错误: " + msg);
            }
        }
    }

    /** 合并相同错误信息，避免刷屏：按关键词归一化后合并，最多返回 3 条 */
    private List<String> consolidateErrors(List<String> raw) {
        if (raw == null || raw.isEmpty()) return raw;
        Map<String, List<String>> byReason = new LinkedHashMap<>();
        for (String e : raw) {
            String reason = e;
            int colon = e.indexOf(": ");
            if (colon > 0) reason = e.substring(colon + 2).trim();
            String norm = normalizeReason(reason);
            byReason.computeIfAbsent(norm, k -> new ArrayList<>()).add(colon > 0 ? e.substring(0, colon).trim() : e);
        }
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> ent : byReason.entrySet()) {
            if (result.size() >= 3) break;
            String reason = ent.getKey();
            List<String> titles = ent.getValue();
            if (titles.size() >= 2) {
                result.add(String.format("以下 %d 个文件：%s", titles.size(), reason));
            } else {
                result.add(titles.get(0) + ": " + reason);
            }
        }
        if (byReason.size() > 3) {
            result.add("……及其他 " + (byReason.size() - 3) + " 类错误");
        }
        return result;
    }

    /** 是否为系统性错误（Redis/Ollama/Milvus/ES 不可用），遇此类错误应立即中止 */
    private boolean isSystemicError(String msg) {
        if (msg == null) return false;
        String m = msg.toLowerCase();
        return m.contains("milvus") || m.contains("ollama") || m.contains("embedding") || m.contains("嵌入向量")
                || m.contains("elasticsearch") || m.contains("索引") || m.contains("redis")
                || m.contains("connection") || m.contains("connect") || m.contains("refused");
    }

    /** 按关键词归一化错误原因，便于合并 */
    private String normalizeReason(String msg) {
        if (msg == null) return "未知";
        String m = msg.toLowerCase();
        if (m.contains("milvus")) return "Milvus 未启用或连接失败";
        if (m.contains("ollama") || m.contains("embedding") || m.contains("嵌入向量")) return "Ollama 未运行或 Embedding 失败";
        if (m.contains("elasticsearch") || m.contains("索引") || m.contains("es ")) return "Elasticsearch 未运行或索引失败";
        if (m.contains("redis")) return "Redis 未运行（配置读取失败，可启动 Redis 或使用默认配置）";
        if (m.contains("connection") || m.contains("connect") || m.contains("refused")) return "服务连接失败（请检查 Milvus/Ollama/ES）";
        return msg.length() > 80 ? msg.substring(0, 80) + "…" : msg;
    }

    /** 从老文档文件名解析日期，如 evolved_2026-02-15_0253 */
    private LocalDate parseDateFromFilename(String filename) {
        if (filename == null || filename.isBlank()) return null;
        Matcher m = DATE_IN_FILENAME.matcher(filename);
        if (m.find()) {
            try {
                String yyyy = m.group(1);
                String mm = m.group(2);
                String dd = m.group(3);
                return LocalDate.parse(yyyy + "-" + mm + "-" + dd, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException ignored) { }
        }
        return null;
    }

    /** 按标题+内容摘要识别抖音/技术 */
    private String classifyByContent(String title, String content) {
        String text = (title + " " + (content != null ? content.substring(0, Math.min(PREVIEW_LEN, content.length())) : "")).toLowerCase();
        int dy = 0, tech = 0;
        for (String k : DOUYIN_KEYWORDS) {
            if (text.contains(k.toLowerCase())) dy++;
        }
        for (String k : TECH_KEYWORDS) {
            if (text.contains(k.toLowerCase())) tech++;
        }
        return dy >= tech ? "douyin" : "zhishi";
    }

    private Long resolveKbId(Long kbId, String kbName, Long userId) {
        if (kbId != null && kbId > 0) {
            return knowledgeBaseRepository.findByIdAndDeleted(kbId, 0)
                    .filter(kb -> kb.getUserId().equals(userId))
                    .map(AiKnowledgeBase::getId)
                    .orElse(null);
        }
        if (kbName != null && !kbName.isBlank()) {
            return knowledgeBaseRepository.findByUserIdAndKbNameAndDeleted(userId, kbName.trim(), 0)
                    .map(AiKnowledgeBase::getId)
                    .orElse(null);
        }
        return null;
    }
}
