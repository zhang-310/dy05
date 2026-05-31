package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.entity.AiKbDocument;
import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase;
import cn.gaifan.douyinOperations.module.ai.entity.KbImportReport;
import cn.gaifan.douyinOperations.module.ai.config.ImportJobStore;
import cn.gaifan.douyinOperations.module.ai.service.ImportProgress;
import cn.gaifan.douyinOperations.module.ai.service.AiAdminInfraService;
import cn.gaifan.douyinOperations.module.ai.service.ImportRequirementsService;
import cn.gaifan.douyinOperations.module.ai.service.EvolutionFitnessService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseImportService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.ai.vo.KbCreateVO;
import cn.gaifan.douyinOperations.module.ai.vo.KbDocumentUploadVO;
import cn.gaifan.douyinOperations.module.ai.vo.KbDocumentSearchVO;
import cn.gaifan.douyinOperations.module.ai.vo.KbFeedbackVO;
import cn.gaifan.douyinOperations.module.ai.vo.KbImportVO;
import cn.gaifan.douyinOperations.module.ai.util.DocumentParser;
import cn.gaifan.douyinOperations.module.ai.util.ContentSecurityScanner;
import cn.gaifan.douyinOperations.module.ai.vo.ChunkItemVO;
import cn.gaifan.douyinOperations.module.ai.vo.DedupPreviewVO;
import cn.gaifan.douyinOperations.module.ai.vo.EvolutionFitnessListVO;
import cn.gaifan.douyinOperations.module.ai.vo.EvolutionFitnessRecordVO;
import cn.gaifan.douyinOperations.module.ai.vo.InfraSearchVO;
import cn.gaifan.douyinOperations.module.ai.vo.KbSearchVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Tag(name = "AI 知识库")
@RestController
@RequestMapping("/api/v1/ai/knowledge-base")
public class KnowledgeBaseController {

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    private AiAdminInfraService aiAdminInfraService;

    @Resource
    private KnowledgeBaseImportService knowledgeBaseImportService;

    @Resource
    private ImportRequirementsService importRequirementsService;

    @Resource
    private ImportJobStore importJobStore;

    @Resource
    private EvolutionFitnessService evolutionFitnessService;

    private static final Set<String> UPLOAD_EXTENSIONS = Set.of("md", "txt", "doc", "docx", "pdf");
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50MB

    @Value("${app.ai.kb.security.scan-on-upload:true}")
    private boolean securityScanOnUpload;

    @Operation(summary = "创建知识库")
    @PostMapping("/create")
    public RESTResult<AiKnowledgeBase> createKnowledgeBase(
            @Valid @RequestBody KbCreateVO vo,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireUserId(httpRequest);
        AiKnowledgeBase kb = knowledgeBaseService.createKnowledgeBase(vo.getName(), vo.getDescription(), userId);
        return RESTResult.getSuccess(kb);
    }

    @Operation(summary = "删除知识库")
    @DeleteMapping("/{kbId:\\d+}")
    public RESTResult<Void> deleteKnowledgeBase(
            @PathVariable Long kbId,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireUserId(httpRequest);

        knowledgeBaseService.deleteKnowledgeBase(kbId, userId);
        return RESTResult.success("删除成功", null);
    }

    @Operation(summary = "获取知识库列表")
    @PostMapping("/list")
    public RESTResult<List<AiKnowledgeBase>> listKnowledgeBases(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest httpRequest) {
        Long userId = requireUserId(httpRequest);

        List<AiKnowledgeBase> list = knowledgeBaseService.listKnowledgeBases(userId);
        return RESTResult.getSuccess(list);
    }

    @Operation(summary = "上传文档")
    @PostMapping("/{kbId:\\d+}/document")
    public RESTResult<AiKbDocument> uploadDocument(
            @PathVariable Long kbId,
            @Valid @RequestBody KbDocumentUploadVO vo,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireUserId(httpRequest);
        String ft = vo.getFileType() != null ? vo.getFileType() : "text";
        AiKbDocument doc = knowledgeBaseService.uploadDocument(kbId, vo.getTitle(), vo.getContent(), ft, userId, null, vo.getContentType());
        return RESTResult.addSuccess(doc);
    }

    @Operation(summary = "删除文档")
    @DeleteMapping("/document/{docId:\\d+}")
    public RESTResult<Void> deleteDocument(
            @PathVariable Long docId,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireUserId(httpRequest);

        knowledgeBaseService.deleteDocument(docId, userId);
        return RESTResult.success("删除成功", null);
    }

