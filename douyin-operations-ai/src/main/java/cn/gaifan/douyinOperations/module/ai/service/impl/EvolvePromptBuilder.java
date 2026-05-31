package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveReport;
import cn.gaifan.douyinOperations.module.ai.entity.AiEvolveTask;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveReportRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiEvolveTaskRepository;
import cn.gaifan.douyinOperations.module.ai.service.AiPromptConfigService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 进化引擎 Prompt 构建器：负责系统提示词、进化提示词、方法论黑名单、上轮参考等 Prompt 组装逻辑。
 */
@Component
public class EvolvePromptBuilder {

    @Value("${app.ai.evolution.strategy-live-chars-cap:600}")
    private int strategyLiveCharsCap;

    @Resource
    private AiPromptConfigService aiPromptConfigService;

    @Resource
    private AiEvolveTaskRepository taskRepository;

    @Resource
    private AiEvolveReportRepository reportRepository;

    // ---- system prompts ----

    String getSystemPrompt() {
        return aiPromptConfigService.getPrompt("ai.prompt.evolve.system",
                "你是一位抖音行业运营专家，同时具备方法论提炼、失败复盘和可执行方案设计能力。");
    }

    String getHuashuSystemPrompt() {
        return aiPromptConfigService.getPrompt("ai.prompt.evolve.huashu.system",
                "你是一位资深直播话术教练，擅长提炼可直接使用的直播话术。输出的每条话术都应是主播能直接照着念的完整句子，不要输出方法论或分析，只要实战话术。");
    }

    String getZhishiSystemPrompt() {
        return aiPromptConfigService.getPrompt("ai.prompt.evolve.zhishi.system",
                "你是一位资深技术架构师，擅长提炼可执行的技术最佳实践。输出应含具体代码示例、配置要点或架构模式，避免空洞表述。");
    }

    // ---- user prompts ----

    String buildEvolvePrompt(String context, String evolveAngle, String qualityHint,
                             String methodologyBlacklist, String lastRoundRef) {
        return "以下是本轮进化的上下文资料：\n\n" + (context != null ? context : "（暂无相关知识）") + "\n\n" +
                "进化角度要求：" + evolveAngle + "\n\n" + qualityHint + "\n" +
                (methodologyBlacklist != null && !methodologyBlacklist.isBlank() ? "方法论去重黑名单：\n" + methodologyBlacklist + "\n" : "") +
                (lastRoundRef != null && !lastRoundRef.isBlank() ? "上轮结论参考：\n" + lastRoundRef + "\n" : "") +
                "\n请严格按以下 3 个章节输出：\n\n## 方法论提炼\n- 至少 3 条，推荐 5 条\n" +
                "- 格式：1 句话概念 + 1 句话实操\n- 必须包含「谁在什么场景下做什么」\n" +
                "- 用具体数据：「前3秒」「转化率>X%」\n- 含可执行 SOP（步骤 1-2-3 或检查清单）\n" +
                "- 如有失败案例，用 [防踩坑] 标签\n\n## 待深化问题\n- 至少 3 个触及知识盲区的问题\n" +
                "- 每个问题必须包含「依据：xxx」说明为何需要深化\n- 避免与已有问题重复\n\n## 可迭代建议\n" +
                "- 至少 2 条可操作建议\n- 优先含指标/阈值/A/B测试\n- 关联行业基准";
    }

    String buildHuashuEvolvePrompt(String context, String evolveAngle, String qualityHint,
                                   String methodologyBlacklist, String lastRoundRef) {
        return "以下是本轮话术进化的参考资料：\n\n" + (context != null ? context : "（暂无相关话术）") + "\n\n" +
                "进化角度：" + evolveAngle + "\n\n" + qualityHint + "\n" +
                (methodologyBlacklist != null && !methodologyBlacklist.isBlank() ? "避免重复以下表述：\n" + methodologyBlacklist + "\n" : "") +
                (lastRoundRef != null && !lastRoundRef.isBlank() ? "上轮参考：\n" + lastRoundRef + "\n" : "") +
                "\n请输出可直接在直播中念的话术片段，格式要求：\n\n## 话术片段\n" +
                "- 至少 5 条可直接使用的话术\n- 每条 50–150 字，口语化、有感染力\n" +
                "- 标注适用场景：如「开场留人」「产品卖点」「互动促单」「收尾秒杀」\n" +
                "- 避免空洞说教，必须是主播能直接念的句子\n\n## 使用建议\n- 1–2 条简短使用提示";
    }

