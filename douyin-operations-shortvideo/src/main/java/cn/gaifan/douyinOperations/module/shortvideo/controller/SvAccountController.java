package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.annotation.CurrentUserId;
import cn.gaifan.douyinOperations.common.vo.IdVO;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvAccountService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * 短视频账号管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/short-video/account")
@Tag(name = "短视频账号管理", description = "账号列表、详情、统计分析")
public class SvAccountController {

    @Resource
    private SvAccountService accountService;

    @PostMapping("/list")
    @Operation(summary = "账号列表", description = "搜索和筛选账号")
    public RESTResult<PageResultVO<SvAccountVO>> list(
            @RequestBody SvAccountSearchVO vo,
            @CurrentUserId Long userId) {
        return RESTResult.success(accountService.searchAccounts(vo, userId));
    }

    @PostMapping("/get")
    @Operation(summary = "账号详情", description = "获取账号详细信息和统计数据")
    public RESTResult<SvAccountDetailVO> get(
            @RequestBody IdVO vo,
            @CurrentUserId Long userId) {
        return RESTResult.success(accountService.getAccountDetail(vo.getId(), userId));
    }

    @PostMapping("/update")
    @Operation(summary = "更新账号", description = "更新账号标签、分类、备注等")
    public RESTResult<Void> update(
            @RequestBody SvAccountUpdateVO vo,
            @CurrentUserId Long userId) {
        accountService.updateAccount(vo, userId);
        return RESTResult.success();
    }

    @PostMapping("/delete")
    @Operation(summary = "删除账号", description = "逻辑删除账号")
    public RESTResult<Void> delete(
            @RequestBody IdVO vo,
            @CurrentUserId Long userId) {
        accountService.deleteAccount(vo.getId(), userId);
        return RESTResult.success();
    }

    @PostMapping("/videos")
    @Operation(summary = "账号视频列表", description = "获取该账号下所有采集的视频")
    public RESTResult<PageResultVO<ViralVideoVO>> videos(
            @RequestBody AccountVideosQueryVO vo,
            @CurrentUserId Long userId) {
        return RESTResult.success(accountService.getAccountVideos(vo, userId));
    }

    @PostMapping("/analytics")
    @Operation(summary = "账号视频综合分析", description = "基于该账号下已采集视频（爆款库）聚合：总播放/点赞/分享、条均、爆款评分均值、深度分析状态分布等")
    public RESTResult<SvAccountAnalyticsVO> analytics(
            @RequestBody IdVO vo,
            @CurrentUserId Long userId) {
        return RESTResult.success(accountService.getAccountAnalytics(vo.getId(), userId));
    }

    @PostMapping("/refresh-stats")
    @Operation(summary = "刷新账号统计", description = "重新计算账号的统计数据")
    public RESTResult<Void> refreshStats(
            @RequestBody IdVO vo,
            @CurrentUserId Long userId) {
        // 验证权限
        accountService.getAccountDetail(vo.getId(), userId);
        // 更新统计
        accountService.updateAccountStatistics(vo.getId());
        return RESTResult.success();
    }
}
