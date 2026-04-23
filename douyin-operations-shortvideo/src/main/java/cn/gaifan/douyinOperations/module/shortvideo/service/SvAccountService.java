package cn.gaifan.douyinOperations.module.shortvideo.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvAccount;
import cn.gaifan.douyinOperations.module.shortvideo.vo.*;

/**
 * 短视频账号管理服务
 */
public interface SvAccountService {

    /**
     * 搜索账号列表
     */
    PageResultVO<SvAccountVO> searchAccounts(SvAccountSearchVO vo, Long userId);

    /**
     * 获取账号详情
     */
    SvAccountDetailVO getAccountDetail(Long id, Long userId);

    /**
     * 更新账号信息
     */
    void updateAccount(SvAccountUpdateVO vo, Long userId);

    /**
     * 删除账号
     */
    void deleteAccount(Long id, Long userId);

    /**
     * 查找或创建账号（内部方法，供采集服务调用）
     */
    SvAccount findOrCreateAccount(String secUid, String nickname, String sourceType,
                                   String sourceKeyword, Long sourceTaskId, Long userId);

    /**
     * 更新账号统计数据（内部方法，采集完成后调用）
     */
    void updateAccountStatistics(Long accountId);

    /**
     * 获取账号的视频列表（分页、可选关键词/深度状态筛选与排序）
     */
    PageResultVO<ViralVideoVO> getAccountVideos(AccountVideosQueryVO query, Long userId);

    /**
     * 基于该账号下全部已采集视频的聚合分析（播放/点赞/深度分析状态分布等）
     */
    SvAccountAnalyticsVO getAccountAnalytics(Long accountId, Long userId);
}
