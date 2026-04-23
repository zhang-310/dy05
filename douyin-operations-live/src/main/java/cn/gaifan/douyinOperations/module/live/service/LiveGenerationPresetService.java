package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.live.entity.LiveGenerationPreset;
import cn.gaifan.douyinOperations.module.live.vo.LiveGenerationPresetSaveVO;

import java.util.List;

/**
 * 生成配置预设服务接口
 */
public interface LiveGenerationPresetService {

    /**
     * 获取用户的所有预设（按创建时间倒序）
     */
    List<LiveGenerationPreset> list(Long ownerId);

    /**
     * 创建或更新预设
     */
    LiveGenerationPreset save(LiveGenerationPresetSaveVO vo, Long ownerId);

    /**
     * 删除预设（逻辑删除）
     */
    void delete(Long id, Long ownerId);

    /**
     * 获取用户的默认预设（无默认返回 null）
     */
    LiveGenerationPreset getDefault(Long ownerId);

    /**
     * 将指定预设设为用户的默认预设
     */
    void setDefault(Long id, Long ownerId);
}