    String buildZhishiEvolvePrompt(String context, String evolveAngle, String qualityHint,
                                   String methodologyBlacklist, String lastRoundRef) {
        return "以下是本轮技术知识进化的参考资料：\n\n" + (context != null ? context : "（暂无相关知识）") + "\n\n" +
                "进化角度：" + evolveAngle + "\n\n" + qualityHint + "\n" +
                (methodologyBlacklist != null && !methodologyBlacklist.isBlank() ? "避免重复：\n" + methodologyBlacklist + "\n" : "") +
                (lastRoundRef != null && !lastRoundRef.isBlank() ? "上轮参考：\n" + lastRoundRef + "\n" : "") +
                "\n请严格按以下 3 个章节输出：\n\n## 技术要点提炼\n- 至少 3 条可执行的技术建议\n" +
                "- 含代码片段、架构模式或配置示例\n- 标注适用场景与前提条件\n- 如有踩坑经验，用 [防踩坑] 标签\n\n## 待深化问题\n" +
                "- 至少 3 个触及知识盲区的问题\n- 每个问题含「依据：xxx」\n\n## 实践检查清单\n- 至少 2 条步骤 1-2-3 或验收标准";
    }

    // ---- blacklist & reference ----

    String buildMethodologyBlacklist(Long kbId) {
        if (kbId == null) return "";
        List<AiEvolveTask> recentTasks = taskRepository.findRecentCompletedByKbId(kbId, PageRequest.of(0, 3));
        if (recentTasks.isEmpty()) return "";
        List<Long> taskIds = recentTasks.stream().map(AiEvolveTask::getId).toList();
        List<AiEvolveReport> reports = reportRepository.findByTaskIdInOrderByCreateTimeDesc(taskIds);
        if (reports.isEmpty()) return "";
        List<String> bullets = new ArrayList<>();
        for (AiEvolveReport r : reports) {
            String section = r.getMethodologySection();
            if (section == null || section.isBlank()) continue;
            for (String line : section.split("\n")) {
                String trimmed = line.replaceAll("^[-*•\\d.]\\s*", "").trim();
                if (trimmed.length() >= 10 && !bullets.contains(trimmed)) {
                    bullets.add(trimmed);
                    if (bullets.size() >= 5) break;
                }
            }
            if (bullets.size() >= 5) break;
        }
        if (bullets.isEmpty()) return "";
        return "方法论去重黑名单（请勿重复输出以下内容）：\n" + String.join("\n", bullets);
    }

    String buildLastRoundRef(Long kbId) {
        if (kbId == null) return "";
        List<AiEvolveTask> recentTasks = taskRepository.findRecentCompletedByKbId(kbId, PageRequest.of(0, 1));
        if (recentTasks.isEmpty()) return "";
        var opt = reportRepository.findByTaskId(recentTasks.get(0).getId());
        if (opt.isEmpty()) return "";
        AiEvolveReport r = opt.get();
        String methodology = r.getMethodologySection() != null ? r.getMethodologySection() : "";
        String iterate = r.getIterateSection() != null ? r.getIterateSection() : "";
        String combined = (methodology + "\n" + iterate).trim();
        if (combined.length() > strategyLiveCharsCap) combined = combined.substring(0, strategyLiveCharsCap - 3) + "...";
        return combined.isEmpty() ? "" : "上轮结论参考：\n" + combined;
    }
}
