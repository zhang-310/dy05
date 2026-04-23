package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiInferenceAudit;
import cn.gaifan.douyinOperations.module.ai.repository.AiInferenceAuditRepository;
import cn.gaifan.douyinOperations.module.ai.service.AiInferenceAuditService;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.ai.inference-audit.enabled", havingValue = "true", matchIfMissing = false)
public class AiInferenceAuditServiceImpl implements AiInferenceAuditService {

    private final AiInferenceAuditRepository repository;

    @Value("${app.ai.inference-audit.log-failures-only:false}")
    private boolean logFailuresOnly;

    public AiInferenceAuditServiceImpl(AiInferenceAuditRepository repository) {
        this.repository = repository;
    }

    @Override
    public void record(String capability, String provider, String modelRef, int inputChars, boolean ok, long latencyMs, String errorMessage, String metaJson) {
        if (logFailuresOnly && ok) {
            return;
        }
        try {
            AiInferenceAudit row = new AiInferenceAudit();
            row.setTraceId(MDC.get("traceId"));
            row.setCapability(capability != null ? capability : "unknown");
            row.setProvider(provider != null ? provider : "");
            row.setModelRef(modelRef != null ? modelRef : "");
            row.setInputChars(inputChars);
            row.setOk(ok ? 1 : 0);
            row.setLatencyMs((int) Math.min(latencyMs, Integer.MAX_VALUE));
            if (errorMessage != null && errorMessage.length() > 500) {
                errorMessage = errorMessage.substring(0, 500);
            }
            row.setErrorMessage(errorMessage);
            row.setMetaJson(metaJson);
            repository.save(row);
        } catch (Exception e) {
            // 审计失败不影响主流程
        }
    }
}
