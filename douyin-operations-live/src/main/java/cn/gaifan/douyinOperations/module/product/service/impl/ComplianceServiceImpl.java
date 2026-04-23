package cn.gaifan.douyinOperations.module.product.service.impl;

import cn.gaifan.douyinOperations.module.product.service.ComplianceService;
import cn.gaifan.douyinOperations.module.script.service.ComplianceWordService;
import cn.gaifan.douyinOperations.module.script.service.ViolationWordService;
import cn.gaifan.douyinOperations.module.script.vo.ViolationCheckResultVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 产品话术合规检测：绝对化用语、医疗功效 + 违规词库
 * 词库优先从 DB 加载（ComplianceWordService），无配置时 fallback 到硬编码默认值
 */
@Service
public class ComplianceServiceImpl implements ComplianceService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceServiceImpl.class);

    /** 默认绝对化用语（fallback） */
    private static final Map<String, String> DEFAULT_ABSOLUTE_REPLACEMENTS = new LinkedHashMap<>();
    /** 默认医疗功效词（fallback） */
    private static final List<String> DEFAULT_MEDICAL_VIOLATIONS = List.of(
            "治疗", "根治", "药效", "疗效", "治愈", "药用", "处方"
    );

    static {
        DEFAULT_ABSOLUTE_REPLACEMENTS.put("最好", "优质");
        DEFAULT_ABSOLUTE_REPLACEMENTS.put("第一", "领先");
        DEFAULT_ABSOLUTE_REPLACEMENTS.put("唯一", "优选");
        DEFAULT_ABSOLUTE_REPLACEMENTS.put("国家级", "高品质");
        DEFAULT_ABSOLUTE_REPLACEMENTS.put("最高级", "高级");
        DEFAULT_ABSOLUTE_REPLACEMENTS.put("顶级", "高端");
        DEFAULT_ABSOLUTE_REPLACEMENTS.put("极致", "出色");
        DEFAULT_ABSOLUTE_REPLACEMENTS.put("100%", "高");
        DEFAULT_ABSOLUTE_REPLACEMENTS.put("百分百", "高");
    }

    @Autowired(required = false)
    private ComplianceWordService complianceWordService;

    @Autowired(required = false)
    private ViolationWordService violationWordService;

    @Override
    public ComplianceResult checkAndFix(String text) {
        if (text == null || text.isBlank()) {
            return ComplianceResult.pass("");
        }

        Map<String, String> absoluteReplacements = resolveAbsoluteReplacements();
        List<String> medicalViolations = resolveMedicalViolations();

        List<String> violations = new ArrayList<>();
        String current = text;

        // 1. 绝对化用语检测与自动修复
        for (Map.Entry<String, String> e : absoluteReplacements.entrySet()) {
            if (current.contains(e.getKey())) {
                violations.add("绝对化用语: " + e.getKey());
                current = current.replace(e.getKey(), e.getValue());
            }
        }

        // 2. 医疗功效词检测（不可自动修复）
        for (String v : medicalViolations) {
            if (current.contains(v)) {
                violations.add("医疗功效宣称: " + v);
            }
        }

        // 3. 违规词库检测（可选）
        if (violationWordService != null) {
            try {
                ViolationCheckResultVO vo = violationWordService.check(current, "live", null);
                if (vo != null && vo.isHasViolation() && vo.getViolations() != null) {
                    for (var hit : vo.getViolations()) {
                        violations.add("违规词: " + (hit.getWord() != null ? hit.getWord() : ""));
                    }
                }
            } catch (Exception e) {
                log.warn("违规词库检测异常: {}", e.getMessage());
            }
        }

        if (violations.isEmpty()) {
            return ComplianceResult.pass(current);
        }
        // 若存在医疗功效或违规词库命中，不通过；仅绝对化用语且已修复的可通过
        boolean hasUnfixable = violations.stream()
                .anyMatch(s -> s.startsWith("医疗功效") || s.startsWith("违规词"));
        if (hasUnfixable) {
            return ComplianceResult.fail(text, violations);
        }
        return new ComplianceResult(true, current, violations);
    }

    private Map<String, String> resolveAbsoluteReplacements() {
        if (complianceWordService != null && complianceWordService.isLoadedFromDb()) {
            Map<String, String> fromDb = complianceWordService.getAbsoluteReplacements();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        return DEFAULT_ABSOLUTE_REPLACEMENTS;
    }

    private List<String> resolveMedicalViolations() {
        if (complianceWordService != null && complianceWordService.isLoadedFromDb()) {
            List<String> fromDb = complianceWordService.getMedicalViolations();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        return DEFAULT_MEDICAL_VIOLATIONS;
    }
}
