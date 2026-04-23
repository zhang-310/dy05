package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VideoBreakdownKbFormatter 测试")
class VideoBreakdownKbFormatterTest {

    private final VideoBreakdownKbFormatter formatter = new VideoBreakdownKbFormatter();

    @Test
    @DisplayName("format 应明确标注推演口播与推演场景的证据口径")
    void format_shouldLabelInferredTranscriptAndScenes() {
        SvViralVideo viral = new SvViralVideo();
        viral.setTitle("护肤爆款");
        viral.setTranscript("【推演口播】先抛问题，再给解决方案");
        viral.setSceneDescriptions("【推演场景】\n[0-3s] 浴室镜前 | 人物:博主 道具:精华 镜头:特写 情绪:紧张");
        viral.setDeepAnalysisResult("""
                {
                  "evidenceLevel": "inferred",
                  "evidenceDetails": {
                    "overallLevel": "inferred",
                    "transcriptLevel": "inferred",
                    "sceneLevel": "inferred",
                    "commentLevel": "missing",
                    "hasCommentSamples": false
                  },
                  "transcript": {
                    "fullText": "先抛问题，再给解决方案"
                  }
                }
                """);

        String markdown = formatter.format(viral);

        assertThat(markdown).contains("## 证据口径");
        assertThat(markdown).contains("- 总体证据：推演");
        assertThat(markdown).contains("推演（基于标题、封面、互动等线索推演，不能视为真实 ASR）");
        assertThat(markdown).contains("## 推演口播文案（非 ASR 实录）");
        assertThat(markdown).contains("## 推演场景拆解（非真实抽帧）");
        assertThat(markdown).contains("使用说明：带“推演”标签的口播/场景");
    }

    @Test
    @DisplayName("format 在缺少 evidenceDetails 时不应把普通 transcript 误标为实证")
    void format_shouldAvoidClaimingEmpiricalWithoutEvidence() {
        SvViralVideo viral = new SvViralVideo();
        viral.setTitle("普通爆款");
        viral.setTranscript("这是抓取到的一段口播");
        viral.setSceneDescriptions("这是一段场景摘要");

        String markdown = formatter.format(viral);

        assertThat(markdown).contains("## 完整话术/文案");
        assertThat(markdown).contains("## 场景拆解");
        assertThat(markdown).doesNotContain("## 实证口播文案");
        assertThat(markdown).doesNotContain("## 实证场景拆解");
        assertThat(markdown).doesNotContain("## 证据口径");
    }
}
