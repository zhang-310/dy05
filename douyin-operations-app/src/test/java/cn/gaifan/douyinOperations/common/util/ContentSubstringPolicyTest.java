package cn.gaifan.douyinOperations.common.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentSubstringPolicyTest {

    @Test
    void anyMatch_isCaseInsensitive() {
        assertTrue(ContentSubstringPolicy.anyMatch("今日军事快讯", List.of("军事")));
        assertTrue(ContentSubstringPolicy.anyMatch("MILITARY", List.of("military")));
    }

    @Test
    void anyMatch_skipsBlankNeedles() {
        assertFalse(ContentSubstringPolicy.anyMatch("abc", Arrays.asList("", "  ", null)));
    }

    @Test
    void firstHit_returnsFirstConfiguredMatch() {
        assertEquals("军事", ContentSubstringPolicy.firstHit("军事与外交", List.of("军事", "外交")));
    }

    @Test
    void firstHit_nullWhenNoMatch() {
        assertNull(ContentSubstringPolicy.firstHit("儿童护肤品专场", List.of("儿童直播")));
    }
}
