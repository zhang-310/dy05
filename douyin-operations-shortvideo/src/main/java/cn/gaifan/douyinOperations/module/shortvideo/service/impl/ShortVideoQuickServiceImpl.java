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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> quickGenerate(String theme, String keywords, String style, Long ownerId) {
        String title = buildTitle(theme, keywords);
        SvProjectSaveVO projectVo = new SvProjectSaveVO();
        projectVo.setTitle(title);
        projectVo.setProjectType("daily");
        projectVo.setStatus("draft");
        Long projectId = projectService.save(projectVo, ownerId);

        String scriptContent = scriptService.generate("daily", theme, null, keywords, style, 30, ownerId);
        SvScriptSaveVO scriptVo = new SvScriptSaveVO();
        scriptVo.setTitle(title);
        scriptVo.setContent(scriptContent);
        scriptVo.setScriptType("daily");
        scriptVo.setTheme(theme);
        scriptVo.setStyle(StringUtils.hasText(style) ? style : "温馨");
        Long scriptId = scriptService.save(scriptVo, ownerId);

        var genResult = shotListService.generateWithResult(scriptId, scriptContent, 6, style, ownerId);
        Long shotListId = genResult.shotListId();  // generateWithResult 已创建并保存分镜

        SvProjectSaveVO updateVo = new SvProjectSaveVO();
        updateVo.setId(projectId);
        updateVo.setScriptId(scriptId);
        updateVo.setShotListId(shotListId);
        updateVo.setTitle(title);
        updateVo.setProjectType("daily");
        projectService.save(updateVo, ownerId);

        Map<String, Object> result = new HashMap<>();
        result.put("projectId", projectId);
        result.put("scriptId", scriptId);
        result.put("shotListId", shotListId);
        result.put("scriptContent", scriptContent);
        result.put("title", title);
        return result;
    }

    private static String buildTitle(String theme, String keywords) {
        if (StringUtils.hasText(keywords) && keywords.length() <= 50) return keywords.trim();
        String t = StringUtils.hasText(theme) ? theme : "短视频";
        return t + " - " + (StringUtils.hasText(keywords) ? keywords.substring(0, Math.min(20, keywords.length())) + "..." : "AI 创作");
    }
}
