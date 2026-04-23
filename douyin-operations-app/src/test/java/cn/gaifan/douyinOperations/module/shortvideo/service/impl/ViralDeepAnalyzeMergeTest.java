package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViralDeepAnalyzeMergeTest {

    @Test
    void parseRound1VisionBySceneIndex_mapsLinesToIndices() {
        String r1 = """
                [场景1 0.0s-3.0s] 室内明亮
                [场景2 3.0s-8.0s] 人物特写
                """;
        List<String> v = ViralDeepAnalyzeExecutor.parseRound1VisionBySceneIndex(r1, 2);
        assertEquals(2, v.size());
        assertEquals("室内明亮", v.get(0));
        assertEquals("人物特写", v.get(1));
    }

    @Test
    void parseRound1VisionBySceneIndex_skipsOutOfRange() {
        String r1 = "[场景1 0.0s-1.0s] only";
        List<String> v = ViralDeepAnalyzeExecutor.parseRound1VisionBySceneIndex(r1, 3);
        assertEquals(3, v.size());
        assertEquals("only", v.get(0));
        assertTrue(v.get(1).isEmpty());
    }

    @Test
    void partialEvidenceLevel_degradesToInferredWhenRoundsMissing() throws Exception {
        Method m = ViralDeepAnalyzeExecutor.class.getDeclaredMethod(
                "partialEvidenceLevel", String.class, String.class, String.class, String.class);
        m.setAccessible(true);
        String level = (String) m.invoke(null, "empirical", "{\"ok\":1}", "", "{\"ok\":1}");
        assertEquals("inferred", level);
    }

    @Test
    void resolveEvidenceLevel_marksCoverOnlyFallbackAsInferred() throws Exception {
        Method m = ViralDeepAnalyzeExecutor.class.getDeclaredMethod(
                "resolveEvidenceLevel", String.class, String.class, boolean.class);
        m.setAccessible(true);
        String level = (String) m.invoke(null, "真实转写文本", "封面分析: 室内护肤展示", true);
        assertEquals("inferred", level);
    }

    @Test
    void buildEvidenceDetails_distinguishesTranscriptSceneAndComments() throws Exception {
        Method m = ViralDeepAnalyzeExecutor.class.getDeclaredMethod(
                "buildEvidenceDetails", String.class, String.class, String.class, String.class);
        m.setAccessible(true);
        Object detailsObj = m.invoke(null,
                "（ASR 未启用，无真实口播转写。请基于标题与互动数据推演完整口播稿，勿伪称来自 ASR。）",
                "封面分析: 室内护肤展示",
                "- 评论1\n- 评论2",
                "inferred");
        @SuppressWarnings("unchecked")
        Map<String, Object> details = assertInstanceOf(Map.class, detailsObj);
        assertEquals("inferred", details.get("overallLevel"));
        assertEquals("inferred", details.get("transcriptLevel"));
        assertEquals("inferred", details.get("sceneLevel"));
        assertEquals("empirical", details.get("commentLevel"));
        assertEquals(true, details.get("hasCommentSamples"));
    }

    @Test
    void buildEvidenceDetails_marksSceneFallbackMarkersAsInferred() throws Exception {
        Method m = ViralDeepAnalyzeExecutor.class.getDeclaredMethod(
                "buildEvidenceDetails", String.class, String.class, String.class, String.class);
        m.setAccessible(true);
        Object detailsObj = m.invoke(null,
                "（无视频文件，仅有标题和元数据）",
                "[场景1 0.0s-3.0s] （视觉分析无响应）\n[场景2 3.0s-8.0s] （按 multi-round-max-vision-scenes 策略跳过单独识图，请结合口播与相邻场景理解）",
                "无",
                "inferred");
        @SuppressWarnings("unchecked")
        Map<String, Object> details = assertInstanceOf(Map.class, detailsObj);
        assertEquals("inferred", details.get("transcriptLevel"));
        assertEquals("inferred", details.get("sceneLevel"));
        assertEquals("missing", details.get("commentLevel"));
        assertEquals(false, details.get("hasCommentSamples"));
    }

    @Test
    void applyParsedFields_prefixesInferredTranscriptAndScenes() {
        ViralDeepAnalyzeExecutor executor = new ViralDeepAnalyzeExecutor(null, null, null);
        SvViralVideo viral = new SvViralVideo();
        executor.applyParsedFields(viral, """
                {
                  "transcript": {"fullText": "这是推演出的口播"},
                  "scenes": [{"time": "0-3s", "environment": "室内试用", "person": "", "props": "", "camera": "", "mood": ""}],
                  "evidenceDetails": {
                    "transcriptLevel": "inferred",
                    "sceneLevel": "inferred"
                  }
                }
                """);
        assertEquals("【推演口播】这是推演出的口播", viral.getTranscript());
        assertTrue(viral.getSceneDescriptions().startsWith("【推演场景】"));
    }
}