    @Operation(summary = "获取文档列表（分页）")
    @PostMapping("/{kbId:\\d+}/documents")
    public RESTResult<PageResultVO<AiKbDocument>> listDocuments(
            @PathVariable Long kbId,
            @RequestBody(required = false) KbDocumentSearchVO vo,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireUserId(httpRequest);
        if (vo == null) vo = new KbDocumentSearchVO();
        vo.validateParams();

        PageResultVO<AiKbDocument> result = knowledgeBaseService.pageDocuments(kbId, userId, vo);
        return RESTResult.getSuccess(result);
    }

    @Operation(summary = "导入报告列表（最近 50 条）")
    @PostMapping("/{kbId:\\d+}/import-reports")
    public RESTResult<List<KbImportReport>> listImportReports(
            @PathVariable Long kbId,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireUserId(httpRequest);
        List<KbImportReport> list = knowledgeBaseService.listImportReports(kbId, userId);
        return RESTResult.getSuccess(list);
    }

    @Operation(summary = "检查文档导入前置依赖")
    @PostMapping("/import-requirements")
    public RESTResult<java.util.Map<String, String>> checkImportRequirements() {
        return RESTResult.getSuccess(importRequirementsService.checkImportRequirements());
    }

    @Operation(summary = "从本地路径导入文档")
    @PostMapping("/import-from-path")
    public RESTResult<KnowledgeBaseImportService.ImportResult> importFromPath(
            @Valid @RequestBody KbImportVO vo,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireUserId(httpRequest);

        // P0-1: 校验路径合法性，防止路径遍历攻击
        String sourcePath = vo.getSourcePath();
        if (sourcePath == null || sourcePath.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "sourcePath 不能为空");
        }

