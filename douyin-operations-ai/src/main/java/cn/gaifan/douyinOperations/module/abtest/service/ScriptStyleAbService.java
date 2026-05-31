package cn.gaifan.douyinOperations.module.abtest.service;

import cn.gaifan.douyinOperations.module.abtest.vo.ScriptStyleAssignVO;

/**
 * 话术风格 A/B 测试：随机分配风格、记录转化，用于对比转化率
 */
public interface ScriptStyleAbService {

    /**
     * 为当前请求分配风格：若有针对该目标的运行中 script_style 实验，则随机返回 A/B 变体及 styleCode；否则返回 null（由业务方使用默认风格）
     *
     * @param ownerId           所属用户
     * @param targetEntityType  product / live_session
     * @param targetEntityId    产品 ID 或直播场次 ID
     * @param userFingerprint   用户指纹（用于去重与一致性）
     * @return 分配结果，无实验时返回 null
     */
    ScriptStyleAssignVO assignStyle(Long ownerId, String targetEntityType, Long targetEntityId, String userFingerprint);

    /**
     * 记录一次“曝光”：分配时调用，用于统计 view
     */
    void recordView(Long experimentId, Long variantId, String userFingerprint);

    /**
     * 记录一次转化：有转化时调用（如订单、成交），用于对比 A/B 转化率
     */
    void recordConversion(Long experimentId, Long variantId, String userFingerprint);
}
