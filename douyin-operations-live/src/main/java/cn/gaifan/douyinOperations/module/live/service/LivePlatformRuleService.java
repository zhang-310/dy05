package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.live.entity.LivePlatform;
import cn.gaifan.douyinOperations.module.live.entity.LiveViolationRule;

import java.util.List;
import java.util.Map;

/**
 * 多平台规则引擎服务
 */
public interface LivePlatformRuleService {

    /** 获取所有启用的平台 */
    List<LivePlatform> listActivePlatforms();

    /** 根据平台编码获取平台配置 */
    LivePlatform getPlatformByCode(String platformCode);

    /** 获取平台级违禁词列表 */
    List<LiveViolationRule> getViolationRules(String platformCode);

    /** 检查文本是否违反平台规则，返回命中的违禁词列表 */
    List<Map<String, Object>> checkPlatformViolation(String text, String platformCode);

    /** 获取平台级 prompt 模板片段 */
    String getPlatformPromptTemplate(String platformCode);
}