        // 检查路径是否包含 ..
        if (sourcePath.contains("..")) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "路径不能包含 ..");
        }

        // 规范化路径并检查是否在允许的目录内
        java.nio.file.Path normalizedPath = java.nio.file.Paths.get(sourcePath).normalize().toAbsolutePath();
        java.nio.file.Path allowedBasePath = java.nio.file.Paths.get("/data/knowledge-base-imports").toAbsolutePath();

        if (!normalizedPath.startsWith(allowedBasePath)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "路径不在允许的导入目录内");
        }

        boolean ac = Boolean.TRUE.equals(vo.getAutoClassify());
        KnowledgeBaseImportService.ImportResult result =
                knowledgeBaseImportService.importFromPath(normalizedPath.toString(), vo.getKbId(), vo.getKbName(), ac, userId, null);
        return RESTResult.getSuccess(result);
    }

    @Operation(summary = "异步导入（返回 jobId，可轮询 import-status）")
    @PostMapping("/import-from-path-async")
    public RESTResult<Map<String, String>> importFromPathAsync(
            @Valid @RequestBody KbImportVO vo,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireUserId(httpRequest);

        // P0-1: 校验路径合法性，防止路径遍历攻击
        String sourcePath = vo.getSourcePath();
        if (sourcePath == null || sourcePath.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "sourcePath 不能为空");
        }

        // 检查路径是否包含 ..
        if (sourcePath.contains("..")) {
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "路径不能包含 ..");
        }

        // 规范化路径并检查是否在允许的目录内
        java.nio.file.Path normalizedPath = java.nio.file.Paths.get(sourcePath).normalize().toAbsolutePath();
        java.nio.file.Path allowedBasePath = java.nio.file.Paths.get("/data/knowledge-base-imports").toAbsolutePath();

        if (!normalizedPath.startsWith(allowedBasePath)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "路径不在允许的导入目录内");
        }

        String jobId = importJobStore.createJob();
        ImportProgress progress = importJobStore.get(jobId);
        String path = normalizedPath.toString();
        Long kid = vo.getKbId();
        String kbn = vo.getKbName();
        boolean ac = Boolean.TRUE.equals(vo.getAutoClassify());
        Long uid = userId;
        new Thread(() -> {
            try {
                knowledgeBaseImportService.importFromPath(path, kid, kbn, ac, uid, progress);
            } catch (Exception e) {
                if (progress != null) {
                    progress.error(e.getMessage());
                }
            }
        }, "kb-import-" + jobId).start();
        return RESTResult.getSuccess(Map.of("jobId", jobId));
    }

    @Operation(summary = "获取正在运行的导入任务 jobId，供前端恢复进度")
    @PostMapping("/import-active-jobs")
    public RESTResult<Map<String, Object>> getActiveImportJobs(@RequestBody(required = false) Map<String, Object> body) {
        List<String> ids = importJobStore.listActiveJobIds();
        return RESTResult.getSuccess(Map.of("jobIds", ids));
    }

    @Operation(summary = "查询导入进度")
    @PostMapping("/import-status/{jobId}")
    public RESTResult<Map<String, Object>> getImportStatus(@PathVariable String jobId, @RequestBody(required = false) Map<String, Object> body) {
        ImportProgress p = importJobStore.get(jobId);
        if (p == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在或已过期");
        }
        Map<String, Object> out = new java.util.HashMap<>();
        out.put("phase", p.getPhase());
        out.put("processed", p.getProcessed());
        out.put("total", p.getTotal());
        out.put("success", p.getSuccess());
        out.put("failed", p.getFailed());
        out.put("logs", p.getLogs());
        if (p.getResult() != null) {
            out.put("result", p.getResult());
        }
        return RESTResult.getSuccess(out);
    }

    @Operation(summary = "提交知识反馈（有用/无用），更新 boost_factor")
    @PostMapping("/feedback")
    public RESTResult<Void> submitFeedback(
            @Valid @RequestBody KbFeedbackVO vo,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireUserId(httpRequest);
        knowledgeBaseService.submitFeedback(
                vo.getDocId(), vo.getQuery(), vo.getRating(),
                vo.getComment(), vo.getSearchMode(), userId);
        return RESTResult.success("反馈已提交", null);
    }

    @Operation(summary = "混合搜索")
    @PostMapping("/{kbId:\\d+}/search")
    public RESTResult<List<KnowledgeBaseService.SearchResult>> search(
            @PathVariable Long kbId,
            @Valid @RequestBody KbSearchVO vo,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireUserId(httpRequest);

        // P0-2: 防止 Prompt 注入攻击
        String query = cn.gaifan.douyinOperations.common.util.PromptInjectionDetector.sanitize(vo.getQuery());
        if (cn.gaifan.douyinOperations.common.util.PromptInjectionDetector.isSuspicious(query)) {
            org.slf4j.LoggerFactory.getLogger(KnowledgeBaseController.class)
                .warn("检测到疑似 Prompt 注入: userId={}, query={}", userId, query);
            throw new BusinessException(ErrorCode.INVALID_PARAMS, "输入包含不安全内容");
        }

        int topK = vo.getTopK() != null ? vo.getTopK() : 10;
        boolean skipQueryRewrite = !vo.isQueryRewrite();
        List<KnowledgeBaseService.SearchResult> results = knowledgeBaseService.hybridSearch(
                kbId, query, topK, userId, null, false, skipQueryRewrite);
        return RESTResult.getSuccess(results);
    }

    @Operation(summary = "文件上传（单文件，支持 doc/docx/pdf/md/txt）")
    @PostMapping("/{kbId:\\d+}/upload-file")
    public RESTResult<AiKbDocument> uploadFile(
            @PathVariable Long kbId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "contentType", required = false, defaultValue = "auto") String contentType,
            HttpServletRequest httpRequest
    ) throws IOException {
        Long userId = requireUserId(httpRequest);
        if (file == null || file.isEmpty()) throw new BusinessException(ErrorCode.INVALID_PARAMS, "请选择文件");
        if (file.getSize() > MAX_FILE_SIZE) throw new BusinessException(ErrorCode.INVALID_PARAMS, "文件大小超过 50MB 限制");
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) throw new BusinessException(ErrorCode.INVALID_PARAMS, "文件名无效");
        String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!UPLOAD_EXTENSIONS.contains(ext)) throw new BusinessException(ErrorCode.INVALID_PARAMS, "仅支持 .md .txt .doc .docx .pdf");
        String content = DocumentParser.parse(file.getInputStream(), ext);
        if (content == null || content.isBlank()) throw new BusinessException(ErrorCode.INVALID_PARAMS, "文件内容为空或解析失败");
        if (securityScanOnUpload) {
            List<ContentSecurityScanner.SecurityWarning> warnings = ContentSecurityScanner.scan(content, filename);
            if (!warnings.isEmpty()) {
                for (ContentSecurityScanner.SecurityWarning w : warnings) {
                    org.slf4j.LoggerFactory.getLogger(KnowledgeBaseController.class).warn("上传安全扫描: {} - {}", filename, w.message());
                }
            }
        }
        String title = filename.lastIndexOf('.') > 0 ? filename.substring(0, filename.lastIndexOf('.')) : filename;
        AiKbDocument doc = knowledgeBaseService.uploadDocument(kbId, title, content, ext, userId, null, contentType);
        return RESTResult.addSuccess(doc);
    }

    @Operation(summary = "批量文件上传")
    @PostMapping("/{kbId:\\d+}/upload-files")
    public RESTResult<Map<String, Object>> uploadFiles(
            @PathVariable Long kbId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "contentType", required = false, defaultValue = "auto") String contentType,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireUserId(httpRequest);
        if (files == null || files.length == 0) throw new BusinessException(ErrorCode.INVALID_PARAMS, "请选择文件");
        List<AiKbDocument> success = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                RESTResult<AiKbDocument> r = uploadFile(kbId, file, contentType, httpRequest);
                if (r.getData() != null) success.add(r.getData());
            } catch (Exception e) {
                errors.add((file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown") + ": " + e.getMessage());
            }
        }
        Map<String, Object> out = new java.util.HashMap<>();
        out.put("successCount", success.size());
        out.put("failedCount", errors.size());
        out.put("documents", success);
        out.put("errors", errors);
        return RESTResult.getSuccess(out);
    }

    @Operation(summary = "索引队列分页（当前用户对该知识库有权限；与运维接口数据一致）")
    @PostMapping("/{kbId:\\d+}/index-queue/list")
    public RESTResult<PageResultVO<Map<String, Object>>> listIndexQueueForKb(
            @PathVariable Long kbId,
            @RequestBody(required = false) InfraSearchVO body,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireUserId(httpRequest);
        knowledgeBaseService.assertKbOwnership(kbId, userId);
        int page = body != null && body.getPage() != null ? body.getPage() : 0;
        int rows = body != null && body.getRows() != null ? Math.min(body.getRows(), 100) : 50;
        String status = body != null ? body.getStatus() : null;
        String keyword = body != null ? body.getKeyword() : null;
        PageResultVO<Map<String, Object>> result = aiAdminInfraService.pageIndexQueue(kbId, status, keyword, page, rows);
        return RESTResult.getSuccess(result);
    }

    @Operation(summary = "进化适应度记录分页（当前用户对该知识库有权限）")
    @PostMapping("/{kbId:\\d+}/evolution-fitness/list")
    public RESTResult<PageResultVO<EvolutionFitnessRecordVO>> listEvolutionFitness(
            @PathVariable Long kbId,
            @RequestBody(required = false) EvolutionFitnessListVO body,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireUserId(httpRequest);
        EvolutionFitnessListVO vo = body != null ? body : new EvolutionFitnessListVO();
        PageResultVO<EvolutionFitnessRecordVO> result = evolutionFitnessService.listForKb(userId, kbId, vo);
        return RESTResult.getSuccess(result);
    }

    @Operation(summary = "去重预览（dry-run，不入库）")
    @PostMapping("/{kbId:\\d+}/dedup-preview")
    public RESTResult<DedupPreviewVO> dedupPreview(
            @PathVariable Long kbId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireUserId(httpRequest);
        String content = body != null && body.get("content") != null ? body.get("content").toString() : null;
        if (content == null || content.isBlank()) throw new BusinessException(ErrorCode.INVALID_PARAMS, "content 不能为空");
        String contentType = body != null && body.get("contentType") != null ? body.get("contentType").toString() : "auto";
        DedupPreviewVO vo = knowledgeBaseService.dedupPreview(kbId, content, contentType, userId);
        return RESTResult.getSuccess(vo);
    }

    @Operation(summary = "文档分块列表（按当前策略重算，供前端预览）")
    @PostMapping("/{kbId:\\d+}/documents/{docId:\\d+}/chunks")
    public RESTResult<List<ChunkItemVO>> getDocumentChunks(
            @PathVariable Long kbId,
            @PathVariable Long docId,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireUserId(httpRequest);
        List<ChunkItemVO> chunks = knowledgeBaseService.getDocumentChunks(kbId, docId, userId);
        return RESTResult.getSuccess(chunks);
    }

    @Operation(summary = "增量导入（仅处理上次导入后新增/修改的文件）")
    @PostMapping("/import-incremental")
    public RESTResult<KnowledgeBaseImportService.ImportResult> importIncremental(
            @Valid @RequestBody KbImportVO vo,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireUserId(httpRequest);
        if (vo.getSourcePath() == null || vo.getSourcePath().isBlank()) throw new BusinessException(ErrorCode.INVALID_PARAMS, "sourcePath 不能为空");
        Long kbId = vo.getKbId() != null ? vo.getKbId() : (vo.getKbName() != null ? knowledgeBaseService.resolveKbIdByName(userId, vo.getKbName()) : null);
        if (kbId == null) throw new BusinessException(ErrorCode.INVALID_PARAMS, "请指定 kbId 或 kbName");
        KnowledgeBaseImportService.ImportResult result = knowledgeBaseImportService.importIncremental(vo.getSourcePath(), kbId, userId, null);
        return RESTResult.getSuccess(result);
    }

    private Long requireUserId(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return userId;
    }
}
