package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.search.QueryIntent;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryIntentClassifierImplTest {

    @Test
    void classify_compareDefinitionHowTo() {
        QueryIntentClassifierImpl c = new QueryIntentClassifierImpl();
        ReflectionTestUtils.setField(c, "intentEnabled", true);
        assertEquals(QueryIntent.COMPARE, c.classify("A 和 B 的区别是什么"));
        assertEquals(QueryIntent.DEFINITION, c.classify("玻尿酸是什么"));
        assertEquals(QueryIntent.HOW_TO, c.classify("如何修复敏感肌"));
        assertEquals(QueryIntent.GENERAL, c.classify("护肤"));
    }

    @Test
    void classify_disabledReturnsGeneral() {
        QueryIntentClassifierImpl c = new QueryIntentClassifierImpl();
        ReflectionTestUtils.setField(c, "intentEnabled", false);
        assertEquals(QueryIntent.GENERAL, c.classify("如何修复敏感肌"));
    }
}
