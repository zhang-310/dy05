package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.live.vo.*;

/**
 * 直播实时辅助面板业务接口
 * 提供话术导航、实时数据管理等功能
 */
public interface LiveRealtimePanelService {

    /**
     * 初始化直播实时面板
     * 查询直播的所有话术段落和实时数据
     *
     * @param liveSessionId 直播场次 ID
     * @param userId        当前用户 ID（用于数据隔离）
     * @return 面板初始化数据
     */
    PanelInitVO initializePanel(Long liveSessionId, Long userId);

    /**
     * 跳转到下一话术段落
     * 将当前段落的 isCurrent 标记为 false，下一段落标记为 true
     *
     * @param liveSessionId 直播场次 ID
     * @param userId        当前用户 ID
     * @return 下一话术段落
     */
    LiveSessionScriptSlotVO nextSlot(Long liveSessionId, Long userId);

    /**
     * 返回到上一话术段落
     * 将当前段落的 isCurrent 标记为 false，上一段落标记为 true
     *
     * @param liveSessionId 直播场次 ID
     * @param userId        当前用户 ID
     * @return 上一话术段落
     */
    LiveSessionScriptSlotVO prevSlot(Long liveSessionId, Long userId);

    /**
     * 跳转到指定序号的话术段落
     *
     * @param liveSessionId 直播场次 ID
     * @param slotIndex     目标段落序号
     * @param userId        当前用户 ID
     * @return 目标话术段落
     */
    LiveSessionScriptSlotVO jumpSlot(Long liveSessionId, Integer slotIndex, Long userId);

    /**
     * 标记话术段落为已完成
     * 设置 isCompleted 为 true，completedAt 为当前时间
     *
     * @param liveSessionId 直播场次 ID
     * @param slotIndex     段落序号
     * @param userId        当前用户 ID
     * @return 已完成的话术段落
     */
    LiveSessionScriptSlotVO completeSlot(Long liveSessionId, Integer slotIndex, Long userId);

    /**
     * 更新实时数据
     *
     * @param data   实时数据保存参数
     * @param userId 当前用户 ID
     * @return 更新后的实时数据
     */
    LiveSessionRealtimeDataVO updateRealtimeData(RealtimeDataSaveVO data, Long userId);

    /**
     * 获取当前话术段落
     *
     * @param liveSessionId 直播场次 ID
     * @param userId        当前用户 ID
     * @return 当前话术段落
     */
    LiveSessionScriptSlotVO getCurrentSlot(Long liveSessionId, Long userId);

    /**
     * 获取实时数据
     *
     * @param liveSessionId 直播场次 ID
     * @param userId        当前用户 ID
     * @return 实时数据
     */
    LiveSessionRealtimeDataVO getRealtimeData(Long liveSessionId, Long userId);

    /**
     * 标记指定话术段落为已完成
     * 仅更新标记，不返回结果
     *
     * @param liveSessionId 直播场次 ID
     * @param slotIndex     段落序号
     * @param userId        当前用户 ID
     * @return 是否操作成功
     */
    boolean markSlotCompleted(Long liveSessionId, Integer slotIndex, Long userId);

    /**
     * 获取场次实时建议（推送服务用）
     * @param liveSessionId 直播场次 ID
     * @param userId 用户 ID
     * @return 建议内容列表（可为空）
     */
    default java.util.List<cn.gaifan.douyinOperations.module.live.vo.RealtimeSuggestionVO> getSuggestionsForSession(Long liveSessionId, Long userId) {
        return java.util.Collections.emptyList();
    }
}
