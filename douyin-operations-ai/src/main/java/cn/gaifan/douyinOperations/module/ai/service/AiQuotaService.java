package cn.gaifan.douyinOperations.module.ai.service;


import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;

import java.util.Map;

/**
 * AI 调用额度服务：按用户/日限制调用次数
 */
public interface AiQuotaService {

    /**
     * 获取用户当日额度信息
     *
     * @param userId 用户 ID
     * @return [usedCount, maxCount]
     */
    QuotaInfo getQuota(Long userId);

    /**
     * 确保有额度（调用前检查，超限抛异常）
     *
     * @param userId 用户 ID
     * @throws BusinessException 超限时抛出 AI_QUOTA_EXCEEDED
     */
    void ensureQuota(Long userId);

    /**
     * 扣减额度（仅在实际调用成功后调用）
     *
     * @param userId 用户 ID
     */
    void consume(Long userId);

    /**
     * 按系数扣减额度（细粒度计费）
     * 引用产品话术=0，模板 Fallback=0.5，AI 生成=1.0
     *
     * @param userId 用户 ID
     * @param coefficient 计费系数（0=不计费，0.5=半额，1.0=全额）
     */
    void consume(Long userId, double coefficient);
    

    /**
     * 仅检查是否超限（不扣减）
     *
     * @param userId 用户 ID
     * @return true 表示可继续调用
     */
    boolean hasQuota(Long userId);

    /**
     * 管理端：获取 AI 配额总览
     */
    Map<String, Object> getAdminQuotaOverview();

    /**
     * 管理端：获取 AI 用量历史
     */
    PageResultVO<Map<String, Object>> getQuotaHistory(int page, int rows, String feature);

    /**
     * 管理端：更新配额配置
     */
    void updateQuotaLimits(Map<String, Object> params, Long operatorId);

    record QuotaInfo(int usedCount, int maxCount, int remaining) {}
}
