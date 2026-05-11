package cn.gaifan.douyinOperations.module.agent.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentWorkflowExecutionVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentWorkflowSaveVO;
import cn.gaifan.douyinOperations.module.agent.vo.AgentWorkflowVO;

import java.util.Map;

/**
 * 智能体协作编排服务接口
 */
public interface AgentWorkflowService {

    /**
     * 分页查询用户的工作流
     */
    PageResultVO<AgentWorkflowVO> list(Long userId, int page, int rows);

    /**
     * 获取工作流详情（含步骤）
     */
    AgentWorkflowVO getById(Long workflowId);

    /**
     * 保存工作流（新增或更新）
     */
    long save(Long userId, AgentWorkflowSaveVO vo);

    /**
     * 删除工作流
     */
    void delete(Long workflowId, Long userId);

    /**
     * 执行工作流编排
     * @param workflowId 工作流ID
     * @param userId 用户ID
     * @param conversationId 会话ID（可选）
     * @param userInput 用户输入
     * @return 各步骤的执行结果
     */
    Map<String, Object> execute(Long workflowId, Long userId, Long conversationId, String userInput);

    /**
     * 查询用户的执行历史
     */
    PageResultVO<AgentWorkflowExecutionVO> getExecutionHistory(Long userId, int page, int rows);

    /**
     * 查询某工作流的执行历史
     */
    PageResultVO<AgentWorkflowExecutionVO> getWorkflowExecutions(Long userId, Long workflowId, int page, int rows);

    /**
     * 获取执行详情
     */
    AgentWorkflowExecutionVO getExecutionById(Long executionId, Long userId);
}
