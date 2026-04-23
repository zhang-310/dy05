package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.util.RequestRoleResolver;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveSessionShortVideoExportService;
import cn.gaifan.douyinOperations.module.live.vo.LiveSessionExportToShortVideoResultVO;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvProjectService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvScriptService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptSaveVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class LiveSessionShortVideoExportServiceImpl implements LiveSessionShortVideoExportService {

    @Resource
    private LiveSessionRepository liveSessionRepository;
    @Resource
    private LiveScriptRepository liveScriptRepository;
    @Resource
    private SvScriptService svScriptService;
    @Resource
    private SvProjectService svProjectService;

    @Override
    public LiveSessionExportToShortVideoResultVO exportToShortVideoProject(Long liveSessionId, Long sessionOwnerId) {
        if (liveSessionId == null || liveSessionId <= 0 || sessionOwnerId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "参数无效");
        }
        LiveSession session;
        if (RequestRoleResolver.isAdmin()) {
            session = liveSessionRepository.findByIdAndDeleted(liveSessionId, 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND, "场次不存在"));
        } else {
            session = liveSessionRepository.findByIdAndUserIdAndDeleted(liveSessionId, sessionOwnerId, 0)
                    .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND, "场次不存在或无权限"));
        }
        List<LiveScript> scripts = liveScriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(liveSessionId, 0);
        if (scripts.isEmpty()) {
            throw new BusinessException(ErrorCode.LIVE_NO_SCRIPTS_FOUND, "该场次无话术，无法导出");
        }

        StringBuilder md = new StringBuilder();
        md.append("# 来自直播场次：").append(session.getLiveTitle() != null ? session.getLiveTitle() : ("ID " + liveSessionId)).append("\n\n");
        int n = 1;
        for (LiveScript s : scripts) {
            md.append("## 槽位 ").append(n++).append(" · ")
                    .append(s.getScriptType() != null ? s.getScriptType() : "custom").append("\n\n");
            md.append(s.getScriptContent() != null ? s.getScriptContent().trim() : "").append("\n\n");
        }
        String body = md.toString().trim();
        int wc = body.length();

        Set<Long> related = new LinkedHashSet<>();
        for (LiveScript s : scripts) {
            if (s.getProductId() != null && s.getProductId() > 0) {
                related.add(s.getProductId());
            }
        }

        SvScriptSaveVO scriptVo = new SvScriptSaveVO();
        scriptVo.setTitle(StringUtils.hasText(session.getLiveTitle())
                ? "直播转短视频·" + session.getLiveTitle()
                : "直播转短视频·场次" + liveSessionId);
        scriptVo.setContent(body);
        scriptVo.setScriptType("daily");
        scriptVo.setGenerationType("from_live");
        scriptVo.setTheme("live_session:" + liveSessionId);
        scriptVo.setStyle(session.getScriptStyle());
        scriptVo.setWordCount(wc);
        if (session.getPersonaId() != null) {
            scriptVo.setPersonaId(session.getPersonaId());
        }
        long scriptId = svScriptService.save(scriptVo, sessionOwnerId);

        SvProjectSaveVO proj = new SvProjectSaveVO();
        proj.setTitle(scriptVo.getTitle());
        proj.setProjectType("daily");
        proj.setStatus("draft");
        proj.setScriptId(scriptId);
        proj.setPersonaId(session.getPersonaId());
        proj.setAccountId(session.getAccountId());
        if (!related.isEmpty()) {
            proj.setRelatedProductIds(new ArrayList<>(related));
        }
        long projectId = svProjectService.save(proj, sessionOwnerId);
        return new LiveSessionExportToShortVideoResultVO(scriptId, projectId);
    }
}
