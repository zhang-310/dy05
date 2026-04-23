package cn.gaifan.douyinOperations.module.benchmark.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.benchmark.service.BenchmarkVideoService;
import cn.gaifan.douyinOperations.module.benchmark.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 对标视频管理Controller
 */
@Tag(name = "对标视频管理")
@RestController
@RequestMapping("/api/v1/benchmark/video")
@RequiredArgsConstructor
public class BenchmarkVideoController {

    private final BenchmarkVideoService videoService;

    @Operation(summary = "分页查询视频")
    @PostMapping("/list")
    public RESTResult<PageResultVO<BenchmarkVideoVO>> list(@RequestBody BenchmarkVideoSearchVO searchVO, HttpServletRequest request) {
        Long ownerId = requireCurrentUserId(request);
        PageResultVO<BenchmarkVideoVO> result = videoService.search(searchVO, ownerId);
        return RESTResult.success(result);
    }

    @Operation(summary = "获取视频详情")
    @PostMapping("/get")
    public RESTResult<BenchmarkVideoVO> get(@RequestBody Long id, HttpServletRequest request) {
        Long ownerId = requireCurrentUserId(request);
        BenchmarkVideoVO result = videoService.getById(id, ownerId);
        return RESTResult.success(result);
    }

    @Operation(summary = "保存视频")
    @PostMapping("/save")
    public RESTResult<BenchmarkVideoVO> save(@Valid @RequestBody BenchmarkVideoSaveVO saveVO, HttpServletRequest request) {
        Long ownerId = requireCurrentUserId(request);
        BenchmarkVideoVO result = videoService.save(saveVO, ownerId);
        return RESTResult.success(result);
    }

    @Operation(summary = "删除视频")
    @PostMapping("/delete")
    public RESTResult<Void> delete(@RequestBody Long id, HttpServletRequest request) {
        Long ownerId = requireCurrentUserId(request);
        videoService.delete(id, ownerId);
        return RESTResult.success(null);
    }

    @Operation(summary = "采集账号视频")
    @PostMapping("/collect")
    public RESTResult<List<BenchmarkVideoVO>> collect(@Valid @RequestBody CollectAccountVideosVO collectVO, HttpServletRequest request) {
        Long ownerId = requireCurrentUserId(request);
        List<BenchmarkVideoVO> result = videoService.collectAccountVideos(collectVO, ownerId);
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
