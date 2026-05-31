package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoQuickService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvProjectService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvScriptService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvShotListService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptSaveVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 快速生成服务实现
 */
@Service
public class ShortVideoQuickServiceImpl implements ShortVideoQuickService {

    @Resource
    private SvProjectService projectService;
    @Resource
    private SvScriptService scriptService;
    @Resource
    private SvShotListService shotListService;
    @Resource
    private ShortVideoCreativePlanner creativePlanner;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> quickGenerate(String theme, String keywords, String style, Long ownerId) {
        String normalizedTheme = StringUtils.hasText(theme) ? theme.trim() : "短视频选题";
        String normalizedKeywords = StringUtils.hasText(keywords) ? keywords.trim() : "";
        String normalizedStyle = StringUtils.hasText(style) ? style.trim() : "专业干货";
        int durationSeconds = inferDurationSeconds(normalizedStyle, normalizedKeywords);
        Map<String, Object> creativeBrief = creativePlanner.buildCreativeBrief(
                normalizedTheme, normalizedKeywords, normalizedStyle, durationSeconds);
        String briefBlock = creativePlanner.toPromptBlock(creativeBrief);
        String title = buildTitle(normalizedTheme, normalizedKeywords);
        SvProjectSaveVO projectVo = new SvProjectSaveVO();
        projectVo.setTitle(title);
        projectVo.setProjectType("daily");
        projectVo.setStatus("draft");
        projectVo.setDuration(durationSeconds);
        Long projectId = projectService.save(projectVo, ownerId);

        String scriptSeed = briefBlock + "\n【补充输入】\n"
                + "- 用户主题：" + normalizedTheme + "\n"
                + "- 用户关键词/参考：" + (StringUtils.hasText(normalizedKeywords) ? normalizedKeywords : "无") + "\n";
        String scriptContent = scriptService.generate("daily", normalizedTheme, null,
                scriptSeed, normalizedStyle, durationSeconds, ownerId);
        SvScriptSaveVO scriptVo = new SvScriptSaveVO();
        scriptVo.setTitle(title);
        scriptVo.setContent(scriptContent);
        scriptVo.setScriptType("daily");
        scriptVo.setGenerationType("ai");
        scriptVo.setTheme(normalizedTheme);
        scriptVo.setStyle(normalizedStyle);
        scriptVo.setDuration(durationSeconds);
        scriptVo.setWordCount(scriptContent.replaceAll("\\s+", "").length());
        scriptVo.setTags(creativePlanner.toJson(Map.of("creativeBrief", creativeBrief)));
        scriptVo.setAiPrompt(scriptSeed);
        Long scriptId = scriptService.save(scriptVo, ownerId);

        var genResult = shotListService.generateWithResult(scriptId,
                briefBlock + "\n\n【脚本正文】\n" + scriptContent,
                inferShotCount(durationSeconds), normalizedStyle, ownerId);
        Long shotListId = genResult.shotListId();  // generateWithResult 已创建并保存分镜

        SvProjectSaveVO updateVo = new SvProjectSaveVO();
        updateVo.setId(projectId);
        updateVo.setScriptId(scriptId);
        updateVo.setShotListId(shotListId);
        updateVo.setTitle(title);
        updateVo.setProjectType("daily");
        updateVo.setStatus("processing");
        updateVo.setDuration(durationSeconds);
        projectService.save(updateVo, ownerId);

        Map<String, Object> result = new HashMap<>();
        result.put("projectId", projectId);
        result.put("scriptId", scriptId);
        result.put("shotListId", shotListId);
        result.put("scriptContent", scriptContent);
        result.put("title", title);
        result.put("creativeBrief", creativeBrief);
        result.put("productionPlan", buildProductionPlan(projectId, scriptId, shotListId, creativeBrief));
        return result;
    }

    private static String buildTitle(String theme, String keywords) {
        if (StringUtils.hasText(keywords) && keywords.length() <= 50) return keywords.trim();
        String t = StringUtils.hasText(theme) ? theme : "短视频";
        return t + " - " + (StringUtils.hasText(keywords) ? keywords.substring(0, Math.min(20, keywords.length())) + "..." : "AI 创作");
    }

    private static int inferDurationSeconds(String style, String keywords) {
        String merged = ((style == null ? "" : style) + " " + (keywords == null ? "" : keywords)).toLowerCase();
        if (merged.contains("口播") || merged.contains("教程") || merged.contains("干货")) {
            return 60;
        }
        if (merged.contains("剧情") || merged.contains("故事")) {
            return 75;
        }
        return 45;
    }

    private static int inferShotCount(int durationSeconds) {
        return Math.max(5, Math.min(12, (int) Math.ceil(durationSeconds / 7.0)));
    }

    private static Map<String, Object> buildProductionPlan(Long projectId, Long scriptId, Long shotListId,
                                                           Map<String, Object> creativeBrief) {
        Map<String, Object> plan = new HashMap<>();
        plan.put("projectId", projectId);
        plan.put("scriptId", scriptId);
        plan.put("shotListId", shotListId);
        plan.put("currentStage", "shot_planning");
        plan.put("nextActions", java.util.List.of(
                "检查脚本前三秒钩子和风险词",
                "在分镜页补齐关键帧提示词、镜头运动和时长",
                "按素材计划上传主体、证据、转场和封面素材",
                "生成关键帧/视频片段后进入自动合成",
                "发布前检查标题、标签、字幕和合规风险"
        ));
        plan.put("acceptanceCriteria", creativeBrief.get("acceptanceCriteria"));
        return plan;
    }
}
