package cn.gaifan.douyinOperations.contract.ai;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VideoAnalysisPortTest {

    @Test
    void videoAnalysisResultIsRecord() {
        var r = new VideoAnalysisPort.VideoAnalysisResult(
                "http://test", "结构", "模式", 0.85,
                java.util.List.of("亮点1", "亮点2"));
        assertEquals(0.85, r.viralScore(), 0.01);
        assertEquals(2, r.highlights().size());
    }

    @Test
    void highScoreIndicatesViral() {
        var r = new VideoAnalysisPort.VideoAnalysisResult(
                "url", "s", "p", 0.92, java.util.List.of());
        assertTrue(r.viralScore() > 0.9);
    }

    @Test
    void emptyHighlightsAllowed() {
        var r = new VideoAnalysisPort.VideoAnalysisResult(
                "url", "s", "p", 0.5, java.util.List.of());
        assertTrue(r.highlights().isEmpty());
    }
}
