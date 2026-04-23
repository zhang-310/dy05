package cn.gaifan.douyinOperations.module.shortvideo.util;

import cn.gaifan.douyinOperations.module.ai.service.VideoEditService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V-3：字幕解析优先级 subtitles &gt; srt &gt; srtUrl（与 auto-compose / 工作流 compose 同源）。
 */
class AutoComposeSubtitleResolverTest {

    @Test
    void resolveSubtitleItems_subtitlesWinOverSrt() {
        List<Map<String, Object>> subs = List.of(
                Map.of("startTime", 0.0, "endTime", 1.0, "text", "from subs")
        );
        Map<String, Object> materials = Map.of(
                "burnSubtitles", true,
                "subtitles", subs,
                "srt", "1\n00:00:00,000 --> 00:00:01,000\nfrom srt\n"
        );
        List<VideoEditService.SubtitleItem> items = AutoComposeSubtitleResolver.resolveSubtitleItems(materials);
        assertEquals(1, items.size());
        assertEquals("from subs", items.get(0).text());
    }

    @Test
    void resolveSubtitleItems_srtWhenNoSubtitlesList() {
        Map<String, Object> materials = Map.of(
                "burnSubtitles", true,
                "srt", "1\n00:00:00,000 --> 00:00:01,000\nhello\n"
        );
        List<VideoEditService.SubtitleItem> items = AutoComposeSubtitleResolver.resolveSubtitleItems(materials);
        assertEquals(1, items.size());
        assertEquals("hello", items.get(0).text());
    }

    @Test
    void resolveSubtitleItems_burnFalseReturnsEmpty() {
        Map<String, Object> materials = Map.of(
                "burnSubtitles", false,
                "srt", "1\n00:00:00,000 --> 00:00:01,000\nx\n"
        );
        assertTrue(AutoComposeSubtitleResolver.resolveSubtitleItems(materials).isEmpty());
    }
}
