package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.live.entity.LiveScriptTemplate;

import java.util.List;

/**
 * 直播话术模板服务
 * 高效话术（effectiveness_score > 80）自动入库
 */
public interface LiveScriptTemplateService {

    /**
     * 从 live_script 导入高效话术到模板库
     * 条件：effectiveness_score >= 80 且未入库
     */
    int importFromHighEffectivenessScripts();

    /**
     * 手动将指定话术入库为模板
     */
    LiveScriptTemplate saveFromScript(Long scriptId, String templateName, String category);

    List<LiveScriptTemplate> listByType(String scriptType, int limit);
}
