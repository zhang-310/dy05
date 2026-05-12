package cn.gaifan.douyinOperations.module.copy.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.copy.service.CopyApprovalService;
import cn.gaifan.douyinOperations.module.copy.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/copy/approval")
@Tag(name = "文案审批 / Copy Approval", description = "文案审批管理（需登录）")
public class CopyApprovalController {

    @Resource
    private CopyApprovalService copyApprovalService;

    @PostMapping("/search")
    @Operation(summary = "分页搜索审批记录")
    public RESTResult<PageResultVO<CopyApprovalVO>> search(HttpServletRequest request,
            @RequestBody(required = false) CopyApprovalSearchVO vo) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo == null) vo = new CopyApprovalSearchVO();
        RESTResult<PageResultVO<CopyApprovalVO>> r = RESTResult.getSuccess(copyApprovalService.search(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(summary = "获取审批详情")
    public RESTResult<CopyApprovalVO> get(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long id = parseLong(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<CopyApprovalVO> r = RESTResult.getSuccess(copyApprovalService.getById(id));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(summary = "提交/更新审批")
    public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody CopyApprovalSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        // P1-3: 审批状态转换校验 - 防止绕过审批权限
        if (vo.getId() != null && vo.getId() > 0 && vo.getApprovalStatus() != null) {
            // 更新场景：检查状态转换是否合法
            CopyApprovalVO existing = copyApprovalService.getById(vo.getId());
            Integer oldStatus = existing.getApprovalStatus();
            Integer newStatus = vo.getApprovalStatus();

            // 从待审核(2)转换到通过(1)或拒绝(0)，必须是管理员
            if (oldStatus != null && oldStatus == 2 && (newStatus == 0 || newStatus == 1)) {
                if (!"admin".equalsIgnoreCase(AuthTokenFilter.getRoleCode(request))) {
                    return RESTResult.error(ErrorCode.FORBIDDEN, "仅管理员可执行审批通过/拒绝");
                }
            }
        }

        // 新建场景：直接设置通过(1)/拒绝(0)也需要管理员权限
        if ((vo.getId() == null || vo.getId() <= 0) && vo.getApprovalStatus() != null
                && (vo.getApprovalStatus() == 0 || vo.getApprovalStatus() == 1)) {
            if (!"admin".equalsIgnoreCase(AuthTokenFilter.getRoleCode(request))) {
                return RESTResult.error(ErrorCode.FORBIDDEN, "仅管理员可执行审批通过/拒绝");
            }
        }

        if (vo.getUserId() == null) vo.setUserId(userId);
        RESTResult<Long> r = RESTResult.addSuccess(copyApprovalService.save(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除审批记录（admin）")
    public RESTResult<Void> delete(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long id = parseLong(body, "id");
        if (id == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 id");
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        copyApprovalService.delete(id);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    private static Long parseLong(java.util.Map<String, Object> body, String key) {
        if (body == null) return null;
        Object v = body.get(key);
        if (v == null) return null;
        return v instanceof Number ? ((Number) v).longValue() : Long.parseLong(v.toString());
    }
}
