package cn.gaifan.douyinOperations.module.benchmark.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.benchmark.service.DouyinCookieQrLoginService;
import cn.gaifan.douyinOperations.module.benchmark.service.DouyinCookieService;
import cn.gaifan.douyinOperations.module.benchmark.vo.DouyinCookieSearchVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.DouyinCookieSaveVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.DouyinCookieVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.DouyinQrLoginPollResultVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.DouyinQrLoginSessionIdVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.DouyinQrLoginStartResultVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.ValidateCookieVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 抖音Cookie管理Controller
 */
@Tag(name = "抖音Cookie管理")
@RestController
@RequestMapping("/api/v1/benchmark/cookie")
@RequiredArgsConstructor
public class DouyinCookieController {

    private final DouyinCookieService cookieService;
    private final DouyinCookieQrLoginService douyinCookieQrLoginService;

    @Operation(summary = "分页查询Cookie")
    @PostMapping("/list")
    public RESTResult<PageResultVO<DouyinCookieVO>> list(@RequestBody DouyinCookieSearchVO searchVO, HttpServletRequest request) {
        Long ownerId = requireCurrentUserId(request);
        PageResultVO<DouyinCookieVO> result = cookieService.search(searchVO, ownerId);
        return RESTResult.success(result);
    }

    @Operation(summary = "获取Cookie详情")
    @PostMapping("/get")
    public RESTResult<DouyinCookieVO> get(@RequestBody Long id, HttpServletRequest request) {
        Long ownerId = requireCurrentUserId(request);
        DouyinCookieVO result = cookieService.getById(id, ownerId);
        return RESTResult.success(result);
    }

    @Operation(summary = "保存Cookie")
    @PostMapping("/save")
    public RESTResult<DouyinCookieVO> save(@Valid @RequestBody DouyinCookieSaveVO saveVO, HttpServletRequest request) {
        Long ownerId = requireCurrentUserId(request);
        DouyinCookieVO result = cookieService.save(saveVO, ownerId);
        return RESTResult.success(result);
    }

    @Operation(summary = "删除Cookie")
    @PostMapping("/delete")
    public RESTResult<Void> delete(@RequestBody Long id, HttpServletRequest request) {
        Long ownerId = requireCurrentUserId(request);
        cookieService.delete(id, ownerId);
        return RESTResult.success(null);
    }

    @Operation(summary = "验证Cookie")
    @PostMapping("/validate")
    public RESTResult<Boolean> validate(@Valid @RequestBody ValidateCookieVO validateVO, HttpServletRequest request) {
        Long ownerId = requireCurrentUserId(request);
        Boolean result = cookieService.validate(validateVO.getCookieId(), ownerId);
        return RESTResult.success(result);
    }

    @Operation(summary = "扫码登录：启动会话并返回页面截图（含二维码）")
    @PostMapping("/qr-login/start")
    public RESTResult<DouyinQrLoginStartResultVO> qrLoginStart(HttpServletRequest request) {
        Long ownerId = requireCurrentUserId(request);
        DouyinQrLoginStartResultVO result = douyinCookieQrLoginService.start(ownerId);
        return RESTResult.success(result);
    }

    @Operation(summary = "扫码登录：轮询是否已登录并获取 Cookie")
    @PostMapping("/qr-login/poll")
    public RESTResult<DouyinQrLoginPollResultVO> qrLoginPoll(
            @Valid @RequestBody DouyinQrLoginSessionIdVO body,
            HttpServletRequest request) {
        Long ownerId = requireCurrentUserId(request);
        DouyinQrLoginPollResultVO result = douyinCookieQrLoginService.poll(body.getSessionId(), ownerId);
        return RESTResult.success(result);
    }

    @Operation(summary = "扫码登录：取消会话")
    @PostMapping("/qr-login/cancel")
    public RESTResult<Void> qrLoginCancel(
            @Valid @RequestBody DouyinQrLoginSessionIdVO body,
            HttpServletRequest request) {
        Long ownerId = requireCurrentUserId(request);
        douyinCookieQrLoginService.cancel(body.getSessionId(), ownerId);
        return RESTResult.success(null);
    }

    private Long requireCurrentUserId(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return userId;
    }
}
