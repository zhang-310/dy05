package cn.gaifan.douyinOperations.module.live.service;

import java.util.List;

/**
 * 直播话术模板服务接口
 */
public interface LiveTemplateService {

    /**
     * 将场次的话术保存为可复用模板
     * <p>
     * 1. 获取场次下所有话术
     * 2. 按 scriptTypes 过滤（为空则全部包含）
     * 3. 对产品话术：将产品名替换为 "{产品名}" 占位符
     * 4. 创建 LiveScriptTemplate 记录
     *
     * @param sessionId    场次 ID
     * @param templateName 模板名称
     * @param scriptTypes  要包含的话术类型列表（为空则全部）
     * @param userId       当前用户 ID
     * @return 首条创建的模板 ID（多条模板共享同一 sourceSessionId）
     */
    Long saveSessionAsTemplate(Long sessionId, String templateName, List<String> scriptTypes, Long userId);
}
