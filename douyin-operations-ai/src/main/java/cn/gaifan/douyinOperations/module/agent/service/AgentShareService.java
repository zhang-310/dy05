package cn.gaifan.douyinOperations.module.agent.service;

import cn.gaifan.douyinOperations.module.agent.vo.AgentShareRequestVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentShareVO;

import java.util.List;

public interface AgentShareService {

    /**
     * 创建分享
     */
    AgentShareVO createShare(Long userId, AgentShareRequestVO request);

    /**
     * 通过分享码获取分享信息
     */
    AgentShareVO getByShareCode(String shareCode);

    /**
     * 获取用户的分享列表
     */
    List<AgentShareVO> listByUser(Long userId);

    /**
     * 删除分享
     */
    void deleteShare(Long userId, Long shareId);

    /**
     * 获取分享的对话消息（用于导入）
     */
    Object getShareConversationData(String shareCode);
}
