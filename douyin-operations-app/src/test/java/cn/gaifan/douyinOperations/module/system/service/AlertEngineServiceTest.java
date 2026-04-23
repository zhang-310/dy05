package cn.gaifan.douyinOperations.module.system.service;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.system.service.impl.AlertEngineServiceImpl;
import cn.gaifan.douyinOperations.module.system.vo.AlertRuleVO;
import cn.gaifan.douyinOperations.module.system.vo.AlertRecordVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 告警引擎服务单元测试
 */
@DisplayName("AlertEngineService 单元测试")
class AlertEngineServiceTest {

    private AlertEngineService alertEngineService;

    @BeforeEach
    void setUp() {
        alertEngineService = new AlertEngineServiceImpl();
    }

    @Test
    @DisplayName("应该成功创建告警规则")
    void testCreateAlertRule() {
        AlertRuleVO vo = AlertRuleVO.builder()
                .name("CPU 使用率告警")
                .metricName("cpu_usage")
                .type("threshold")
                .threshold(80.0)
                .operator(">")
                .duration(300)
                .severity("critical")
                .description("CPU 使用率超过 80%")
                .enabled(true)
                .build();

        long ruleId = alertEngineService.createAlertRule(vo);

        assertTrue(ruleId > 0);
        AlertRuleVO created = alertEngineService.getAlertRule(ruleId);
        assertNotNull(created);
        assertEquals("CPU 使用率告警", created.getName());
        assertEquals("cpu_usage", created.getMetricName());
    }

    @Test
    @DisplayName("应该拒绝参数无效的告警规则")
    void testCreateInvalidAlertRule() {
        AlertRuleVO vo = AlertRuleVO.builder()
                .name(null)
                .metricName("cpu_usage")
                .type("threshold")
                .threshold(80.0)
                .operator(">")
                .duration(300)
                .severity("critical")
                .build();

        assertThrows(BusinessException.class, () -> {
            alertEngineService.createAlertRule(vo);
        });
    }

    @Test
    @DisplayName("应该成功更新告警规则")
    void testUpdateAlertRule() {
        AlertRuleVO vo = AlertRuleVO.builder()
                .name("CPU 使用率告警")
                .metricName("cpu_usage")
                .type("threshold")
                .threshold(80.0)
                .operator(">")
                .duration(300)
                .severity("critical")
                .enabled(true)
                .build();

        long ruleId = alertEngineService.createAlertRule(vo);

        AlertRuleVO update = AlertRuleVO.builder()
                .name("更新的 CPU 使用率告警")
                .metricName("cpu_usage")
                .type("threshold")
                .threshold(75.0)
                .operator(">")
                .duration(300)
                .severity("warning")
                .enabled(true)
                .build();

        alertEngineService.updateAlertRule(ruleId, update);
        AlertRuleVO updated = alertEngineService.getAlertRule(ruleId);

        assertEquals("更新的 CPU 使用率告警", updated.getName());
        assertEquals(75.0, updated.getThreshold());
    }

    @Test
    @DisplayName("应该成功删除告警规则")
    void testDeleteAlertRule() {
        AlertRuleVO vo = AlertRuleVO.builder()
                .name("CPU 使用率告警")
                .metricName("cpu_usage")
                .type("threshold")
                .threshold(80.0)
                .operator(">")
                .duration(300)
                .severity("critical")
                .enabled(true)
                .build();

        long ruleId = alertEngineService.createAlertRule(vo);
        alertEngineService.deleteAlertRule(ruleId);

        assertNull(alertEngineService.getAlertRule(ruleId));
    }

    @Test
    @DisplayName("应该拒绝删除不存在的规则")
    void testDeleteNonExistentRule() {
        assertThrows(BusinessException.class, () -> {
            alertEngineService.deleteAlertRule(999L);
        });
    }

    @Test
    @DisplayName("应该成功启用规则")
    void testEnableAlertRule() {
        AlertRuleVO vo = AlertRuleVO.builder()
                .name("CPU 使用率告警")
                .metricName("cpu_usage")
                .type("threshold")
                .threshold(80.0)
                .operator(">")
                .duration(300)
                .severity("critical")
                .enabled(false)
                .build();

        long ruleId = alertEngineService.createAlertRule(vo);
        alertEngineService.enableAlertRule(ruleId);

        AlertRuleVO enabled = alertEngineService.getAlertRule(ruleId);
        assertTrue(enabled.getEnabled());
    }

    @Test
    @DisplayName("应该成功禁用规则")
    void testDisableAlertRule() {
        AlertRuleVO vo = AlertRuleVO.builder()
                .name("CPU 使用率告警")
                .metricName("cpu_usage")
                .type("threshold")
                .threshold(80.0)
                .operator(">")
                .duration(300)
                .severity("critical")
                .enabled(true)
                .build();

        long ruleId = alertEngineService.createAlertRule(vo);
        alertEngineService.disableAlertRule(ruleId);

        AlertRuleVO disabled = alertEngineService.getAlertRule(ruleId);
        assertFalse(disabled.getEnabled());
    }

    @Test
    @DisplayName("应该成功分页查询告警规则")
    void testListAlertRules() {
        // 创建多个规则
        for (int i = 0; i < 5; i++) {
            AlertRuleVO vo = AlertRuleVO.builder()
                    .name("告警规则 " + i)
                    .metricName("metric_" + i)
                    .type("threshold")
                    .threshold(50.0 + i * 10)
                    .operator(">")
                    .duration(300)
                    .severity("critical")
                    .enabled(true)
                    .build();
            alertEngineService.createAlertRule(vo);
        }

        var result = alertEngineService.listAlertRules(0, 10);

        assertEquals(5, result.getList().size());
        assertEquals(5, result.getTotal());
    }

    @Test
    @DisplayName("应该成功分页查询告警记录")
    void testListAlertRecords() {
        // 告警记录是通过规则检查自动生成的
        // 这里只测试查询功能
        var result = alertEngineService.listAlertRecords(0, 10);

        assertNotNull(result);
        assertTrue(result.getTotal() >= 0);
    }

    @Test
    @DisplayName("应该成功获取告警记录")
    void testGetAlertRecord() {
        AlertRecordVO record = alertEngineService.getAlertRecord(1L);

        // 初始化时没有记录
        assertNull(record);
    }
}
