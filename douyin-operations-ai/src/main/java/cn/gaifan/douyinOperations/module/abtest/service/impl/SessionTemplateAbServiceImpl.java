package cn.gaifan.douyinOperations.module.abtest.service.impl;

import cn.gaifan.douyinOperations.module.abtest.entity.AbExperiment;
import cn.gaifan.douyinOperations.module.abtest.entity.AbVariant;
import cn.gaifan.douyinOperations.module.abtest.repository.AbExperimentRepository;
import cn.gaifan.douyinOperations.module.abtest.repository.AbVariantRepository;
import cn.gaifan.douyinOperations.module.abtest.service.AbTestService;
import cn.gaifan.douyinOperations.module.abtest.service.SessionTemplateAbService;
import cn.gaifan.douyinOperations.module.abtest.vo.AbEventSaveVO;
import cn.gaifan.douyinOperations.module.abtest.vo.SessionTemplateAssignVO;
import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SessionTemplateAbServiceImpl implements SessionTemplateAbService {

    private static final Logger log = LoggerFactory.getLogger(SessionTemplateAbServiceImpl.class);
    private static final String EXPERIMENT_TYPE = "session_template";
    private static final String TARGET_LIVE_ACCOUNT = "live_account";
    private static final int STATUS_RUNNING = 1;

    @Resource
    private AbExperimentRepository experimentRepository;
    @Resource
    private AbVariantRepository variantRepository;
    @Resource
    private AbTestService abTestService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SessionTemplateAssignVO assignTemplateForNewSession(Long ownerId, Long accountId, String userFingerprint) {
        if (ownerId == null || accountId == null) {
            return null;
        }
        List<AbExperiment> list = experimentRepository.findByExperimentTypeAndTargetEntityTypeAndTargetEntityIdAndStatusAndDeleted(
                EXPERIMENT_TYPE, TARGET_LIVE_ACCOUNT, accountId, STATUS_RUNNING, 0);
        if (list == null || list.isEmpty()) {
            return null;
        }
        AbExperiment exp = list.get(0);
        List<AbVariant> variants = variantRepository.findByExperimentIdAndDeleted(exp.getId(), 0);
        if (variants == null || variants.size() < 2) {
            return null;
        }
        AbVariant chosen = variants.get(ThreadLocalRandom.current().nextInt(variants.size()));
        Long templateId = parseTemplateIdFromVariant(chosen);
        if (templateId == null || templateId <= 0) {
            return null;
        }
        SessionTemplateAssignVO vo = new SessionTemplateAssignVO(
                exp.getId(),
                chosen.getId(),
                templateId,
                chosen.getVariantType() != null ? chosen.getVariantType() : "A");
        recordTemplateView(exp.getId(), chosen.getId(), userFingerprint);
        return vo;
    }

    private Long parseTemplateIdFromVariant(AbVariant v) {
        if (v == null) {
            return null;
        }
        if (v.getEntityId() != null && v.getEntityId() > 0
                && "live_session_template".equalsIgnoreCase(String.valueOf(v.getEntityType()))) {
            return v.getEntityId();
        }
        String content = v.getContent();
        if (!StringUtils.hasText(content)) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = JSON.parseObject(content, HashMap.class);
            if (m == null) {
                return null;
            }
            Object tid = m.get("templateId");
            if (tid instanceof Number n) {
                return n.longValue();
            }
            if (tid != null) {
                return Long.parseLong(tid.toString());
            }
        } catch (Exception ignored) {
            log.debug("解析templateId失败: content={}", content);
        }
        return null;
    }

    private void recordTemplateView(Long experimentId, Long variantId, String userFingerprint) {
        if (experimentId == null || variantId == null || !StringUtils.hasText(userFingerprint)) {
            return;
        }
        AbEventSaveVO vo = new AbEventSaveVO();
        vo.setExperimentId(experimentId);
        vo.setVariantId(variantId);
        vo.setEventType("view");
        vo.setUserFingerprint(userFingerprint);
        abTestService.recordEvent(vo);
    }
}
