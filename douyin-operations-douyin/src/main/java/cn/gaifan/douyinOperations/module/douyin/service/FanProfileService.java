package cn.gaifan.douyinOperations.module.douyin.service;

import cn.gaifan.douyinOperations.module.douyin.entity.DyFanProfile;
import cn.gaifan.douyinOperations.module.douyin.entity.DyFanProfileStats;
import cn.gaifan.douyinOperations.module.douyin.vo.FanProfileVO;

import java.util.List;

/**
 * 粉丝画像服务
 */
public interface FanProfileService {

    /**
     * 同步粉丝画像数据
     */
    void syncFanProfile(Long accountId);

    /**
     * 获取账号的粉丝画像
     */
    FanProfileVO getFanProfile(Long accountId, Long userId);

    /**
     * 获取账号的统计数据
     */
    List<DyFanProfileStats> getStats(Long accountId, String statType, Long userId);

    /**
     * 手动触发同步
     */
    void manualSync(Long accountId, Long userId);
}
