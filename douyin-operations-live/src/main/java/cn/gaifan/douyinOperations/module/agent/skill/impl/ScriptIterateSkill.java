package cn.gaifan.douyinOperations.module.agent.skill.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.agent.service.UserPreferenceService;
import cn.gaifan.douyinOperations.module.agent.skill.Skill;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveAiService;
import org.springframework.stereotype.Component;

/**
 * 话术迭代技能：调用 LiveAiService.refineScript
 * 指令格式：/iterate 123 修改得更热情 或 /话术迭代 123 加入促单话术
 */
@Component
public class ScriptIterateSkill implements Skill {

    private static final String[] TRIGGERS = {"iterate", "script_iterate", "话术迭代", "迭代话术"};

    @jakarta.annotation.Resource
    private LiveAiService liveAiService;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private UserPreferenceService userPreferenceService;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private LiveScriptRepository liveScriptRepository;

    @Override
    public String getName() {
        return "script_iterate";
    }

    @Override
    public boolean matches(String input) {
        return matchesInput(input);
    }

    @Override
    public String getDescription() {
        return "话术迭代：/iterate <scriptId> <修改指令>，根据指令 AI 优化直播话术";
    }

    @Override
    public String execute(SkillContext ctx) {
        Skill.SkillContext parsed = parseContext(ctx.rawInput(), ctx.userId(), ctx.agentId(), ctx.conversationId());
        Long scriptId = parsed.params() != null && parsed.params().get("scriptId") != null
                ? Long.parseLong(parsed.params().get("scriptId").toString())
                : null;
        String instruction = parsed.params() != null && parsed.params().get("instruction") != null
                ? parsed.params().get("instruction").toString()
                : "";
        if (scriptId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "请提供话术 ID，格式：/iterate <scriptId> <修改指令>");
        }
        if (instruction == null || instruction.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "请提供修改指令");
        }
        String result = liveAiService.refineScript(scriptId, instruction, ctx.userId());
        if (userPreferenceService != null) {
            userPreferenceService.record(ctx.userId(), "instruction_used", instruction);
            if (liveScriptRepository != null) {
                liveScriptRepository.findById(scriptId).map(LiveScript::getScriptType)
                        .filter(t -> t != null && !t.isBlank())
                        .ifPresent(st -> userPreferenceService.record(ctx.userId(), "script_type", st));
            }
        }
        return result != null ? result : "迭代完成，但未返回新内容";
    }

    public static boolean matchesInput(String content) {
        if (content == null || content.isBlank()) return false;
        String lower = content.trim().toLowerCase();
        for (String t : TRIGGERS) {
            if (lower.startsWith("/" + t + " ") || lower.startsWith(t + " ")) return true;
        }
        return false;
    }

    public static Skill.SkillContext parseContext(String rawInput, Long userId, Long agentId, Long conversationId) {
        String rest = rawInput;
        for (String t : TRIGGERS) {
            String prefix = "/" + t + " ";
            if (rest.toLowerCase().startsWith(prefix)) {
                rest = rest.substring(prefix.length()).trim();
                break;
            }
            String p2 = t + " ";
            if (rest.toLowerCase().startsWith(p2)) {
                rest = rest.substring(p2.length()).trim();
                break;
            }
        }
        Long scriptId = null;
        String instruction = rest;
        int firstSpace = rest.indexOf(' ');
        if (firstSpace > 0) {
            try {
                scriptId = Long.parseLong(rest.substring(0, firstSpace));
                instruction = rest.substring(firstSpace + 1).trim();
            } catch (NumberFormatException ignored) {}
        }
        return new Skill.SkillContext(userId, agentId, conversationId, rawInput,
                scriptId != null ? java.util.Map.of("scriptId", scriptId, "instruction", instruction) : java.util.Map.of("instruction", instruction));
    }
}
