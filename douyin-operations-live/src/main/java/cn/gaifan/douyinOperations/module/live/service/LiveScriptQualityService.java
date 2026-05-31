package cn.gaifan.douyinOperations.module.live.service;

import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiResultVO;
import cn.gaifan.douyinOperations.module.product.entity.DyProduct;

import java.util.List;
import java.util.Map;

/**
 * 直播话术质量服务（P2-1 从 LiveScriptGenerationServiceImpl 拆分）
 * 负责违规检测、表演指导提取、成篇优化
 */
public interface LiveScriptQualityService {

    /**
     * 深度合规增强检测（品类禁售 + 价格一致性 + 售后承诺）
     * <p>
     * 在基础违禁词检测之上，进行三层深度校验：
     * <ol>
     *   <li>品类级禁售/限售校验（抖音禁止类目关键词）</li>
     *   <li>价格一致性检测（话术报价与商品系统价格交叉校验）</li>
     *   <li>售后承诺高风险检测（无法兑现的退换货/赔偿承诺）</li>
     * </ol>
     *
     * @param content  话术内容
     * @param product  关联商品（可为 null，null 时跳过价格一致性检测）
     * @return 增强检测结果（违规条目追加到 violations 列表）
     */
    default LiveAiResultVO.ViolationCheckResult checkViolationEnhanced(String content, DyProduct product) {
        return checkViolationEnhanced(content, product, product != null ? product.getUserId() : null);
    }

    LiveAiResultVO.ViolationCheckResult checkViolationEnhanced(String content, DyProduct product, Long userId);

    /**
     * 违规检测
     *
     * @param content 话术内容
     * @param userId  用户 ID
     * @param scope   检测范围（如 "live"）
     * @return 检测结果
     */
    LiveAiResultVO.ViolationCheckResult checkViolation(String content, Long userId, String scope);

    /**
     * 从话术内容中提取表演指导（【...】标注）
     *
     * @param content 话术内容
     * @return 表演指导列表
     */
    List<String> extractPerformanceGuides(String content);

    /**
     * 分析并优化整场话术的衔接与语气
     *
     * @param sessionId 直播场次 ID
     * @param userId    用户 ID
     * @param modelId   模型 ID（可为 null）
     */
    void analyzeAndRefineFullScript(Long sessionId, Long userId, Long modelId);

    /**
     * 按字数粗估口播时长与槽位时长对比（Q-4）
     *
     * @param content             话术全文
     * @param slotDurationSeconds 槽位可用秒数，null 时不校验
     */
    Map<String, Object> checkDurationFit(String content, Integer slotDurationSeconds);

    /**
     * 生成落库前：按正文与槽位 {@link LiveScript#getDurationLimitSec()} 校验口播时长；
     * 若不通过则将提示合并到 {@link LiveScript#getAiSuggestion()}（前缀 {@code [时长] }），不改变 generation_status。
     *
     * @param script 已设置 {@code scriptContent} 与（可选）{@code durationLimitSec} 的槽位实体
     * @return 与 {@link #checkDurationFit} 相同结构的 Map，供 API 透出
     */
    Map<String, Object> mergeDurationFitHint(LiveScript script);

    /**
     * Q-2：与同用户历史 {@code live_script} 正文做字符 bigram Jaccard 粗查重（非向量/未接 ES），不写库。
     */
    Map<String, Object> checkCorpusDuplicate(Long userId, Long excludeScriptId, String content);

    /**
     * Q-2：疑似高相似复述时合并 {@code [查重] } 到 {@link LiveScript#getAiSuggestion()}。
     */
    Map<String, Object> mergeCorpusDuplicateHint(LiveScript script);

    /**
     * 生成落库前：{@link #mergeDurationFitHint} + {@link #mergeCorpusDuplicateHint}。
     *
     * @return Map 含 {@code durationFit}、{@code duplicateCheck}
     */
    Map<String, Object> applyGenerationQualityHints(LiveScript script);
}
