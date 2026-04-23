package cn.gaifan.douyinOperations.module.shortvideo.util;

import cn.gaifan.douyinOperations.module.ai.service.VideoEditService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProjectTimelineJsonCodecTest {

    @Test
    void extractSubtitleSegments_readsInOutSec() {
        String json = """
                {"version":2,"subtitleTrack":{"segments":[
                  {"inSec":0,"outSec":1.5,"text":"你好"},
                  {"startTime":2,"endTime":3,"text":"世界"}
                ]}}""";
        List<VideoEditService.SubtitleItem> items = ProjectTimelineJsonCodec.extractSubtitleSegments(json);
        assertEquals(2, items.size());
        assertEquals("你好", items.get(0).text());
        assertEquals(0.0, items.get(0).startTime(), 0.01);
        assertEquals(1.5, items.get(0).endTime(), 0.01);
        assertEquals("世界", items.get(1).text());
    }

    @Test
    void reorderAlignedList_permutes() {
        List<String> a = List.of("a", "b", "c");
        String json = """
                {"version":2,"videoTracks":[{"clipIndicesOrdered":[2,0,1]}]}""";
        assertEquals(List.of("c", "a", "b"), ProjectTimelineJsonCodec.reorderAlignedListIfTimelineSaysSo(a, json));
    }

    @Test
    void parseClipIndicesPermutation_invalidReturnsNull() {
        String bad = """
                {"version":2,"videoTracks":[{"clipIndicesOrdered":[0,0]}]}""";
        assertNull(ProjectTimelineJsonCodec.parseClipIndicesPermutation(bad, 2));
    }
}
