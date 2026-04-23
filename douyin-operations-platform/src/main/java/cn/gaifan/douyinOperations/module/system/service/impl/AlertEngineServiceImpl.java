package cn.gaifan.douyinOperations.module.system.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.system.service.AlertEngineService;
import cn.gaifan.douyinOperations.module.system.vo.AlertRuleVO;
import cn.gaifan.douyinOperations.module.system.vo.AlertRecordVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警引擎服务实现
 */
@Service
public class AlertEngineServiceImpl implements AlertEngineService {

    private static final Logger log = LoggerFactory.getLogger(AlertEngineServiceImpl.class);

    // 简单实现：内存存储（生产环境应使用数据库）
    private final Map<Long, AlertRuleVO> rules = new ConcurrentHashMap<>();
    private final Map<Long, AlertRecordVO> records = new ConcurrentHashMap<>();
    private Long ruleIdCounter = 1L;
    private Long recordIdCounter = 1L;

    @Override
    public long createAlertRule(AlertRuleVO vo) {
        if (vo == null || vo.getName() == null || vo.getMetricName() == null) {
            throw new BusinessException(ErrorCode.ALERT_RULE_INVALID, "告警规则参数无效");
        }

        Long ruleId = ruleIdCounter++;
        vo.setId(ruleId);
        if (vo.getEnabled() == null) {
            vo.setEnabled(true);
        }
        rules.put(ruleId, vo);

        log.info("Alert rule created: ruleId={}, name={}", ruleId, vo.getName());
        return ruleId;
    }

    @Override
    public void updateAlertRule(Long ruleId, AlertRuleVO vo) {
        if (!rules.containsKey(ruleId)) {
            throw new BusinessException(ErrorCode.ALERT_RULE_NOT_FOUND, "告警规则不存在");
        }

        vo.setId(ruleId);
        rules.put(ruleId, vo);
        log.info("Alert rule updated: ruleId={}", ruleId);
    }

    @Override
    public void deleteAlertRule(Long ruleId) {
        if (!rules.containsKey(ruleId)) {
            throw new BusinessException(ErrorCode.ALERT_RULE_NOT_FOUND, "告警规则不存在");
        }

        rules.remove(ruleId);
        log.info("Alert rule deleted: ruleId={}", ruleId);
    }

    @Override
    public AlertRuleVO getAlertRule(Long ruleId) {
        return rules.getOrDefault(ruleId, null);
    }

    @Override
    public PageResultVO<AlertRuleVO> listAlertRules(int page, int rows) {
        List<AlertRuleVO> allRules = new ArrayList<>(rules.values());
        int total = allRules.size();

        int start = page * rows;
        int end = Math.min(start + rows, total);

        List<AlertRuleVO> pageData = allRules.subList(Math.min(start, total), end);
        return PageResultVO.of((long) total, pageData, page, rows);
    }

    @Override
    @Scheduled(fixedDelay = 60000) // 每分钟执行一次
    public void executeAlertChecks() {
        log.debug("Executing alert checks...");

        for (AlertRuleVO rule : rules.values()) {
            if (!rule.getEnabled()) {
                continue;
            }

            // 模拟告警检查
            executeRuleCheck(rule);
        }

        log.debug("Alert checks completed");
    }

    @Override
    public AlertRecordVO getAlertRecord(Long recordId) {
        return records.getOrDefault(recordId, null);
    }

    @Override
    public PageResultVO<AlertRecordVO> listAlertRecords(int page, int rows) {
        List<AlertRecordVO> allRecords = new ArrayList<>(records.values());
        int total = allRecords.size();

        int start = page * rows;
        int end = Math.min(start + rows, total);

        List<AlertRecordVO> pageData = allRecords.subList(Math.min(start, total), end);
        return PageResultVO.of((long) total, pageData, page, rows);
    }

    @Override
    public void enableAlertRule(Long ruleId) {
        AlertRuleVO rule = getAlertRule(ruleId);
        if (rule == null) {
            throw new BusinessException(ErrorCode.ALERT_RULE_NOT_FOUND, "告警规则不存在");
        }

        rule.setEnabled(true);
        log.info("Alert rule enabled: ruleId={}", ruleId);
    }

    @Override
    public void disableAlertRule(Long ruleId) {
        AlertRuleVO rule = getAlertRule(ruleId);
        if (rule == null) {
            throw new BusinessException(ErrorCode.ALERT_RULE_NOT_FOUND, "告警规则不存在");
        }

        rule.setEnabled(false);
        log.info("Alert rule disabled: ruleId={}", ruleId);
    }

    private void executeRuleCheck(AlertRuleVO rule) {
        try {
            // 模拟指标检查
            double currentValue = Math.random() * 100;
            boolean triggered = evaluateCondition(currentValue, rule.getThreshold(), rule.getOperator());

            if (triggered) {
                createAlertRecord(rule, currentValue);
            }
        } catch (Exception e) {
            log.error("Failed to execute alert rule: ruleId={}", rule.getId(), e);
        }
    }

    private boolean evaluateCondition(double value, double threshold, String operator) {
        return switch (operator) {
            case ">" -> value > threshold;
            case "<" -> value < threshold;
            case ">=" -> value >= threshold;
            case "<=" -> value <= threshold;
            case "=" -> Math.abs(value - threshold) < 0.001;
            default -> false;
        };
    }

    private void createAlertRecord(AlertRuleVO rule, double value) {
        Long recordId = recordIdCounter++;
        AlertRecordVO record = AlertRecordVO.builder()
                .id(recordId)
                .ruleId(rule.getId())
                .ruleName(rule.getName())
                .metricName(rule.getMetricName())
                .message(String.format("Metric %s exceeded threshold: %.2f > %.2f",
                    rule.getMetricName(), value, rule.getThreshold()))
                .status("triggered")
                .severity(rule.getSeverity())
                .value(value)
                .threshold(rule.getThreshold())
                .triggeredAt(LocalDateTime.now())
                .build();

        records.put(recordId, record);
        log.warn("Alert triggered: recordId={}, ruleName={}", recordId, rule.getName());
    }
}
