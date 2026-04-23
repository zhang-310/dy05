package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.annotation.CurrentUserId;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDailyBatch;
import cn.gaifan.douyinOperations.module.shortvideo.service.DailyContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 短视频一键日更 Controller
 */
@RestController
@RequestMapping("/api/v1/short-video/daily")
@Tag(name = "短视频日更 / Daily Content", description = "一键日更批量生成（需登录）")
public class DailyContentController {

    @Resource
    private DailyContentService dailyContentService;

    @PostMapping("/generate-batch")
    @Operation(summary = "生成每日批量内容",
            description = "根据来源类型和数量批量生成短视频内容（需登录）")
    public RESTResult<SvDailyBatch> generateBatch(@CurrentUserId Long userId,
                                                   @RequestBody Map<String, Object> body) {
        Long personaId = parseLong(body, "personaId");
        String sourceType = body != null && body.get("sourceType") instanceof String s ? s : "hot_topic";
        int batchSize = body != null && body.get("batchSize") instanceof Number n ? n.intValue() : 3;

        SvDailyBatch data = dailyContentService.generateBatch(userId, personaId, sourceType, batchSize);
        return withTraceId(RESTResult.addSuccess(data));
    }

    @PostMapping("/batch-status")
    @Operation(summary = "查询批次状态",
            description = "根据批次 ID 查询处理状态（需登录）")
    public RESTResult<SvDailyBatch> batchStatus(@CurrentUserId Long userId,
                                                 @RequestBody Map<String, Object> body) {
        Long batchId = parseLong(body, "batchId");
        if (batchId == null) return withTraceId(RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 batchId"));

        SvDailyBatch data = dailyContentService.getBatchStatus(batchId);
        return withTraceId(RESTResult.getSuccess(data));
    }

    @PostMapping("/batch-list")
    @Operation(summary = "批次历史列表",
            description = "查看当前用户的批量生成历史（需登录，分页）")
    public RESTResult<PageResultVO<SvDailyBatch>> batchList(@CurrentUserId Long userId,
                                                             @RequestBody(required = false) Map<String, Object> body) {
        int page = body != null && body.get("page") instanceof Number n ? n.intValue() : 0;
        int rows = body != null && body.get("rows") instanceof Number n ? n.intValue() : 10;
        PageResultVO<SvDailyBatch> data = dailyContentService.listBatches(userId, page, rows);
        return withTraceId(RESTResult.getSuccess(data));
    }

    private <T> RESTResult<T> withTraceId(RESTResult<T> r) {
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    private static Long parseLong(Map<String, Object> body, String key) {
        if (body == null) return null;
        Object v = body.get(key);
        if (v == null) return null;
        return v instanceof Number n ? n.longValue() : Long.parseLong(v.toString());
    }
}
