package cn.gaifan.douyinOperations.module.shortvideo.service;

import java.util.List;
import java.util.Map;

/**
 * 短视频脚本效果追踪服务。
 * 关联 sv_script → sv_project → sv_video_data，按维度聚合效果数据。
 */
public interface SvScriptEffectivenessService {

    /**
     * 按脚本类型聚合效果统计
     * @return [{scriptType, totalViews, totalLikes, avgCompletionRate, scriptCount}]
     */
    List<Map<String, Object>> aggregateByScriptType(Long ownerId);

    /**
     * 按风格聚合效果统计
     * @return [{style, totalViews, totalLikes, scriptCount}]
     */
    List<Map<String, Object>> aggregateByStyle(Long ownerId);

    /**
     * 单脚本效果追踪（查询关联视频数据）
     */
    Map<String, Object> getScriptEffectiveness(Long scriptId, Long ownerId);
}
