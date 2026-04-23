package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptVersionService;
import cn.gaifan.douyinOperations.module.live.vo.*;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/live/script/version")
public class LiveScriptVersionController {

    @Resource
    private LiveScriptVersionService liveScriptVersionService;

    @PostMapping("/search")
    public RESTResult<PageResultVO<LiveScriptVersionVO>> search(@Valid @RequestBody LiveScriptVersionSearchVO vo) {
        return RESTResult.success(liveScriptVersionService.search(vo));
    }

    @PostMapping("/get")
    public RESTResult<LiveScriptVersionVO> getById(@RequestBody Long id) {
        return RESTResult.success(liveScriptVersionService.getById(id));
    }

    @PostMapping("/save")
    public RESTResult<Long> save(@Valid @RequestBody LiveScriptVersionSaveVO vo) {
        return RESTResult.success(liveScriptVersionService.save(vo));
    }

    @PostMapping("/delete")
    public RESTResult<Void> delete(@RequestBody Long id) {
        liveScriptVersionService.delete(id);
        return RESTResult.success();
    }

    @PostMapping("/getByScriptId")
    public RESTResult<List<LiveScriptVersionVO>> getVersionsByScriptId(@RequestBody Long scriptId) {
        return RESTResult.success(liveScriptVersionService.getVersionsByScriptId(scriptId));
    }

    @PostMapping("/getLatestVersion")
    public RESTResult<LiveScriptVersionVO> getLatestVersion(@RequestBody Long scriptId) {
        return RESTResult.success(liveScriptVersionService.getLatestVersion(scriptId));
    }

    @PostMapping("/diff")
    public RESTResult<VersionDiffVO> diffVersions(@RequestBody VersionDiffRequestVO requestVO) {
        return RESTResult.success(liveScriptVersionService.diffVersions(
                requestVO.getOldVersionId(), requestVO.getNewVersionId()));
    }

    @PostMapping("/setRecommended")
    public RESTResult<Void> setRecommended(@RequestBody SetRecommendedVO vo) {
        liveScriptVersionService.setRecommended(vo.getVersionId(), vo.getRecommendReason());
        return RESTResult.success();
    }

    @PostMapping("/cancelRecommended")
    public RESTResult<Void> cancelRecommended(@RequestBody Long versionId) {
        liveScriptVersionService.cancelRecommended(versionId);
        return RESTResult.success();
    }

    @PostMapping("/getRecommendedVersions")
    public RESTResult<List<LiveScriptVersionVO>> getRecommendedVersions(@RequestBody Long scriptId) {
        return RESTResult.success(liveScriptVersionService.getRecommendedVersions(scriptId));
    }

    @PostMapping("/recommend")
    public RESTResult<List<LiveScriptVersionVO>> recommendVersions(@RequestBody Long scriptId) {
        return RESTResult.success(liveScriptVersionService.recommendVersions(scriptId));
    }

    @PostMapping("/updateStatus")
    public RESTResult<Void> updateVersionStatus(@RequestBody UpdateVersionStatusVO vo) {
        liveScriptVersionService.updateVersionStatus(vo.getVersionId(), vo.getVersionStatus());
        return RESTResult.success();
    }

    @PostMapping("/incrementUsageCount")
    public RESTResult<Void> incrementUsageCount(@RequestBody Long versionId) {
        liveScriptVersionService.incrementUsageCount(versionId);
        return RESTResult.success();
    }

    @PostMapping("/like")
    public RESTResult<Void> incrementLikedCount(@RequestBody Long versionId) {
        liveScriptVersionService.incrementLikedCount(versionId);
        return RESTResult.success();
    }

    @PostMapping("/getLatestByScriptIds")
    public RESTResult<List<LiveScriptVersionVO>> getLatestVersionsByScriptIds(@RequestBody List<Long> scriptIds) {
        return RESTResult.success(liveScriptVersionService.getLatestVersionsByScriptIds(scriptIds));
    }

    @PostMapping("/getUserVersionsBySession")
    public RESTResult<List<LiveScriptVersionVO>> getUserVersionsBySession(@RequestBody UserSessionVersionsVO vo) {
        return RESTResult.success(liveScriptVersionService.getUserVersionsBySession(
                vo.getSessionId(), vo.getOwnerId()));
    }

    @PostMapping("/createFromExisting")
    public RESTResult<Long> createVersionFromExisting(@RequestBody CreateFromExistingVO vo) {
        return RESTResult.success(liveScriptVersionService.createVersionFromExisting(
                vo.getSourceVersionId(), vo.getVersionData()));
    }
}
