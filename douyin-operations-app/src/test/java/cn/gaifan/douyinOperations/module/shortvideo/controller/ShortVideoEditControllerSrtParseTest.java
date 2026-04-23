package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.module.ai.service.VideoEditService;
import cn.gaifan.douyinOperations.module.shortvideo.util.AutoComposeSubtitleResolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShortVideoEditControllerSrtParseTest {

    @Test
    void parseSrtDocument_basicCommaAndDot() {
        String srt = """
                1
                00:00:01,000 --> 00:00:04,000
                Hello

                2
                00:00:05.000 --> 00:00:07.500
                Line two
                """;
        List<VideoEditService.SubtitleItem> items = AutoComposeSubtitleResolver.parseSrtDocumentToItems(srt);
        assertEquals(2, items.size());
        assertEquals("Hello", items.get(0).text());
        assertEquals(1.0, items.get(0).startTime(), 0.05);
        assertEquals(4.0, items.get(0).endTime(), 0.05);
        assertEquals("Line two", items.get(1).text());
        assertEquals(5.0, items.get(1).startTime(), 0.05);
    }

    @Test
    void parseSrtDocument_skipsBadBlocks() {
        String srt = """
                no time line here

                1
                00:00:00,100 --> 00:00:01,200
                Ok
                """;
        List<VideoEditService.SubtitleItem> items = AutoComposeSubtitleResolver.parseSrtDocumentToItems(srt);
        assertEquals(1, items.size());
        assertEquals("Ok", items.get(0).text());
    }

    @Test
    void parseSrtDocument_emptyInput() {
        assertTrue(AutoComposeSubtitleResolver.parseSrtDocumentToItems(null).isEmpty());
        assertTrue(AutoComposeSubtitleResolver.parseSrtDocumentToItems("   ").isEmpty());
    }
}
