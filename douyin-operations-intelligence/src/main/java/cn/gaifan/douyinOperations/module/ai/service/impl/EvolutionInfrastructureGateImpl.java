package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.service.EvolutionInfrastructureGate;
import cn.gaifan.douyinOperations.module.ai.service.ImportRequirementsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EvolutionInfrastructureGateImpl implements EvolutionInfrastructureGate {

    @Value("${app.ai.evolution-fitness.auto-merge-enabled:false}")
    private boolean autoMergeEnabled;

    @Value("${app.ai.evolution-fitness.infra-check-enabled:true}")
    private boolean infraCheckEnabled;

    @Value("${app.milvus.enabled:true}")
    private boolean milvusEnabledApp;

    @Autowired(required = false)
    private ImportRequirementsService importRequirementsService;

    @Override
    public void assertAutoMergeAllowed() {
        if (!autoMergeEnabled) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "自动合并进化结果至生产知识库未开启（app.ai.evolution-fitness.auto-merge-enabled=false），请使用人工发布或打开开关并评估风险）");
        }
        if (!infraCheckEnabled || importRequirementsService == null) {
            return;
        }
        Map<String, String> r = importRequirementsService.checkImportRequirements();
        String es = r.get("elasticsearch");
        if (es != null) {
            throw new BusinessException(ErrorCode.AI_ES_UNAVAILABLE,
                    "Elasticsearch 不可用，已阻止自动将进化结果写入索引队列: " + es);
        }
        if (milvusEnabledApp) {
            String mv = r.get("milvus");
            if (mv != null) {
                throw new BusinessException(ErrorCode.AI_MILVUS_UNAVAILABLE,
                        "Milvus 不可用，已阻止自动将进化结果写入索引队列: " + mv);
            }
        }
    }
}
