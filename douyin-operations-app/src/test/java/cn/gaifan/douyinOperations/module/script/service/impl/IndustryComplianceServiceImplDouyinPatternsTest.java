package cn.gaifan.douyinOperations.module.script.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link IndustryComplianceServiceImpl} 中抖音公开摘要正则（DOUYIN_PUBLIC_RULE_PATTERNS）回归用例。
 */
class IndustryComplianceServiceImplDouyinPatternsTest {

    private final IndustryComplianceServiceImpl svc = new IndustryComplianceServiceImpl();

    @Test
    void douyinPublic_hitsCounterfeitAndOffPlatform() {
        assertTrue(hasDouyinPublic(svc.checkCompliance("这款是原单正品", "cosmetics")));
        assertTrue(hasDouyinPublic(svc.checkCompliance("私下交易更便宜", "cosmetics")));
    }

    @Test
    void douyinPublic_hitsPriceAndRumor() {
        assertTrue(hasDouyinPublic(svc.checkCompliance("虚构原价199现在99", "cosmetics")));
        assertTrue(hasDouyinPublic(svc.checkCompliance("恶意造谣竞品", "cosmetics")));
    }

    @Test
    void industryRule_firstNameDoesNotFalsePositive() {
        assertTrue(svc.checkCompliance("我们第一名夺冠了", "cosmetics").stream()
                .noneMatch(m -> "第一".equals(m.get("matchedText"))));
        assertTrue(svc.checkCompliance("全网最低价在这里", "cosmetics").stream()
                .anyMatch(m -> "industry".equals(m.get("source"))));
    }

    private static boolean hasDouyinPublic(java.util.List<java.util.Map<String, Object>> rows) {
        return rows.stream().anyMatch(m -> "douyin_public_summary".equals(m.get("source")));
    }
}
