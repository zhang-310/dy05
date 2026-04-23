package cn.gaifan.douyinOperations.module.system.controller;

import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.system.service.AlertEngineService;
import cn.gaifan.douyinOperations.module.system.service.DashboardDataService;
import cn.gaifan.douyinOperations.module.system.vo.AlertRuleVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.Map;

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
     */
    @PostMapping("/alert/rule/update")
    public RESTResult<?> updateAlertRule(@RequestBody Map<String, Object> request) {
        Long ruleId = Long.parseLong(request.get("ruleId").toString());
        AlertRuleVO vo = convertToAlertRuleVO(request);
        alertEngineService.updateAlertRule(ruleId, vo);
        return RESTResult.success();
    }

    /**
     * 删除告警规则
     */
    @PostMapping("/alert/rule/delete")
    public RESTResult<?> deleteAlertRule(@RequestBody Map<String, Long> request) {
        Long ruleId = request.get("ruleId");
        alertEngineService.deleteAlertRule(ruleId);
        return RESTResult.success();
    }

    /**
     * 获取告警规则详情
     */
    @PostMapping("/alert/rule/get")
    public RESTResult<?> getAlertRule(@RequestBody Map<String, Long> request) {
        Long ruleId = request.get("ruleId");
        return RESTResult.success(alertEngineService.getAlertRule(ruleId));
    }

    /**
     * 分页查询告警规则
     */
    @PostMapping("/alert/rule/list")
    public RESTResult<?> listAlertRules(@RequestBody Map<String, Integer> request) {
        Integer page = request.getOrDefault("page", 0);
        Integer rows = request.getOrDefault("rows", 30);
        return RESTResult.success(alertEngineService.listAlertRules(page, rows));
    }

    /**
     * 启用告警规则
     */
    @PostMapping("/alert/rule/enable")
    public RESTResult<?> enableAlertRule(@RequestBody Map<String, Long> request) {
        Long ruleId = request.get("ruleId");
        alertEngineService.enableAlertRule(ruleId);
        return RESTResult.success();
    }

    /**
     * 禁用告警规则
     */
    @PostMapping("/alert/rule/disable")
    public RESTResult<?> disableAlertRule(@RequestBody Map<String, Long> request) {
        Long ruleId = request.get("ruleId");
        alertEngineService.disableAlertRule(ruleId);
        return RESTResult.success();
    }

    // ==================== 告警记录 ====================

    /**
     * 获取告警记录详情
     */
    @PostMapping("/alert/record/get")
    public RESTResult<?> getAlertRecord(@RequestBody Map<String, Long> request) {
        Long recordId = request.get("recordId");
        return RESTResult.success(alertEngineService.getAlertRecord(recordId));
    }

    /**
     * 分页查询告警记录
     */
    @PostMapping("/alert/record/list")
    public RESTResult<?> listAlertRecords(@RequestBody Map<String, Integer> request) {
        Integer page = request.getOrDefault("page", 0);
        Integer rows = request.getOrDefault("rows", 30);
        return RESTResult.success(alertEngineService.listAlertRecords(page, rows));
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

    // ==================== 辅助方法 ====================

    private AlertRuleVO convertToAlertRuleVO(Map<String, Object> request) {
        return AlertRuleVO.builder()
                .name((String) request.get("name"))
                .metricName((String) request.get("metricName"))
                .type((String) request.get("type"))
                .threshold(((Number) request.get("threshold")).doubleValue())
                .operator((String) request.get("operator"))
                .duration(((Number) request.get("duration")).intValue())
                .severity((String) request.get("severity"))
                .description((String) request.get("description"))
                .enabled((Boolean) request.get("enabled"))
                .build();
    }
}
