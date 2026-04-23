package cn.gaifan.douyinOperations.module.storage.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.storage.service.UploadService;
import cn.gaifan.douyinOperations.module.storage.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/storage/upload")
@Tag(name = "文件存储 - 分块上传", description = "支持断点续传的分块上传功能（需登录）")
public class UploadController {

    @Resource
    private UploadService uploadService;

    @PostMapping("/init")
    @Operation(summary = "初始化上传")
    public RESTResult<UploadInitResultVO> initUpload(
            HttpServletRequest request,
            @Valid @RequestBody UploadInitVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        RESTResult<UploadInitResultVO> r = RESTResult.addSuccess(uploadService.initUpload(userId, vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/chunk")
    @Operation(summary = "上传分块")
    public RESTResult<Void> uploadChunk(
            HttpServletRequest request,
            @RequestParam String uploadId,
            @RequestParam Integer chunkIndex,
            @RequestParam String chunkMd5,
            @RequestParam("chunk") MultipartFile chunkFile) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        uploadService.uploadChunk(userId, uploadId, chunkIndex, chunkMd5, chunkFile);

        RESTResult<Void> r = RESTResult.addSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @GetMapping("/chunks")
    @Operation(summary = "查询已上传分块列表")
    public RESTResult<List<Integer>> getUploadedChunks(
            HttpServletRequest request,
            @RequestParam String uploadId) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        RESTResult<List<Integer>> r = RESTResult.getSuccess(uploadService.getUploadedChunks(userId, uploadId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @GetMapping("/progress")
    @Operation(summary = "查询上传进度")
    public RESTResult<UploadProgressVO> getProgress(
            HttpServletRequest request,
            @RequestParam String uploadId) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        RESTResult<UploadProgressVO> r = RESTResult.getSuccess(uploadService.getProgress(userId, uploadId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/complete")
    @Operation(summary = "完成上传（触发合并）")
    public RESTResult<UploadCompleteResultVO> completeUpload(
            HttpServletRequest request,
            @Valid @RequestBody UploadCompleteVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        RESTResult<UploadCompleteResultVO> r = RESTResult.addSuccess(
            uploadService.completeUpload(userId, vo.getUploadId())
        );
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消上传")
    public RESTResult<Void> cancelUpload(
            HttpServletRequest request,
            @RequestParam String uploadId) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        uploadService.cancelUpload(userId, uploadId);

        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
