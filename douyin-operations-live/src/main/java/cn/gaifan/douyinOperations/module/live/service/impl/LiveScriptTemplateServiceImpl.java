package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.entity.LiveScriptTemplate;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptTemplateRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptTemplateService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class LiveScriptTemplateServiceImpl implements LiveScriptTemplateService {

    private static final BigDecimal EFFECTIVENESS_THRESHOLD = new BigDecimal("80");

    @Resource
    private LiveScriptRepository scriptRepository;
    @Resource
    private LiveScriptTemplateRepository templateRepository;

    /** P3-04: 平台行业编码（自动入库的模板继承此行业标记） */
    @Value("${app.live.compliance.industry-code:cosmetics}")
    private String defaultIndustryCode;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importFromHighEffectivenessScripts() {
        List<LiveScript> scripts = scriptRepository.findByEffectivenessScoreGreaterThanEqual(EFFECTIVENESS_THRESHOLD);
        int imported = 0;
        for (LiveScript s : scripts) {
            List<LiveScriptTemplate> existing = templateRepository.findBySourceScriptId(s.getId());
            if (!existing.isEmpty()) continue;
            LiveScriptTemplate t = new LiveScriptTemplate();
            t.setTemplateName("话术-" + s.getScriptType() + "-" + s.getId());
            t.setScriptType(s.getScriptType() != null ? s.getScriptType() : "custom");
            t.setContent(s.getScriptContent());
            t.setEffectivenessScore(s.getEffectivenessScore());
            t.setSourceScriptId(s.getId());
            t.setSourceSessionId(s.getSessionId());
            t.setUsageCount(0);
            t.setStatus(1);
            // P3-04: 自动继承平台行业编码
            t.setIndustryCode(defaultIndustryCode);
            templateRepository.save(t);
            imported++;
        }
        return imported;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LiveScriptTemplate saveFromScript(Long scriptId, String templateName, String category) {
        LiveScript script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "话术不存在"));
        List<LiveScriptTemplate> existing = templateRepository.findBySourceScriptId(scriptId);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        LiveScriptTemplate t = new LiveScriptTemplate();
        t.setTemplateName(templateName != null && !templateName.isBlank() ? templateName : "话术-" + script.getScriptType() + "-" + scriptId);
        t.setScriptType(script.getScriptType() != null ? script.getScriptType() : "custom");
        t.setCategory(category);
        t.setContent(script.getScriptContent());
        t.setEffectivenessScore(script.getEffectivenessScore() != null ? script.getEffectivenessScore() : BigDecimal.ZERO);
        t.setSourceScriptId(scriptId);
        t.setSourceSessionId(script.getSessionId());
        t.setUsageCount(0);
        t.setStatus(1);
        return templateRepository.save(t);
    }

    @Override
    public List<LiveScriptTemplate> listByType(String scriptType, int limit) {
        return templateRepository.findByScriptTypeAndDeletedOrderByEffectivenessScoreDesc(
                scriptType, 0, PageRequest.of(0, Math.min(limit, 50)));
    }
}
