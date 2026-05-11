package cn.gaifan.douyinOperations.module.douyin.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.contract.auth.DataScopeResolver;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.douyin.service.DouyinAccountService;
import cn.gaifan.douyinOperations.module.douyin.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * 抖音账号管理 Controller
 * Douyin Account Management Controller
 */
@RestController
@RequestMapping("/api/v1/douyin/account")
@Tag(name = "抖音账号 / Douyin Account", description = "抖音账号的管理（需登录）")
public class DouyinAccountController {

    @Resource
    private DouyinAccountService douyinAccountService;
    @Resource
    private DataScopeResolver dataScopeService;

    private boolean isAuthenticated(HttpServletRequest request) {
        return AuthTokenFilter.getUserId(request) != null;
    }

    @PostMapping("/search")
    @Operation(
            summary = "查询抖音账号 / Search Douyin Accounts",
            description = "分页查询抖音账号（需登录，非管理员仅能查看自己的账号） / Search and paginate Douyin accounts (authenticated users can only see their own)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "查询成功 / Search successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<PageResultVO<DouyinAccountVO>> search(HttpServletRequest request, @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "查询条件 / Search criteria",
            required = false
    ) @RequestBody(required = false) DouyinAccountSearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (vo == null) vo = new DouyinAccountSearchVO();
        // 数据范围：按角色限制可见用户
        String roleCode = AuthTokenFilter.getRoleCode(request);
        java.util.List<Long> visibleIds = dataScopeService.getVisibleUserIds(userId, roleCode);
        if (visibleIds != null) {
            vo.setOwnerIds(visibleIds);
        }
        PageResultVO<DouyinAccountVO> data = douyinAccountService.search(vo);
        RESTResult<PageResultVO<DouyinAccountVO>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/get")
    @Operation(
            summary = "获取抖音账号详情 / Get Douyin Account Details",
            description = "根据账号 ID 获取抖音账号详细信息（需登录） / Get Douyin account details by ID (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "获取成功 / Get successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "404", description = "账号不存在 / Account not found"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<DouyinAccountVO> get(HttpServletRequest request, @Parameter(
            description = "账号 ID / Account ID",
            required = true
    ) @RequestParam Long id) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        DouyinAccountVO data = douyinAccountService.getAccount(id);
        // 权限校验：非管理员只能查看自己的账号
        String roleCode = AuthTokenFilter.getRoleCode(request);
        if (!"admin".equals(roleCode) && !userId.equals(data.getOwnerId())) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限访问此账号");
        }
        RESTResult<DouyinAccountVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(
            summary = "保存抖音账号 / Save Douyin Account",
            description = "创建或更新抖音账号信息（需登录） / Create or update Douyin account (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "保存成功 / Save successful"),
            @ApiResponse(responseCode = "400", description = "请求参数错误 / Invalid request parameters"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Long> save(HttpServletRequest request, @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "账号信息 / Account information",
            required = true
    ) @RequestBody DouyinAccountSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        vo.setOwnerId(userId);
        long id = douyinAccountService.saveAccount(vo);
        RESTResult<Long> r = RESTResult.addSuccess(id);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(
            summary = "删除抖音账号 / Delete Douyin Account",
            description = "根据账号 ID 删除抖音账号（需登录） / Delete Douyin account by ID (authentication required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "删除成功 / Delete successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "404", description = "账号不存在 / Account not found"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<Void> delete(HttpServletRequest request, @Parameter(
            description = "账号 ID / Account ID",
            required = true
    ) @RequestParam Long id) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        // 权限校验：只有账号所有者或管理员可删除
        DouyinAccountVO account = douyinAccountService.getAccount(id);
        String roleCode = AuthTokenFilter.getRoleCode(request);
        if (!"admin".equals(roleCode) && !userId.equals(account.getOwnerId())) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限删除此账号");
        }
        douyinAccountService.deleteAccount(id);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/statistics")
    @Operation(
            summary = "获取账号统计信息 / Get Account Statistics",
            description = "获取抖音账号的统计数据（如粉丝数、视频数等）（需登录） / Get Douyin account statistics (followers, video count, etc.)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "0", description = "获取成功 / Get successful"),
            @ApiResponse(responseCode = "401", description = "未登录 / Not logged in"),
            @ApiResponse(responseCode = "404", description = "账号不存在 / Account not found"),
            @ApiResponse(responseCode = "500", description = "服务器错误 / Server Error")
    })
    public RESTResult<DouyinAccountStatisticsVO> statistics(HttpServletRequest request, @Parameter(
            description = "账号 ID / Account ID",
            required = true
    ) @RequestParam Long id) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        // 权限校验：只有账号所有者或管理员可查看统计
        DouyinAccountVO account = douyinAccountService.getAccount(id);
        String roleCode = AuthTokenFilter.getRoleCode(request);
        if (!"admin".equals(roleCode) && !userId.equals(account.getOwnerId())) {
            return RESTResult.error(ErrorCode.FORBIDDEN, "无权限查看此账号统计");
        }
        DouyinAccountStatisticsVO data = douyinAccountService.getAccountStatistics(id);
        RESTResult<DouyinAccountStatisticsVO> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
