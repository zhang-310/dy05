package cn.gaifan.douyinOperations.module.system.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.system.entity.SysAlertRecord;
import cn.gaifan.douyinOperations.module.system.entity.SysAlertRule;
import cn.gaifan.douyinOperations.module.system.repository.SysAlertRecordRepository;
import cn.gaifan.douyinOperations.module.system.repository.SysAlertRuleRepository;
import cn.gaifan.douyinOperations.module.system.service.AlertEngineService;
import cn.gaifan.douyinOperations.module.system.vo.AlertRecordVO;
import cn.gaifan.douyinOperations.module.system.vo.AlertRuleVO;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AlertEngineServiceImpl implements AlertEngineService {

    private static final Logger log = LoggerFactory.getLogger(AlertEngineServiceImpl.class);

    @Resource
    private SysAlertRuleRepository ruleRepository;

    @Resource
    private SysAlertRecordRepository recordRepository;

    @Resource
    private MeterRegistry meterRegistry;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long createAlertRule(AlertRuleVO vo) {
        validateRule(vo);
        SysAlertRule rule = new SysAlertRule();
        applyRule(rule, vo);
        return ruleRepository.save(rule).getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAlertRule(Long ruleId, AlertRuleVO vo) {
        validateRule(vo);
        SysAlertRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALERT_RULE_NOT_FOUND, "告警规则不存在"));
        applyRule(rule, vo);
        ruleRepository.save(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAlertRule(Long ruleId) {
        SysAlertRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALERT_RULE_NOT_FOUND, "告警规则不存在"));
        rule.setDeleted(1);
        ruleRepository.save(rule);
    }

    @Override
    public AlertRuleVO getAlertRule(Long ruleId) {
        return ruleRepository.findById(ruleId)
                .filter(rule -> rule.getDeleted() == null || rule.getDeleted() == 0)
                .map(this::toRuleVO)
                .orElse(null);
    }

    @Override
    public PageResultVO<AlertRuleVO> listAlertRules(int page, int rows) {
        var pageable = PageRequest.of(Math.max(0, page), Math.max(1, rows),
                Sort.by(Sort.Direction.DESC, "id"));
        var data = ruleRepository.findByDeleted(0, pageable);
        return PageResultVO.of(data.getTotalElements(), data.getContent().stream().map(this::toRuleVO).toList(), page, rows);
    }

    @Override
    @Scheduled(fixedDelay = 60000)
    @Transactional(rollbackFor = Exception.class)
    public void executeAlertChecks() {
        for (SysAlertRule rule : ruleRepository.findByEnabledAndDeleted(true, 0)) {
            executeRuleCheck(rule);
        }
    }

    @Override
    public AlertRecordVO getAlertRecord(Long recordId) {
        return recordRepository.findById(recordId)
                .filter(record -> record.getDeleted() == null || record.getDeleted() == 0)
                .map(this::toRecordVO)
                .orElse(null);
    }

    @Override
    public PageResultVO<AlertRecordVO> listAlertRecords(int page, int rows) {
        var pageable = PageRequest.of(Math.max(0, page), Math.max(1, rows),
                Sort.by(Sort.Direction.DESC, "triggeredAt"));
        var data = recordRepository.findByDeleted(0, pageable);
        return PageResultVO.of(data.getTotalElements(), data.getContent().stream().map(this::toRecordVO).toList(), page, rows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acknowledgeAlertRecord(Long recordId) {
        updateRecordStatus(recordId, "acknowledged", false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolveAlertRecord(Long recordId) {
        updateRecordStatus(recordId, "resolved", true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeAlertRecord(Long recordId) {
        updateRecordStatus(recordId, "closed", true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableAlertRule(Long ruleId) {
        updateEnabled(ruleId, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableAlertRule(Long ruleId) {
        updateEnabled(ruleId, false);
    }

    private void updateEnabled(Long ruleId, boolean enabled) {
        SysAlertRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ALERT_RULE_NOT_FOUND, "告警规则不存在"));
        rule.setEnabled(enabled);
        ruleRepository.save(rule);
    }

    private void updateRecordStatus(Long recordId, String status, boolean markResolved) {
        if (recordId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "告警记录 ID 不能为空");
        }
        SysAlertRecord record = recordRepository.findById(recordId)
                .filter(item -> item.getDeleted() == null || item.getDeleted() == 0)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "告警记录不存在"));
        record.setStatus(status);
        if (markResolved && record.getResolvedAt() == null) {
            record.setResolvedAt(LocalDateTime.now());
        }
        recordRepository.save(record);
    }

    private void executeRuleCheck(SysAlertRule rule) {
        Optional<Double> current = readMetric(rule.getMetricName());
        if (current.isEmpty()) {
            log.debug("Skip alert rule without metric value: ruleId={}, metric={}", rule.getId(), rule.getMetricName());
            return;
        }

        double value = current.get();
        if (!evaluateCondition(value, rule.getThreshold(), rule.getOperator())) {
            return;
        }

        SysAlertRecord record = new SysAlertRecord();
        record.setRuleId(rule.getId());
        record.setRuleName(rule.getName());
        record.setMetricName(rule.getMetricName());
        record.setMessage(String.format("Metric %s is %.2f, threshold %s %.2f",
                rule.getMetricName(), value, rule.getOperator(), rule.getThreshold()));
        record.setStatus("triggered");
        record.setSeverity(rule.getSeverity());
        record.setValue(value);
        record.setThreshold(rule.getThreshold());
        record.setTriggeredAt(LocalDateTime.now());
        recordRepository.save(record);
        log.warn("Alert triggered: recordId={}, ruleName={}, metric={}, value={}",
                record.getId(), rule.getName(), rule.getMetricName(), value);
    }

    private Optional<Double> readMetric(String metricName) {
        if (metricName == null || metricName.isBlank()) {
            return Optional.empty();
        }
        return switch (metricName) {
            case "cpu_usage", "system.cpu.usage", "process.cpu.usage" -> Optional.ofNullable(readCpuUsage());
            case "memory_usage", "jvm.memory.usage" -> Optional.ofNullable(readMemoryUsage());
            default -> readMicrometerMetric(metricName);
        };
    }

    private Optional<Double> readMicrometerMetric(String metricName) {
        try {
            for (Meter meter : meterRegistry.find(metricName).meters()) {
                var iterator = meter.measure().iterator();
                double sum = iterator.hasNext() ? iterator.next().getValue() : Double.NaN;
                if (!Double.isNaN(sum) && Double.isFinite(sum)) {
                    return Optional.of(sum);
                }
            }
        } catch (Exception e) {
            log.debug("Unable to read metric {}: {}", metricName, e.getMessage());
        }
        return Optional.empty();
    }

    private Double readCpuUsage() {
        try {
            for (var gauge : meterRegistry.find("process.cpu.usage").gauges()) {
                double value = gauge.value();
                if (!Double.isNaN(value) && value >= 0) {
                    return value * 100;
                }
            }
            OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
            if (os instanceof com.sun.management.OperatingSystemMXBean sunOs) {
                double value = sunOs.getProcessCpuLoad();
                if (!Double.isNaN(value) && value >= 0) {
                    return value * 100;
                }
            }
        } catch (Exception e) {
            log.debug("Unable to read cpu usage: {}", e.getMessage());
        }
        return null;
    }

    private Double readMemoryUsage() {
        try {
            MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
            long used = mem.getHeapMemoryUsage().getUsed();
            long max = mem.getHeapMemoryUsage().getMax();
            return max > 0 ? (double) used / max * 100 : null;
        } catch (Exception e) {
            log.debug("Unable to read memory usage: {}", e.getMessage());
            return null;
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

    private void validateRule(AlertRuleVO vo) {
        if (vo == null || vo.getName() == null || vo.getMetricName() == null
                || vo.getThreshold() == null || vo.getOperator() == null) {
            throw new BusinessException(ErrorCode.ALERT_RULE_INVALID, "告警规则参数无效");
        }
    }

    private void applyRule(SysAlertRule rule, AlertRuleVO vo) {
        rule.setName(vo.getName());
        rule.setMetricName(vo.getMetricName());
        rule.setType(vo.getType());
        rule.setThreshold(vo.getThreshold());
        rule.setOperator(vo.getOperator());
        rule.setDuration(vo.getDuration());
        rule.setSeverity(vo.getSeverity());
        rule.setDescription(vo.getDescription());
        rule.setEnabled(vo.getEnabled() == null || vo.getEnabled());
    }

    private AlertRuleVO toRuleVO(SysAlertRule rule) {
        return AlertRuleVO.builder()
                .id(rule.getId())
                .name(rule.getName())
                .metricName(rule.getMetricName())
                .type(rule.getType())
                .threshold(rule.getThreshold())
                .operator(rule.getOperator())
                .duration(rule.getDuration())
                .severity(rule.getSeverity())
                .description(rule.getDescription())
                .enabled(rule.getEnabled())
                .build();
    }

    private AlertRecordVO toRecordVO(SysAlertRecord record) {
        return AlertRecordVO.builder()
                .id(record.getId())
                .ruleId(record.getRuleId())
                .ruleName(record.getRuleName())
                .metricName(record.getMetricName())
                .message(record.getMessage())
                .status(record.getStatus())
                .severity(record.getSeverity())
                .value(record.getValue())
                .threshold(record.getThreshold())
                .triggeredAt(record.getTriggeredAt())
                .resolvedAt(record.getResolvedAt())
                .build();
    }
}
