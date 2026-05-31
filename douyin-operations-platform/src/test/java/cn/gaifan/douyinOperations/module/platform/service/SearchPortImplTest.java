package cn.gaifan.douyinOperations.module.platform.service;

import cn.gaifan.douyinOperations.contract.port.SearchPort;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SearchPortImplTest {

    private final SearchPort search = new SearchPortImpl();

    @Test
    void globalSearchReturnsResults() {
        var results = search.globalSearch("测试", 5);
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).score() > 0.8);
    }

    @Test
    void searchByModule() {
        var results = search.searchByModule("抖音", "douyin", 10);
        assertEquals(1, results.size());
        assertEquals("douyin", results.get(0).type());
    }

    @Test
    void emptyQueryStillWorks() {
        var results = search.globalSearch("", 3);
        assertNotNull(results);
    }
}
