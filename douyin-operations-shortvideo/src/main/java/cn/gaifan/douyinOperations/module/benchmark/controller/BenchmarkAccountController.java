package cn.gaifan.douyinOperations.module.benchmark.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.benchmark.service.BenchmarkAccountService;
import cn.gaifan.douyinOperations.module.benchmark.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 对标账号管理Controller
 */
@Tag(name = "对标账号管理")
@RestController
@RequestMapping("/api/v1/benchmark/account")
@RequiredArgsConstructor
public class BenchmarkAccountController {

    private final BenchmarkAccountService accountService;

    @Operation(summary = "分页查询账号")
    @PostMapping("/list")
    public RESTResult<PageResultVO<BenchmarkAccountVO>> list(@RequestBody BenchmarkAccountSearchVO searchVO, HttpServletRequest request) {
        Long ownerId = requireCurrentUserId(request);
        PageResultVO<BenchmarkAccountVO> result = accountService.search(searchVO, ownerId);
        return RESTResult.success(result);
    }

    @Operation(summary = "获取账号详情")
    @PostMapping("/get")
    public RESTResult<BenchmarkAccountVO> get(@RequestBody Long id, HttpServletRequest request) {
        Long ownerId = requireCurrentUserId(request);
        BenchmarkAccountVO result = accountService.getById(id, ownerId);
        return RESTResult.success(result);
    }

    @Operation(summary = "保存账号")
    @PostMapping("/save")
    public RESTResult<BenchmarkAccountVO> save(@Valid @RequestBody BenchmarkAccountSaveVO saveVO, HttpServletRequest request) {
        Long ownerId = requireCurrentUserId(request);
        BenchmarkAccountVO result = accountService.save(saveVO, ownerId);
        return RESTResult.success(result);
    }

    @Operation(summary = "删除账号")
    @PostMapping("/delete")
    public RESTResult<Void> delete(@RequestBody Long id, HttpServletRequest request) {
        Long ownerId = requireCurrentUserId(request);
        accountService.delete(id, ownerId);
        return RESTResult.success(null);
    }

    @Operation(summary = "按关键词搜索账号")
    @PostMapping("/search-by-keyword")
    public RESTResult<List<BenchmarkAccountVO>> searchByKeyword(@Valid @RequestBody SearchAccountByKeywordVO searchVO, HttpServletRequest request) {
        Long ownerId = requireCurrentUserId(request);
        List<BenchmarkAccountVO> result = accountService.searchByKeyword(searchVO, ownerId);
        return RESTResult.success(result);
    }

    @Operation(summary = "按URL分析账号")
    @PostMapping("/analyze-by-url")
    public RESTResult<BenchmarkAccountVO> analyzeByUrl(@Valid @RequestBody AnalyzeAccountByUrlVO analyzeVO, HttpServletRequest request) {
        Long ownerId = requireCurrentUserId(request);
        BenchmarkAccountVO result = accountService.analyzeByUrl(analyzeVO, ownerId);
        return RESTResult.success(result);
    }

    private Long requireCurrentUserId(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return userId;
    }
}
