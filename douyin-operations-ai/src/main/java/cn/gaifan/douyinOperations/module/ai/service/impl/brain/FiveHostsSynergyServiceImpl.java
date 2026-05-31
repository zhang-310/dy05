package cn.gaifan.douyinOperations.module.ai.service.impl.brain;

import cn.gaifan.douyinOperations.module.ai.entity.AiHostPersona;
import cn.gaifan.douyinOperations.module.ai.service.brain.FiveHostsSynergyService;
import cn.gaifan.douyinOperations.module.ai.service.brain.HostPersonaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 五位主播协同优化引擎实现
 */
@Service
public class FiveHostsSynergyServiceImpl implements FiveHostsSynergyService {

    @Value("${app.ai.brain.synergy.enabled:true}")
    private boolean enabled;

    @Value("${app.ai.brain.synergy.flow-path-description:}")
    private String flowPathDescription;

    @Value("${app.ai.brain.synergy.transfer-triggers:}")
    private String transferTriggersCsv;

    @Value("${app.ai.brain.synergy.transfer-channels:}")
    private String transferChannelsCsv;

    @Value("${app.ai.brain.synergy.expected-transfer-rate:}")
    private String expectedTransferRate;

    @Value("${app.ai.brain.synergy.transfer-hint:}")
    private String transferHint;

    @Autowired(required = false)
    private HostPersonaService hostPersonaService;

    private static final String DEFAULT_FLOW_DESC = "流量路径: 肖瑶(校园)→肖蝉/阳阳(职场/生活)→智慧/田玲红(B端)";
    private static final String DEFAULT_TRIGGERS = "消费升级需求,创业意向,供应链兴趣";
    private static final String DEFAULT_CHANNELS = "专题内容,直播连麦,社群运营";
    private static final String DEFAULT_RATE = "C端粉丝→B端客户: 3-5%";
    private static final String DEFAULT_HINT = "建议通过专题内容与直播连麦引导高意向用户";

    private List<String> splitCsv(String raw, String fallback) {
        String s = raw != null && !raw.isBlank() ? raw : fallback;
        if (s == null || s.isBlank()) {
            return List.of();
        }
        return Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isEmpty()).toList();
    }

    @Override
    public FlowPath getFlowPath() {
        if (!enabled || hostPersonaService == null) {
            return new FlowPath(List.of(), List.of(), List.of(), "协同未启用");
        }
        List<AiHostPersona> all = hostPersonaService.getFlowPath();
        List<AiHostPersona> entrance = all.stream().filter(p -> p.getFlowPhase() != null && p.getFlowPhase() == 0).toList();
        List<AiHostPersona> conversion = all.stream().filter(p -> p.getFlowPhase() != null && p.getFlowPhase() == 1).toList();
        List<AiHostPersona> sink = all.stream().filter(p -> p.getFlowPhase() != null && p.getFlowPhase() == 2).toList();
        String desc = flowPathDescription != null && !flowPathDescription.isBlank() ? flowPathDescription.trim() : DEFAULT_FLOW_DESC;
        return new FlowPath(entrance, conversion, sink, desc);
    }

    @Override
    public ResourceTransferPlan getTransferPlan() {
        if (!enabled) {
            return new ResourceTransferPlan(List.of(), List.of(), "0%", "未启用");
        }
        return new ResourceTransferPlan(
                splitCsv(transferTriggersCsv, DEFAULT_TRIGGERS),
                splitCsv(transferChannelsCsv, DEFAULT_CHANNELS),
                expectedTransferRate != null && !expectedTransferRate.isBlank() ? expectedTransferRate.trim() : DEFAULT_RATE,
                transferHint != null && !transferHint.isBlank() ? transferHint.trim() : DEFAULT_HINT
        );
    }

    @Override
    public Map<String, Object> getSynergySummary() {
        Map<String, Object> m = new HashMap<>();
        FlowPath path = getFlowPath();
        m.put("flowPath", Map.of(
                "entrance", path.entranceHosts().stream().map(AiHostPersona::getHostName).toList(),
                "conversion", path.conversionHosts().stream().map(AiHostPersona::getHostName).toList(),
                "sink", path.sinkHosts().stream().map(AiHostPersona::getHostName).toList()
        ));
        m.put("description", path.description());
        ResourceTransferPlan plan = getTransferPlan();
        m.put("transferPlan", Map.of(
                "triggers", plan.transferTriggers(),
                "channels", plan.transferChannels(),
                "expectedRate", plan.expectedTransferRate()
        ));
        m.put("hostCount", hostPersonaService != null ? hostPersonaService.listAll().size() : 0);
        return m;
    }

    @Override
    public boolean isAvailable() {
        return enabled && hostPersonaService != null;
    }
}
