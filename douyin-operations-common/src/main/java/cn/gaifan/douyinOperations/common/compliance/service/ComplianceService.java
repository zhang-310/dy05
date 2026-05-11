package cn.gaifan.douyinOperations.common.compliance.service;

import cn.gaifan.douyinOperations.common.compliance.vo.ComplianceCheckResult;

/**
 * 违规检测服务接口
 */
public interface ComplianceService {

    /**
     * 检测内容是否违规
     *
     * @param userId      用户 ID
     * @param contentType 内容类型（script/video/material）
     * @param contentId   内容 ID（可选）
     * @param content     检测内容
     * @return 检测结果
     */
    ComplianceCheckResult check(Long userId, String contentType, Long contentId, String content);

    /**
     * 检测内容是否违规（简化版，无 contentId）
     *
     * @param userId      用户 ID
     * @param contentType 内容类型
     * @param content     检测内容
     * @return 检测结果
     */
    ComplianceCheckResult check(Long userId, String contentType, String content);

    /**
     * 关键词匹配检测
     *
     * @param content 检测内容
     * @return 匹配的关键词列表
     */
    ComplianceCheckResult checkKeywords(String content);

    /**
     * 正则表达式匹配检测
     *
     * @param content 检测内容
     * @return 匹配的模式列表
     */
    ComplianceCheckResult checkPatterns(String content);

    /**
     * 语义检测（AI 理解）
     *
     * @param content 检测内容
     * @return 语义检测结果
     */
    ComplianceCheckResult checkSemantic(String content);
}
