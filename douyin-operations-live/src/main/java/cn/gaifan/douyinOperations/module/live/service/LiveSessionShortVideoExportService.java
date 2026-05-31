package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.live.vo.LiveSessionExportToShortVideoResultVO;

/**
 * 将直播场次话术导出为短视频脚本 + 项目（CONTENT-04）
 */
public interface LiveSessionShortVideoExportService {

    /**
     * @param liveSessionId 场次 ID
     * @param sessionOwnerId  场次归属 userId（用于写 sv_script/sv_project.owner_id）
     */
    LiveSessionExportToShortVideoResultVO exportToShortVideoProject(Long liveSessionId, Long sessionOwnerId);

    /**
     * @param style 导出为短视频脚本时使用的风格，可为空
     */
    default LiveSessionExportToShortVideoResultVO exportToShortVideoProject(Long liveSessionId, Long sessionOwnerId, String style) {
        return exportToShortVideoProject(liveSessionId, sessionOwnerId);
    }
}
