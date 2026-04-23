package cn.gaifan.douyinOperations.module.ai.service.impl.brain;

import cn.gaifan.douyinOperations.module.ai.service.brain.RiskWarningService;
import cn.gaifan.douyinOperations.module.ai.util.ContentSecurityScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cn.gaifan.douyinOperations.module.script.service.ComplianceWordService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 风险预警服务实现（Phase2）
 * 复用 ContentSecurityScanner、ComplianceWordService
 */
@Service
public class RiskWarningServiceImpl implements RiskWarningService {

    private static final Logger log = LoggerFactory.getLogger(RiskWarningServiceImpl.class);

    @Value("${app.ai.brain.risk-warning.enabled:true}")
    private boolean enabled;

    @Autowired(required = false)
    private ComplianceWordService complianceWordService;

    private final AtomicLong totalChecks = new AtomicLong(0);
    private final AtomicLong violationCount = new AtomicLong(0);

    @Override
    public List<RiskItem> warn(String content, Long userId) {
        if (!enabled || content == null) return List.of();
        totalChecks.incrementAndGet();
        List<RiskItem> items = new ArrayList<>();
        var warnings = ContentSecurityScanner.scan(content, "risk-check");
        for (var w : warnings) {
            int level = "privacy".equals(w.type()) ? 3 : ("compliance".equals(w.type()) ? 2 : 1);
            if (level >= 2) violationCount.incrementAndGet();
            items.add(new RiskItem(level, w.type(), w.message(), 0, 0, w.suggestion()));
        }
        if (complianceWordService != null && complianceWordService.isLoadedFromDb()) {
            for (String v : complianceWordService.getMedicalViolations()) {
                if (v != null && !v.isBlank() && content.contains(v)) {
                    items.add(new RiskItem(3, "medical", "医疗违禁: " + v, 0, 0, "建议移除或替换"));
                    violationCount.incrementAndGet();
                }
            }
            Map<String, String> abs = complianceWordService.getAbsoluteReplacements();
            if (abs != null) {
                for (Map.Entry<String, String> e : abs.entrySet()) {
                    if (e.getKey() != null && !e.getKey().isBlank() && content.contains(e.getKey())) {
                        items.add(new RiskItem(2, "compliance", "绝对化用语: " + e.getKey(), 0, 0,
                                "建议替换为: " + (e.getValue() != null ? e.getValue() : "合规表述")));
                        violationCount.incrementAndGet();
                    }
                }
            }
        }
        return items;
    }

    @Override
    public List<RiskItem> warnBatch(List<String> contents, Long userId) {
        if (!enabled || contents == null) return List.of();
        List<RiskItem> all = new ArrayList<>();
        for (String c : contents) {
            all.addAll(warn(c, userId));
        }
        return all;
    }

    @Override
    public RiskStats getStats() {
        long total = totalChecks.get();
        long violations = violationCount.get();
        double acc = total > 0 ? 1.0 - (double) violations / total : 1.0;
        return new RiskStats(total, violations, acc);
    }

    @Override
    public boolean isAvailable() {
        return enabled;
    }
}
