package cn.gaifan.douyinOperations.module.system.service;

import cn.gaifan.douyinOperations.module.system.vo.AlertRuleVO;
import cn.gaifan.douyinOperations.module.system.vo.AlertRecordVO;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;

/**
 * 告警引擎服务
 */
public interface AlertEngineService {

    /**
     * 创建告警规则
     */
    long createAlertRule(AlertRuleVO vo);

    /**
     * 更新告警规则
     */
    void updateAlertRule(Long ruleId, AlertRuleVO vo);

    /**
     * 删除告警规则
     */
    void deleteAlertRule(Long ruleId);

    /**
     * 获取告警规则详情
     */
    AlertRuleVO getAlertRule(Long ruleId);

    /**
     * 列表查询告警规则
     */
    PageResultVO<AlertRuleVO> listAlertRules(int page, int rows);

    /**
     * 执行告警检查
     */
    void executeAlertChecks();

    /**
     * 获取告警记录
     */
    AlertRecordVO getAlertRecord(Long recordId);

    /**
     * 分页查询告警记录
     */
    PageResultVO<AlertRecordVO> listAlertRecords(int page, int rows);

    /**
     * 确认告警记录
     */
    void acknowledgeAlertRecord(Long recordId);

    /**
     * 恢复告警记录
     */
    void resolveAlertRecord(Long recordId);

    /**
     * 关闭告警记录
     */
    void closeAlertRecord(Long recordId);

    /**
     * 启用规则
     */
    void enableAlertRule(Long ruleId);

    /**
     * 禁用规则
     */
    void disableAlertRule(Long ruleId);
}
