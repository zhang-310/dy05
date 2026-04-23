package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveProductService;
import cn.gaifan.douyinOperations.module.live.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 直播产品管理 Controller
 * Live Product Management Controller
 */
@RestController
@RequestMapping("/api/v1/live/product")
@Tag(name = "直播产品 / Live Product", description = "直播间产品的管理（需登录）")
public class LiveProductController {

    @Resource
    private LiveProductService liveProductService;
    @Resource
    private DataScopeResolver dataScopeService;
    @Resource
    private LiveSessionRepository liveSessionRepository;

    @PostMapping("/search")
    @Operation(
            summary = "查询直播产品 / Search Live Products",
            description = "分页查询直播产品（需登录，非管理员仅能查看自己场次的产品） / Search and paginate live products (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "查询成功 / Search successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<PageResultVO<LiveProductVO>> search(HttpServletRequest request,
                                                          @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                                  description = "查询条件 / Search criteria",
                                                                  required = false
                                                          )
                                                          @RequestBody(required = false) LiveProductSearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (vo == null) vo = new LiveProductSearchVO();
        // 数据范围：按角色限制可见场次
        String roleCode = AuthTokenFilter.getRoleCode(request);
        List<Long> visibleUserIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        if (visibleUserIds != null && vo.getSessionId() == null) {
            List<Long> visibleSessionIds = liveSessionRepository.findIdsByUserIdIn(visibleUserIds);
            vo.setSessionIds(visibleSessionIds);
        }
        PageResultVO<LiveProductVO> data = liveProductService.search(vo);
        RESTResult<PageResultVO<LiveProductVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(
            summary = "获取产品详情 / Get Product Details",
            description = "根据产品 ID 获取产品详细信息（需登录） / Get product details by ID (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "获取成功 / Get successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "404", description = "产品不存在 / Product not found"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<LiveProductVO> get(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long id = parseId(body);
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        LiveProductVO data = liveProductService.getById(id);
        RESTResult<LiveProductVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(
            summary = "保存产品 / Save Product",
            description = "创建或更新直播产品信息（需登录） / Create or update live product (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "保存成功 / Save successful"),
            @ApiResponse(responseCode = "400", description = "请求参数错误 / Invalid request parameters"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Long> save(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "产品信息 / Product information",
            required = true
    ) @RequestBody LiveProductSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        long id = liveProductService.save(vo);
        RESTResult<Long> r = RESTResult.addSuccess(id);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(
            summary = "删除产品 / Delete Product",
            description = "根据产品 ID 删除直播产品（需登录） / Delete live product by ID (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "删除成功 / Delete successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "404", description = "产品不存在 / Product not found"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Void> delete(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long id = parseId(body);
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        liveProductService.delete(id);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    private static Long parseId(java.util.Map<String, Object> body) {
        if (body == null) return null;
        Object v = body.get("id");
        if (v == null) return null;
        return v instanceof Number ? ((Number) v).longValue() : Long.parseLong(v.toString());
    }

    @PostMapping("/by-session")
    @Operation(
            summary = "获取场次的产品列表 / Get Products by Session",
            description = "获取指定直播场次下的所有产品列表（需登录） / Get all products in specified live session (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "获取成功 / Get successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "404", description = "场次不存在 / Session not found"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<List<LiveProductVO>> getBySessionId(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Object sid = body != null ? body.get("sessionId") : null;
        if (sid == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 sessionId");
        Long sessionId = sid instanceof Number ? ((Number) sid).longValue() : Long.parseLong(sid.toString());
        List<LiveProductVO> data = liveProductService.getBySessionId(sessionId);
        RESTResult<List<LiveProductVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/batch-sort")
    @Operation(
            summary = "批量排序产品 / Batch Sort Products",
            description = "按 id 列表顺序更新产品 position（需登录） / Update product positions by ordered id list (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "排序成功 / Sort successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Void> batchSort(HttpServletRequest request,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "sessionId + productIds 有序列表",
                    required = true
            ) @RequestBody Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long sessionId = body != null && body.get("sessionId") != null ? ((Number) body.get("sessionId")).longValue() : null;
        @SuppressWarnings("unchecked")
        List<Number> ids = body != null && body.get("productIds") instanceof List ? (List<Number>) body.get("productIds") : null;
        if (sessionId == null || ids == null || ids.isEmpty()) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "sessionId 和 productIds 不能为空");
        }
        List<Long> productIds = ids.stream().map(Number::longValue).toList();
        liveProductService.batchSort(sessionId, productIds);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
