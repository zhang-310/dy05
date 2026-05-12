package cn.gaifan.douyinOperations.module.system.controller;

import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.system.service.AlertEngineService;
import cn.gaifan.douyinOperations.module.system.service.DashboardDataService;
import cn.gaifan.douyinOperations.module.system.vo.AlertRecordIdVO;
import cn.gaifan.douyinOperations.module.system.vo.AlertRuleIdVO;
import cn.gaifan.douyinOperations.module.system.vo.AlertRuleUpdateVO;
import cn.gaifan.douyinOperations.module.system.vo.AlertRuleVO;
import cn.gaifan.douyinOperations.module.system.vo.PageQueryVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

/**
 * 告警和仪表板控制器
 */
@RestController
@RequestMapping("/api/v1/system")
public class AlertController {

    @Resource
    private AlertEngineService alertEngineService;

    @Resource
    private DashboardDataService dashboardDataService;

    // ==================== 告警规则管理 ====================

    /**
     * 创建告警规则
     */
    @PostMapping("/alert/rule/create")
    public RESTResult<?> createAlertRule(@Valid @RequestBody AlertRuleVO vo) {
        long ruleId = alertEngineService.createAlertRule(vo);
        return RESTResult.success(ruleId);
    }

    /**
     * 更新告警规则
     * P1-6: 使用强类型 VO，避免 Map 手动转换的 NPE 风险
     */
    @PostMapping("/alert/rule/update")
    public RESTResult<?> updateAlertRule(@Valid @RequestBody AlertRuleUpdateVO vo) {
        AlertRuleVO ruleVO = AlertRuleVO.builder()
                .name(vo.getName())
                .metricName(vo.getMetricName())
                .type(vo.getType())
                .threshold(vo.getThreshold())
                .operator(vo.getOperator())
                .duration(vo.getDuration())
                .severity(vo.getSeverity())
                .description(vo.getDescription())
                .enabled(vo.getEnabled())
                .build();
        alertEngineService.updateAlertRule(vo.getRuleId(), ruleVO);
        return RESTResult.success();
    }

    /**
     * 删除告警规则
     * P1-6: 使用强类型 VO，避免 Map 手动转换的 NPE 风险
     */
    @PostMapping("/alert/rule/delete")
    public RESTResult<?> deleteAlertRule(@Valid @RequestBody AlertRuleIdVO vo) {
        alertEngineService.deleteAlertRule(vo.getRuleId());
        return RESTResult.success();
    }

    /**
     * 获取告警规则详情
     * P1-6: 使用强类型 VO，避免 Map 手动转换的 NPE 风险
     */
    @PostMapping("/alert/rule/get")
    public RESTResult<?> getAlertRule(@Valid @RequestBody AlertRuleIdVO vo) {
        return RESTResult.success(alertEngineService.getAlertRule(vo.getRuleId()));
    }

    /**
     * 分页查询告警规则
     * P1-6: 使用强类型 VO，避免 Map 手动转换的 NPE 风险
     */
    @PostMapping("/alert/rule/list")
    public RESTResult<?> listAlertRules(@Valid @RequestBody PageQueryVO vo) {
        return RESTResult.success(alertEngineService.listAlertRules(vo.getPage(), vo.getRows()));
    }

    /**
     * 启用告警规则
     * P1-6: 使用强类型 VO，避免 Map 手动转换的 NPE 风险
     */
    @PostMapping("/alert/rule/enable")
    public RESTResult<?> enableAlertRule(@Valid @RequestBody AlertRuleIdVO vo) {
        alertEngineService.enableAlertRule(vo.getRuleId());
        return RESTResult.success();
    }

    /**
     * 禁用告警规则
     * P1-6: 使用强类型 VO，避免 Map 手动转换的 NPE 风险
     */
    @PostMapping("/alert/rule/disable")
    public RESTResult<?> disableAlertRule(@Valid @RequestBody AlertRuleIdVO vo) {
        alertEngineService.disableAlertRule(vo.getRuleId());
        return RESTResult.success();
    }

    // ==================== 告警记录 ====================

    /**
     * 获取告警记录详情
     * P1-6: 使用强类型 VO，避免 Map 手动转换的 NPE 风险
     */
    @PostMapping("/alert/record/get")
    public RESTResult<?> getAlertRecord(@Valid @RequestBody AlertRecordIdVO vo) {
        return RESTResult.success(alertEngineService.getAlertRecord(vo.getRecordId()));
    }

    /**
     * 分页查询告警记录
     * P1-6: 使用强类型 VO，避免 Map 手动转换的 NPE 风险
     */
    @PostMapping("/alert/record/list")
    public RESTResult<?> listAlertRecords(@Valid @RequestBody PageQueryVO vo) {
        return RESTResult.success(alertEngineService.listAlertRecords(vo.getPage(), vo.getRows()));
    }

    // ==================== 仪表板数据 ====================

    /**
     * 获取系统整体统计
     */
    @GetMapping("/dashboard/overview")
    public RESTResult<?> getSystemOverview() {
        return RESTResult.success(dashboardDataService.getSystemOverview());
    }

    /**
     * 获取实时告警数据
     */
    @GetMapping("/dashboard/alerts")
    public RESTResult<?> getRealtimeAlerts() {
        return RESTResult.success(dashboardDataService.getRealtimeAlerts());
    }

    /**
     * 获取性能指标趋势
     */
    @GetMapping("/dashboard/performance")
    public RESTResult<?> getPerformanceTrends() {
        return RESTResult.success(dashboardDataService.getPerformanceTrends());
    }

    /**
     * 获取日志聚合统计
     */
    @GetMapping("/dashboard/logs")
    public RESTResult<?> getLogStatistics() {
        return RESTResult.success(dashboardDataService.getLogStatistics());
    }

    /**
     * 获取链路追踪摘要
     */
    @GetMapping("/dashboard/traces")
    public RESTResult<?> getTracesSummary() {
        return RESTResult.success(dashboardDataService.getTracesSummary());
    }

    /**
     * 获取健康检查状态
     */
    @GetMapping("/dashboard/health")
    public RESTResult<?> getHealthStatus() {
        return RESTResult.success(dashboardDataService.getHealthStatus());
    }
}
