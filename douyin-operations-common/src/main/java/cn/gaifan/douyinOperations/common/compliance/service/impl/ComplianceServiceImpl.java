package cn.gaifan.douyinOperations.common.compliance.service.impl;

import cn.gaifan.douyinOperations.common.compliance.entity.ComplianceCheckLog;
import cn.gaifan.douyinOperations.common.compliance.entity.ComplianceKeyword;
import cn.gaifan.douyinOperations.common.compliance.entity.ComplianceRule;
import cn.gaifan.douyinOperations.common.compliance.repository.ComplianceCheckLogRepository;
import cn.gaifan.douyinOperations.common.compliance.repository.ComplianceKeywordRepository;
import cn.gaifan.douyinOperations.common.compliance.repository.ComplianceRuleRepository;
import cn.gaifan.douyinOperations.common.compliance.service.ComplianceService;
import cn.gaifan.douyinOperations.common.compliance.vo.ComplianceCheckResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 违规检测服务实现
 * 三层检测机制：
 * 1. 关键词匹配（快速筛查）
 * 2. 正则表达式匹配（模式识别）
 * 3. 语义检测（AI 理解）
 */
@Slf4j
@Service
public class ComplianceServiceImpl implements ComplianceService {

    @Resource
    private ComplianceRuleRepository ruleRepository;

    @Resource
    private ComplianceKeywordRepository keywordRepository;

    @Resource
    private ComplianceCheckLogRepository checkLogRepository;

    @Resource
    private ObjectMapper objectMapper;

    // 缓存：所有启用的规则
    private List<ComplianceRule> allRules;

    // 缓存：所有启用的敏感词
    private List<ComplianceKeyword> allKeywords;

    // 缓存：敏感词集合（用于快速查找）
    private Set<String> keywordSet;

    @PostConstruct
    public void init() {
        refreshCache();
    }

    /**
     * 刷新缓存
     */
    public void refreshCache() {
        allRules = ruleRepository.findAllEnabled();
        allKeywords = keywordRepository.findAllEnabled();
        keywordSet = allKeywords.stream()
                .map(ComplianceKeyword::getKeyword)
                .collect(Collectors.toSet());
        log.info("Compliance cache refreshed: {} rules, {} keywords", allRules.size(), allKeywords.size());
    }

    @Override
    public ComplianceCheckResult check(Long userId, String contentType, Long contentId, String content) {
        long startTime = System.currentTimeMillis();

        // 1. 关键词匹配
        ComplianceCheckResult keywordResult = checkKeywords(content);

        // 2. 正则表达式匹配
        ComplianceCheckResult patternResult = checkPatterns(content);

        // 3. 语义检测（暂时跳过，后续集成 AI）
        // ComplianceCheckResult semanticResult = checkSemantic(content);

        // 4. 综合评分
        ComplianceCheckResult finalResult = mergeResults(keywordResult, patternResult);
        finalResult.setCheckDurationMs((int) (System.currentTimeMillis() - startTime));

        // 5. 保存检测记录
        saveCheckLog(userId, contentType, contentId, content, finalResult);

        return finalResult;
    }

    @Override
    public ComplianceCheckResult check(Long userId, String contentType, String content) {
        return check(userId, contentType, null, content);
    }

    @Override
    public ComplianceCheckResult checkKeywords(String content) {
        if (!StringUtils.hasText(content)) {
            return createPassResult();
        }

        List<ComplianceCheckResult.MatchedRule> matchedRules = new ArrayList<>();
        int score = 0;

        // 遍历所有敏感词
        for (ComplianceKeyword keyword : allKeywords) {
            if (content.contains(keyword.getKeyword())) {
                // 找到匹配的规则
                ComplianceRule rule = findRuleBySeverity(keyword.getSeverity(), keyword.getCategory());
                if (rule != null) {
                    matchedRules.add(new ComplianceCheckResult.MatchedRule(
                            rule.getRuleCode(),
                            rule.getRuleName(),
                            rule.getSeverity(),
                            "包含敏感词: " + keyword.getKeyword(),
                            rule.getPunishment()
                    ));

                    // 根据严重程度扣分
                    score += getSeverityScore(keyword.getSeverity());
                }
            }
        }

        return buildResult(score, matchedRules);
    }

