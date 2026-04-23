package cn.gaifan.douyinOperations.module.script.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndustryComplianceMultiVerticalTest {

    private final IndustryComplianceServiceImpl svc = new IndustryComplianceServiceImpl();

    @Test
    void food_vertical_hits_disease_claim() {
        assertFalse(svc.checkCompliance("普通饼干抗癌食品天天吃", "food").isEmpty());
    }

    @Test
    void unknown_vertical_skips_cosmetics_medical_terms() {
        assertTrue(svc.checkCompliance("本品为医学级药妆速效", "nonexistent_industry").stream()
                .noneMatch(m -> "医学级".equals(m.get("matchedText")) || "药妆".equals(m.get("matchedText"))));
    }

    @Test
    void cosmetics_still_hits_medical_cosmetics() {
        assertFalse(svc.checkCompliance("医学级药妆", "cosmetics").isEmpty());
    }

    @Test
    void listSupportedIndustryCodes_includes_core_verticals() {
        assertTrue(svc.listSupportedIndustryCodes().contains("cosmetics"));
        assertTrue(svc.listSupportedIndustryCodes().contains("food"));
        assertTrue(svc.listSupportedIndustryCodes().contains("finance"));
    }
}
