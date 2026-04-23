package cn.gaifan.douyinOperations.module.agent.skill;

import java.util.Map;

/**
 * 技能接口：Agent 对话中可通过指令触发的扩展能力
 */
public interface Skill {

    /** 技能唯一标识 */
    String getName();

    /** 简短描述（用于帮助提示） */
    String getDescription();

    /**
     * 判断用户输入是否匹配此技能（用于指令触发）
     */
    default boolean matches(String input) {
        return false;
    }

    /**
     * 执行技能
     * @param ctx 上下文：userId, agentId, conversationId, params 等
     * @return 执行结果文本
     */
    String execute(SkillContext ctx);

    /**
     * 技能上下文
     */
    record SkillContext(Long userId, Long agentId, Long conversationId, String rawInput, Map<String, Object> params) {}
}