    @Override
    public ComplianceCheckResult checkPatterns(String content) {
        if (!StringUtils.hasText(content)) {
            return createPassResult();
        }

        List<ComplianceCheckResult.MatchedRule> matchedRules = new ArrayList<>();
        int score = 0;

        // 遍历所有规则的正则表达式
        for (ComplianceRule rule : allRules) {
            if (!StringUtils.hasText(rule.getPatterns())) {
                continue;
            }

            try {
                List<String> patterns = objectMapper.readValue(rule.getPatterns(), new TypeReference<List<String>>() {});
                for (String patternStr : patterns) {
                    Pattern pattern = Pattern.compile(patternStr);
                    Matcher matcher = pattern.matcher(content);
                    if (matcher.find()) {
                        matchedRules.add(new ComplianceCheckResult.MatchedRule(
                                rule.getRuleCode(),
                                rule.getRuleName(),
                                rule.getSeverity(),
                                "匹配违规模式: " + matcher.group(),
                                rule.getPunishment()
                        ));

                        score += getSeverityScore(rule.getSeverity());
                        break; // 每个规则只记录一次
                    }
                }
            } catch (Exception e) {
                log.error("Failed to parse patterns for rule: {}", rule.getRuleCode(), e);
            }
        }

        return buildResult(score, matchedRules);
    }

    @Override
    public ComplianceCheckResult checkSemantic(String content) {
        // TODO: 集成 AI 语义检测
        // 1. 向量化内容
        // 2. 向量检索相似规则
        // 3. LLM 判断是否违规
        return createPassResult();
    }

    /**
     * 合并多个检测结果
     */
    private ComplianceCheckResult mergeResults(ComplianceCheckResult... results) {
        List<ComplianceCheckResult.MatchedRule> allMatched = new ArrayList<>();
        int totalScore = 0;

        for (ComplianceCheckResult result : results) {
            if (result.getMatchedRules() != null) {
                allMatched.addAll(result.getMatchedRules());
            }
            if (result.getRiskScore() != null) {
                totalScore += result.getRiskScore().intValue();
            }
        }

        // 去重（相同规则编码只保留一个）
        Map<String, ComplianceCheckResult.MatchedRule> uniqueRules = new LinkedHashMap<>();
        for (ComplianceCheckResult.MatchedRule rule : allMatched) {
            uniqueRules.putIfAbsent(rule.getRuleCode(), rule);
        }

        return buildResult(totalScore, new ArrayList<>(uniqueRules.values()));
    }

    /**
     * 构建检测结果
     */
    private ComplianceCheckResult buildResult(int score, List<ComplianceCheckResult.MatchedRule> matchedRules) {
        ComplianceCheckResult result = new ComplianceCheckResult();
        result.setRiskScore(BigDecimal.valueOf(Math.min(score, 100)));
        result.setMatchedRules(matchedRules);

        // 判定结果
        if (score >= 50) {
            result.setResult("reject");
            result.setSuggestions("内容包含严重违规，建议重新编写");
        } else if (score >= 20) {
            result.setResult("warning");
            result.setSuggestions("内容存在违规风险，建议修改");
        } else {
            result.setResult("pass");
            result.setSuggestions(null);
        }

        return result;
    }

    /**
     * 创建通过结果
     */
    private ComplianceCheckResult createPassResult() {
        ComplianceCheckResult result = new ComplianceCheckResult();
        result.setResult("pass");
        result.setRiskScore(BigDecimal.ZERO);
        result.setMatchedRules(Collections.emptyList());
        return result;
    }

    /**
     * 根据严重程度获取扣分
     */
    private int getSeverityScore(String severity) {
        return switch (severity) {
            case "critical" -> 50;
            case "high" -> 30;
            case "medium" -> 15;
            case "low" -> 5;
            default -> 0;
        };
    }

    /**
     * 根据严重程度和分类查找规则
     */
    private ComplianceRule findRuleBySeverity(String severity, String category) {
        return allRules.stream()
                .filter(r -> r.getSeverity().equals(severity) && r.getCategory().contains(category))
                .findFirst()
                .orElse(null);
    }

    /**
     * 保存检测记录
     */
    private void saveCheckLog(Long userId, String contentType, Long contentId, String content, ComplianceCheckResult result) {
        try {
            ComplianceCheckLog log = new ComplianceCheckLog();
            log.setUserId(userId);
            log.setContentType(contentType);
            log.setContentId(contentId);
            log.setContentText(content.length() > 1000 ? content.substring(0, 1000) : content);
            log.setCheckResult(result.getResult());
            log.setRiskScore(result.getRiskScore());
            log.setSuggestions(result.getSuggestions());
            log.setCheckDurationMs(result.getCheckDurationMs());

            // 序列化匹配的规则
            if (result.getMatchedRules() != null && !result.getMatchedRules().isEmpty()) {
                log.setMatchedRules(objectMapper.writeValueAsString(result.getMatchedRules()));
            }

            checkLogRepository.save(log);
        } catch (Exception e) {
            log.error("Failed to save compliance check log", e);
        }
    }
}
