package cn.gaifan.douyinOperations.module.script.service;

import java.util.List;
import java.util.Map;

public interface IndustryComplianceService {
    List<Map<String, Object>> checkCompliance(String text, String industryCode);

    List<Map<String, Object>> listRules(String industryCode);

    /** 已配置的垂直行业编码（不含「通用广告法摘要」，通用规则对所有检测均生效） */
    List<String> listSupportedIndustryCodes();
}
