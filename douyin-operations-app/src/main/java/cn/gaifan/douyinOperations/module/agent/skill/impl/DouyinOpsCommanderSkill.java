package cn.gaifan.douyinOperations.module.agent.skill.impl;

import cn.gaifan.douyinOperations.module.agent.skill.Skill;
import cn.gaifan.douyinOperations.module.ai.service.DouyinOpsCommanderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class DouyinOpsCommanderSkill implements Skill {

    @Resource
    private DouyinOpsCommanderService commanderService;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public String getName() {
        return "douyin_ops_commander";
    }

    @Override
    public String getDescription() {
        return "运营总控：汇总官方知识库、AI引用闭环、短视频分布式采集队列和下一步动作建议";
    }

    @Override
    public boolean matches(String input) {
        if (input == null) {
            return false;
        }
        String text = input.toLowerCase();
        return text.contains("运营总控")
                || text.contains("总控")
                || text.contains("采集队列")
                || text.contains("douyin_ops_commander");
    }

    @Override
    public String execute(SkillContext ctx) {
        try {
            return objectMapper.writeValueAsString(commanderService.buildBrief(ctx.userId()));
        } catch (Exception e) {
            return "运营总控生成失败: " + e.getMessage();
        }
    }
}
