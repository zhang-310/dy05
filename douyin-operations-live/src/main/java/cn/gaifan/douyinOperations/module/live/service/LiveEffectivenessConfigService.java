package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.live.entity.LiveEffectivenessConfig;
import cn.gaifan.douyinOperations.module.live.vo.LiveEffectivenessConfigSaveVO;

import java.util.List;

/**
 * 效果评分权重配置 Service 接口
 * Q3-5: Configurable Effectiveness Score Formula
 */
public interface LiveEffectivenessConfigService {

    /**
     * 获取用户的默认配置；如果用户没有自定义配置，返回系统默认值（0.3/0.3/0.4）
     */
    LiveEffectivenessConfig getDefaultConfig(Long userId);

    /**
     * 创建或更新配置
     */
    LiveEffectivenessConfig save(LiveEffectivenessConfigSaveVO vo, Long userId);

    /**
     * 列出用户的所有配置
     */
    List<LiveEffectivenessConfig> list(Long userId);

    /**
     * 将指定配置设为用户的默认配置
     */
    void setDefault(Long configId, Long userId);

    /**
     * 逻辑删除配置
     */
    void delete(Long configId, Long userId);
}
