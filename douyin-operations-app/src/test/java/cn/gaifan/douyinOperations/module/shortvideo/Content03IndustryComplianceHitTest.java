package cn.gaifan.douyinOperations.module.shortvideo;

import cn.gaifan.douyinOperations.module.script.service.IndustryComplianceService;
import cn.gaifan.douyinOperations.module.script.service.impl.IndustryComplianceServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * CONTENT-03：行业 / 公开规则合规命中（无 Spring / DB，避免 Flyway 锁争用）
 */
class Content03IndustryComplianceHitTest {

    private final IndustryComplianceService industryComplianceService = new IndustryComplianceServiceImpl();

    @Test
    void cosmetics_absolute_claim_triggers_violation() {
        assertFalse(industryComplianceService.checkCompliance("本品速效美白一天见效", "cosmetics").isEmpty());
    }
}
