package cn.gaifan.douyinOperations.module.script.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.script.service.UserViolationWordService;
import cn.gaifan.douyinOperations.module.script.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户自定义违规词管理 Controller
 */
@RestController
@RequestMapping("/api/v1/script/user-violation")
@Tag(name = "用户违规词 / User Violation Word", description = "用户自定义违规词管理（需登录）")
public class UserViolationWordController {

    @Resource
    private UserViolationWordService userViolationWordService;

    @PostMapping("/search")
    @Operation(summary = "查询用户违规词 / Search User Violation Words")
    public RESTResult<PageResultVO<UserViolationWordVO>> search(HttpServletRequest request,
                                                                 @RequestBody(required = false) UserViolationWordSearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo == null) vo = new UserViolationWordSearchVO();
        if (vo.getUserId() == null) vo.setUserId(userId);
        PageResultVO<UserViolationWordVO> data = userViolationWordService.search(vo);
        RESTResult<PageResultVO<UserViolationWordVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(summary = "获取违规词详情 / Get Violation Word Details")
    public RESTResult<UserViolationWordVO> get(HttpServletRequest request,
                                                @Parameter(description = "违规词 ID", required = true) @RequestParam Long id) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        UserViolationWordVO data = userViolationWordService.getById(id);
        RESTResult<UserViolationWordVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(summary = "保存用户违规词 / Save User Violation Word")
    public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody UserViolationWordSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo.getUserId() == null) vo.setUserId(userId);
        long id = userViolationWordService.save(vo);
        RESTResult<Long> r = RESTResult.addSuccess(id);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除用户违规词 / Delete User Violation Word")
    public RESTResult<Void> delete(HttpServletRequest request,
                                    @Parameter(description = "违规词 ID", required = true) @RequestParam Long id) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        userViolationWordService.delete(id);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/active")
    @Operation(summary = "获取当前用户有效违规词 / List Active Violation Words")
    public RESTResult<List<UserViolationWordVO>> listActive(HttpServletRequest request,
            @RequestBody(required = false) java.util.Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        List<UserViolationWordVO> data = userViolationWordService.listActiveByUserId(userId);
        RESTResult<List<UserViolationWordVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
