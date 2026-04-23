package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.live.vo.*;

/**
 * 直播场次服务接口
 */
public interface LiveSessionService {

    /**
     * 搜索直播场次列表
     */
    PageResultVO<LiveSessionVO> search(LiveSessionSearchVO vo);

    /**
     * 获取直播场次详情
     */
    LiveSessionVO getById(Long id);

    /**
     * 保存直播场次（新增或更新）
     */
    long save(LiveSessionSaveVO vo);

    /**
     * 删除直播场次
     */
    void delete(Long id);

    /**
     * 更新直播状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 更新观看人数
     */
    void updateViewers(Long id, Integer viewers);

    /**
     * 更新点赞数
     */
    void updateLikes(Long id, Long likes);

    /**
     * 按数据范围校验并获取场次（不存在或不在可见范围内则抛异常）
     */
    LiveSessionVO getByIdWithScope(Long id, java.util.List<Long> visibleUserIds);

    /**
     * 场次数据概览（产品/话术等汇总）
     */
    LiveSessionOverviewVO getOverview(Long id);

    /**
     * 开播准备清单（产品/人设/话术/合规检查）
     */
    LiveReadinessVO getReadiness(Long id);

    /**
     * 直播历史趋势分析
     */
    LiveTrendResultVO analyzeTrend(LiveTrendRequestVO vo, Long userId);

    /**
     * 克隆直播场次（复制基础信息、商品、话术）
     */
    default long cloneSession(Long sessionId, Long userId, String newTitle) {
        // 默认实现，由 ServiceImpl 覆盖
        throw new cn.gaifan.douyinOperations.common.exception.BusinessException(
                cn.gaifan.douyinOperations.common.constant.ErrorCode.SYSTEM_ERROR, "克隆功能暂未实现");
    }

    /**
     * 导出场次至短视频项目（根据话术和商品生成短视频脚本项目）
     */
    default long exportToShortVideo(Long sessionId, Long userId) {
        throw new cn.gaifan.douyinOperations.common.exception.BusinessException(
                cn.gaifan.douyinOperations.common.constant.ErrorCode.SYSTEM_ERROR, "导出功能暂未实现");
    }
}
