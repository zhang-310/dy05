package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.vo.*;
import java.util.List;

/**
 * 直播话术版本服务接口
 */
public interface LiveScriptVersionService {

    PageResultVO<LiveScriptVersionVO> search(LiveScriptVersionSearchVO vo);
    LiveScriptVersionVO getById(Long id);
    long save(LiveScriptVersionSaveVO vo);
    void delete(Long id);
    List<LiveScriptVersionVO> getVersionsByScriptId(Long scriptId);
    LiveScriptVersionVO getLatestVersion(Long scriptId);
    VersionDiffVO diffVersions(Long oldVersionId, Long newVersionId);
    void setRecommended(Long versionId, String recommendReason);
    void cancelRecommended(Long versionId);
    List<LiveScriptVersionVO> getRecommendedVersions(Long scriptId);
    List<LiveScriptVersionVO> recommendVersions(Long scriptId);
    void updateVersionStatus(Long versionId, String versionStatus);
    void incrementUsageCount(Long versionId);
    void incrementLikedCount(Long versionId);
    List<LiveScriptVersionVO> getLatestVersionsByScriptIds(List<Long> scriptIds);
    List<LiveScriptVersionVO> getUserVersionsBySession(Long sessionId, Long ownerId);
    long createVersionFromExisting(Long sourceVersionId, LiveScriptVersionSaveVO vo);
}
