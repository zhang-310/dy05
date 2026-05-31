package cn.gaifan.douyinOperations.module.abtest.service.impl;

import cn.gaifan.douyinOperations.module.abtest.entity.AbExperiment;
import cn.gaifan.douyinOperations.module.abtest.entity.AbVariant;
import cn.gaifan.douyinOperations.module.abtest.repository.AbExperimentRepository;
import cn.gaifan.douyinOperations.module.abtest.repository.AbVariantRepository;
import cn.gaifan.douyinOperations.module.abtest.service.AbTestService;
import cn.gaifan.douyinOperations.module.abtest.service.ScriptStyleAbService;
import cn.gaifan.douyinOperations.module.abtest.vo.AbEventSaveVO;
import cn.gaifan.douyinOperations.module.abtest.vo.ScriptStyleAssignVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 话术风格 A/B：按目标实体查找运行中实验，随机分配 A/B，并记录 view/conversion
 */
@Service
public class ScriptStyleAbServiceImpl implements ScriptStyleAbService {

    private static final String EXPERIMENT_TYPE = "script_style";
    private static final int STATUS_RUNNING = 1;

    @Resource
    private AbExperimentRepository experimentRepository;
    @Resource
    private AbVariantRepository variantRepository;
    @Resource
    private AbTestService abTestService;

    @Override
    public ScriptStyleAssignVO assignStyle(Long ownerId, String targetEntityType, Long targetEntityId, String userFingerprint) {
        if (ownerId == null || targetEntityType == null || targetEntityId == null) {
            return null;
        }
        List<AbExperiment> list = experimentRepository.findByExperimentTypeAndTargetEntityTypeAndTargetEntityIdAndStatusAndDeleted(
                EXPERIMENT_TYPE, targetEntityType, targetEntityId, STATUS_RUNNING, 0);
        if (list == null || list.isEmpty()) {
            return null;
        }
        AbExperiment exp = list.get(0);
        List<AbVariant> variants = variantRepository.findByExperimentIdAndDeleted(exp.getId(), 0);
        if (variants == null || variants.size() < 2) {
            return null;
        }
        // 随机选 A 或 B（50/50）
        AbVariant chosen = variants.get(ThreadLocalRandom.current().nextInt(variants.size()));
        ScriptStyleAssignVO vo = new ScriptStyleAssignVO(
                exp.getId(), chosen.getId(),
                chosen.getStyleCode() != null ? chosen.getStyleCode() : chosen.getVariantType(),
                chosen.getVariantType());
        recordView(exp.getId(), chosen.getId(), userFingerprint);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordView(Long experimentId, Long variantId, String userFingerprint) {
        if (experimentId == null || variantId == null || userFingerprint == null || userFingerprint.isBlank()) {
            return;
        }
        AbEventSaveVO vo = new AbEventSaveVO();
        vo.setExperimentId(experimentId);
        vo.setVariantId(variantId);
        vo.setEventType("view");
        vo.setUserFingerprint(userFingerprint);
        abTestService.recordEvent(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordConversion(Long experimentId, Long variantId, String userFingerprint) {
        if (experimentId == null || variantId == null || userFingerprint == null || userFingerprint.isBlank()) {
            return;
        }
        AbEventSaveVO vo = new AbEventSaveVO();
        vo.setExperimentId(experimentId);
        vo.setVariantId(variantId);
        vo.setEventType("conversion");
        vo.setUserFingerprint(userFingerprint);
        abTestService.recordEvent(vo);
    }
}
