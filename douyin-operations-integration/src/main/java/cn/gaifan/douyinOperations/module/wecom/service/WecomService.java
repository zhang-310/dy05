package cn.gaifan.douyinOperations.module.wecom.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.wecom.vo.*;

import java.util.List;

public interface WecomService {

    PageResultVO<WcRobotConfigVO> searchRobots(WcRobotSearchVO vo);

    WcRobotConfigVO getRobotById(Long id);

    long saveRobot(WcRobotConfigSaveVO vo);

    void deleteRobot(Long id);

    void updateRobotStatus(Long id, Integer status);

    List<WcPushRuleVO> listRules(Long ownerId);

    WcPushRuleVO getRuleById(Long id);

    long saveRule(WcPushRuleSaveVO vo);

    void deleteRule(Long id);

    void updateRuleStatus(Long id, Integer status);

    PageResultVO<WcMessageLogVO> searchLogs(WcMessageLogSearchVO vo);

    void sendMessage(WcSendMessageVO vo, Long ownerId);
}
