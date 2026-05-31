package cn.gaifan.douyinOperations.module.system.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.system.entity.ExternalApiConfig;
import cn.gaifan.douyinOperations.module.system.vo.ExternalApiConfigSaveVO;
import cn.gaifan.douyinOperations.module.system.vo.ExternalApiConfigSearchVO;

import java.util.List;

/**
 * 外部 API 配置管理服务
 */
public interface ExternalApiConfigService {

    /**
     * 分页搜索配置（Specification 动态查询）
     */
    PageResultVO<ExternalApiConfig> search(ExternalApiConfigSearchVO searchVO);

    /**
     * 根据供应商编码获取配置
     */
    ExternalApiConfig getByProviderCode(String code);

    /**
     * 根据供应商编码获取原始配置（内部任务使用，不做密钥脱敏）
     */
    ExternalApiConfig getRawByProviderCode(String code);

    /**
     * 新增或更新配置
     */
    ExternalApiConfig save(ExternalApiConfigSaveVO saveVO);

    /**
     * 逻辑删除
     */
    void delete(Long id);

    /**
     * 获取指定分类下已启用的配置（按 priority 排序）
     */
    List<ExternalApiConfig> getEnabledByCategory(String category);

    /**
     * 更新健康检查状态
     */
    void updateHealthStatus(String providerCode, String status, Integer latencyMs, Float successRate);

    /**
     * 记录一次 API 调用日志
     */
    void logApiCall(String providerCode, String endpoint, String method,
                    int responseStatus, int latencyMs, String error,
                    String callerModule, Long userId);

    /**
     * 获取解密后的 API Key（仅限内部服务调用，如健康检查）
     */
    String getDecryptedApiKey(String providerCode);

    /**
     * 获取所有已启用的配置（用于定时健康检查）
     */
    List<ExternalApiConfig> getAllEnabled();
}
