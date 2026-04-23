package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.annotation.CurrentUserId;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.AccountVideoCollectService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectAnalyzeVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectTaskIdVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectTaskSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectTaskSearchVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectTaskVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.AccountCollectVideosQueryVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 账号短视频采集控制器：按账号全量采集视频、深度拆解、入库知识库
 */
@RestController
@RequestMapping("/api/v1/short-video/account-collect")
@Tag(name = "账号短视频采集", description = "指定抖音账号采集全部短视频，拆解后入库知识库")
public class AccountVideoCollectController {

    @Resource
    private AccountVideoCollectService accountVideoCollectService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/start")
    @Operation(summary = "发起采集任务")
    public RESTResult<AccountCollectTaskVO> start(
            @RequestBody @Valid AccountCollectTaskSaveVO vo,
            @CurrentUserId Long userId) {
        AccountCollectTaskVO result = accountVideoCollectService.startCollect(vo, userId);
        RESTResult<AccountCollectTaskVO> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/list")
    @Operation(summary = "任务列表")
    public RESTResult<PageResultVO<AccountCollectTaskVO>> list(
            @RequestBody AccountCollectTaskSearchVO vo,
            @CurrentUserId Long userId) {
        PageResultVO<AccountCollectTaskVO> result = accountVideoCollectService.listTasks(vo, userId);
        RESTResult<PageResultVO<AccountCollectTaskVO>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/status")
    @Operation(summary = "任务状态")
    public RESTResult<AccountCollectTaskVO> status(
            @RequestBody AccountCollectTaskIdVO body,
            @CurrentUserId Long userId) {
        if (body == null || body.getTaskId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "taskId 不能为空");
        }
        AccountCollectTaskVO result = accountVideoCollectService.getTaskStatus(body.getTaskId(), userId);
        RESTResult<AccountCollectTaskVO> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/cancel")
    @Operation(summary = "取消任务")
    public RESTResult<Void> cancel(
            @RequestBody AccountCollectTaskIdVO body,
            @CurrentUserId Long userId) {
        if (body == null || body.getTaskId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "taskId 不能为空");
        }
        accountVideoCollectService.cancelTask(body.getTaskId(), userId);
        // 勿用 getSuccess(null)：会把 null 当成「无数据」返回 204「资源或者信息为空」
        RESTResult<Void> r = RESTResult.success("任务已取消", (Void) null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/retry")
    @Operation(summary = "重试失败任务")
    public RESTResult<AccountCollectTaskVO> retry(
            @RequestBody AccountCollectTaskIdVO body,
            @CurrentUserId Long userId) {
        if (body == null || body.getTaskId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "taskId 不能为空");
        }
        AccountCollectTaskVO result = accountVideoCollectService.retryTask(body.getTaskId(), userId);
        RESTResult<AccountCollectTaskVO> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/analyze-selected")
    @Operation(summary = "对选中视频执行深度分析")
    public RESTResult<AccountCollectTaskVO> analyzeSelected(
            @RequestBody AccountCollectAnalyzeVO body,
            @CurrentUserId Long userId) {
        if (body == null || body.getTaskId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "taskId 不能为空");
        }
        if (CollectionUtils.isEmpty(body.getViralVideoIds())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "请至少选择一个视频");
        }
        AccountCollectTaskVO result = accountVideoCollectService.analyzeSelected(body.getTaskId(), body.getViralVideoIds(), userId);
        RESTResult<AccountCollectTaskVO> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/videos")
    @Operation(summary = "任务下的视频列表")
    public RESTResult<PageResultVO<Map<String, Object>>> videos(
            @RequestBody AccountCollectVideosQueryVO body,
            @CurrentUserId Long userId) {
        if (body == null || body.getTaskId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "taskId 不能为空");
        }
        int page = body.getPage() != null ? body.getPage() : 0;
        int rows = body.getRows() != null ? Math.min(body.getRows(), 100) : 20;
        PageResultVO<Map<String, Object>> result = accountVideoCollectService.listTaskVideos(
                body.getTaskId(), userId, page, rows, body.getSortBy());
        RESTResult<PageResultVO<Map<String, Object>>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除任务（逻辑删除）")
    public RESTResult<Void> delete(
            @RequestBody AccountCollectTaskIdVO body,
            @CurrentUserId Long userId) {
        if (body == null || body.getTaskId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "taskId 不能为空");
        }
        accountVideoCollectService.deleteTask(body.getTaskId(), userId);
        RESTResult<Void> r = RESTResult.success("任务已删除", (Void) null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * AC-10: SSE 实时进度推送 —— 订阅采集任务进度，路径含 -stream 以绕过 OperationLogFilter 缓冲
     */
    @PostMapping(value = "/status-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "任务状态 SSE 实时推送 / Task status SSE stream")
    public SseEmitter statusStream(
            @RequestBody AccountCollectTaskIdVO body,
            @CurrentUserId Long userId) {
        if (body == null || body.getTaskId() == null) {
            SseEmitter err = new SseEmitter(1000L);
            err.completeWithError(new IllegalArgumentException("taskId 不能为空"));
            return err;
        }
        Long taskId = body.getTaskId();
        SseEmitter emitter = new SseEmitter(30L * 60 * 1000); // 30 分钟超时

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                AccountCollectTaskVO status = accountVideoCollectService.getTaskStatus(taskId, userId);
                String json = objectMapper.writeValueAsString(status);
                emitter.send(SseEmitter.event().name("status").data(json));
                // 终态时关闭 SSE
                if ("completed".equals(status.getStatus()) || "failed".equals(status.getStatus())) {
                    emitter.complete();
                    scheduler.shutdown();
                }
            } catch (IOException e) {
                emitter.completeWithError(e);
                scheduler.shutdown();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (IOException ignored) {}
                emitter.complete();
                scheduler.shutdown();
            }
        }, 0, 3, TimeUnit.SECONDS);

        emitter.onCompletion(scheduler::shutdown);
        emitter.onTimeout(scheduler::shutdown);
        emitter.onError(t -> scheduler.shutdown());

        return emitter;
    }
}
