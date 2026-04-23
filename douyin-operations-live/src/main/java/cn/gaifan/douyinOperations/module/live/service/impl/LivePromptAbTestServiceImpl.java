package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.abtest.entity.AbExperiment;
import cn.gaifan.douyinOperations.module.abtest.entity.AbVariant;
import cn.gaifan.douyinOperations.module.abtest.repository.AbExperimentRepository;
import cn.gaifan.douyinOperations.module.abtest.repository.AbVariantRepository;
import cn.gaifan.douyinOperations.module.live.service.LivePromptAbTestService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Prompt A/B 测试服务实现
 * <p>
 * 复用 ab_experiment / ab_variant 表结构：
 * - experiment_type = 'live_prompt'
 * - ab_variant.content 存储 JSON 格式的 prompt 变体配置：
 *   {"systemPromptOverride": "...", "userPromptAppend": "...", "scriptType": "...", "liveFormat": "..."}
 */
@Service
public class LivePromptAbTestServiceImpl implements LivePromptAbTestService {

    private static final Logger log = LoggerFactory.getLogger(LivePromptAbTestServiceImpl.class);
    private static final String EXPERIMENT_TYPE = "live_prompt";
    private static final Random RNG = new Random();

    @Resource
    private AbExperimentRepository experimentRepository;

    @Resource
    private AbVariantRepository variantRepository;

    @Override
    public Map<String, PromptVariant> getActiveVariants(Long userId, Long sessionId,
                                                          String scriptType, String liveFormat) {
        Map<String, PromptVariant> result = new LinkedHashMap<>();
        if (userId == null) return result;

        try {
            // 查询活跃的 prompt 实验（owner = userId，type = live_prompt，status = 1）
            List<AbExperiment> experiments = experimentRepository
                    .findByStatusAndDeleted(1, 0).stream()
                    .filter(e -> {
                        String expType = e.getExperimentType();
                        Long expOwnerId = e.getOwnerId();
                        return EXPERIMENT_TYPE.equals(expType) && userId.equals(expOwnerId);
                    })
                    .toList();

            for (AbExperiment exp : experiments) {
                List<AbVariant> variants = variantRepository
                        .findByExperimentIdAndDeleted(exp.getId(), 0);

                for (AbVariant v : variants) {
                    if (v.getContent() == null || v.getContent().isBlank()) continue;
                    try {
                        JSONObject cfg = JSON.parseObject(v.getContent());
                        // 检查变体是否适用于当前 scriptType / liveFormat
                        String vScriptType = cfg.getString("scriptType");
                        String vLiveFormat = cfg.getString("liveFormat");
                        if (vScriptType != null && !vScriptType.isBlank()
                                && !vScriptType.equals(scriptType)) continue;
                        if (vLiveFormat != null && !vLiveFormat.isBlank()
                                && !vLiveFormat.equals(liveFormat)) continue;

                        String systemPromptOverride = cfg.getString("systemPromptOverride");
                        String userPromptAppend = cfg.getString("userPromptAppend");
                        double weight = cfg.getDoubleValue("trafficWeight") > 0
                                ? cfg.getDoubleValue("trafficWeight") : 0.5;

                        result.put(String.valueOf(v.getId()), new PromptVariant(
                                v.getId(), v.getVariantName(),
                                systemPromptOverride, userPromptAppend, weight
                        ));
                    } catch (Exception e) {
                        log.debug("[PromptAB] 跳过无效变体配置 variantId={}: {}", v.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[PromptAB] 查询活跃实验失败: {}", e.getMessage());
        }

        return result;
    }

    @Override
    public PromptVariant pickVariant(Map<String, PromptVariant> variants) {
        if (variants == null || variants.isEmpty()) return null;
        List<Map.Entry<String, PromptVariant>> entries = new ArrayList<>(variants.entrySet());
        double totalWeight = entries.stream().mapToDouble(e -> e.getValue().trafficWeight()).sum();
        if (totalWeight <= 0) return entries.get(0).getValue();

        double rand = RNG.nextDouble() * totalWeight;
        double cumulative = 0;
        for (Map.Entry<String, PromptVariant> entry : entries) {
            cumulative += entry.getValue().trafficWeight();
            if (rand <= cumulative) return entry.getValue();
        }
        return entries.get(entries.size() - 1).getValue();
    }

    @Override
    @Transactional
    public void recordVariantResult(Long variantId, Long scriptId, double effectivenessScore, double gmvDelta) {
        if (variantId == null) return;
        try {
            // 更新变体的 view_count 和 conversion_count
            variantRepository.incrementViewCount(variantId);
            if (effectivenessScore >= 60 || gmvDelta > 0) {
                variantRepository.incrementConversionCount(variantId);
            }
            log.debug("[PromptAB] 记录变体结果 variantId={} score={} gmvDelta={}",
                    variantId, effectivenessScore, gmvDelta);
        } catch (Exception e) {
            log.debug("[PromptAB] 记录变体结果失败 variantId={}: {}", variantId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public Long createPromptExperiment(Long ownerId, String name, String scriptType,
                                        String liveFormat, Map<String, String> variantConfigs) {
        if (ownerId == null || variantConfigs == null || variantConfigs.isEmpty()) {
            throw new IllegalArgumentException("参数不完整");
        }

        // 创建实验
        AbExperiment experiment = new AbExperiment();
        experiment.setOwnerId(ownerId);
        experiment.setName(name);
        experiment.setExperimentType(EXPERIMENT_TYPE);
        experiment.setStatus(1); // 直接运行中
        experiment.setStartTime(new Timestamp(System.currentTimeMillis()));
        experiment.setTargetEntityType("live_prompt");
        experiment.setDescription("Prompt A/B 实验: scriptType=" + scriptType + " liveFormat=" + liveFormat);
        AbExperiment saved = experimentRepository.save(experiment);

        // 创建变体
        String[] typeNames = {"A", "B", "C", "D"};
        int idx = 0;
        double equalWeight = 1.0 / variantConfigs.size();
        for (Map.Entry<String, String> entry : variantConfigs.entrySet()) {
            AbVariant variant = new AbVariant();
            variant.setExperimentId(saved.getId());
            variant.setVariantName(entry.getKey());
            variant.setVariantType(idx < typeNames.length ? typeNames[idx] : String.valueOf((char)('A' + idx)));

            // 将 systemPromptOverride 和元信息序列化为 content JSON
            Map<String, Object> cfg = new HashMap<>();
            cfg.put("systemPromptOverride", entry.getValue());
            cfg.put("scriptType", scriptType);
            cfg.put("liveFormat", liveFormat);
            cfg.put("trafficWeight", equalWeight);
            variant.setContent(JSON.toJSONString(cfg));
            variantRepository.save(variant);
            idx++;
        }

        log.info("[PromptAB] 创建 Prompt A/B 实验 experimentId={} name={} variants={}",
                saved.getId(), name, variantConfigs.size());
        return saved.getId();
    }
}
