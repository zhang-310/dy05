package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 将 SvViralVideo 深度分析结果格式化为面向直播话术策划的结构化知识库文档。
 */
@Slf4j
@Component
public class VideoBreakdownKbFormatter {

    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * 格式化为 Markdown 知识库文档
     */
    public String format(SvViralVideo viral) {
        StringBuilder sb = new StringBuilder();
        ViralEvidenceHelper.EvidenceSnapshot evidence = ViralEvidenceHelper.resolveEvidence(
                viral.getDeepAnalysisResult(),
                viral.getTranscript(),
                viral.getSceneDescriptions()
        );

        sb.append("# 视频拆解：").append(safeStr(viral.getTitle(), "无标题")).append("\n\n");

        sb.append("## 基本信息\n");
        if (StringUtils.hasText(viral.getAuthorName())) {
            sb.append("- 作者：").append(viral.getAuthorName()).append("\n");
        }
        if (viral.getPublishTime() != null) {
            sb.append("- 发布时间：").append(viral.getPublishTime()).append("\n");
        }
        if (viral.getVideoDuration() != null) {
            sb.append("- 时长：").append(viral.getVideoDuration()).append("秒\n");
        }
        sb.append("- 播放量：").append(num(viral.getViewCount()));
        sb.append(" | 点赞：").append(num(viral.getLikeCount()));
        sb.append(" | 评论：").append(num(viral.getCommentCount()));
        sb.append(" | 分享：").append(num(viral.getShareCount()));
        sb.append(" | 收藏：").append(num(viral.getFavoriteCount())).append("\n");
        if (StringUtils.hasText(viral.getCoverBosUrl())) {
            sb.append("- 封面地址：").append(viral.getCoverBosUrl()).append("\n");
        }
        if (StringUtils.hasText(viral.getVideoBosUrl())) {
            sb.append("- 视频地址：").append(viral.getVideoBosUrl()).append("\n");
        }
        if (StringUtils.hasText(viral.getDescription())) {
            sb.append("- 视频描述：").append(viral.getDescription()).append("\n");
        }
        if (StringUtils.hasText(viral.getHashtags())) {
            sb.append("- 话题标签：").append(viral.getHashtags()).append("\n");
        }
        sb.append("\n");

        appendEvidenceSection(sb, evidence);

        if (StringUtils.hasText(viral.getTranscript())) {
            sb.append(transcriptHeading(evidence)).append("\n");
            sb.append(viral.getTranscript()).append("\n\n");
        }

        if (StringUtils.hasText(viral.getSceneDescriptions())) {
            sb.append(sceneHeading(evidence)).append("\n");
            sb.append(viral.getSceneDescriptions()).append("\n\n");
        }

        appendDeepAnalysisSection(sb, viral.getDeepAnalysisResult(), viral.getTranscript(), evidence);

        if (StringUtils.hasText(viral.getAnalysisResult())) {
            sb.append("## AI 分析摘要\n");
            sb.append(viral.getAnalysisResult()).append("\n\n");
        }

        return sb.toString();
    }

    /**
     * 生成知识库文档标题
     */
    public String formatTitle(SvViralVideo viral) {
        String author = StringUtils.hasText(viral.getAuthorName()) ? viral.getAuthorName() : "未知";
        String title = StringUtils.hasText(viral.getTitle()) ? viral.getTitle() : "无标题视频";
        if (title.length() > 60) title = title.substring(0, 60) + "...";
        return "视频拆解-" + author + "-" + title;
    }

    private void appendDeepAnalysisSection(StringBuilder sb, String deepAnalysisResult,
                                           String persistedTranscript, ViralEvidenceHelper.EvidenceSnapshot evidence) {
        if (!StringUtils.hasText(deepAnalysisResult)) return;

        try {
            JsonNode root = JSON.readTree(deepAnalysisResult);

            appendJsonField(sb, root, "opening", "## 开场结构分析");
            appendJsonField(sb, root, "climax", "## 高潮结构分析");
            appendJsonField(sb, root, "ending", "## 结尾结构分析");
            appendJsonFieldWithFallback(sb, root, "emotionCurve", "emotion_curve", "## 情绪曲线");
            appendJsonFieldWithFallback(sb, root, "viralElements", "viral_elements", "## 爆款元素");
            appendJsonField(sb, root, "copywriting", "## 文案技巧");
            appendJsonField(sb, root, "transitions", "## 转场设计");
            appendJsonField(sb, root, "bgm", "## 音乐/BGM 分析");
            appendJsonFieldWithFallback(sb, root, "remakeAdvice", "remake_advice", "## 二创建议");

            JsonNode transcript = root.path("transcript");
            if (!transcript.isMissingNode() && transcript.has("fullText")) {
                String fullText = transcript.path("fullText").asText("");
                if (StringUtils.hasText(fullText) && !sameContent(fullText, persistedTranscript)) {
                    sb.append(structuredTranscriptHeading(evidence)).append("\n");
                    sb.append(fullText).append("\n\n");
                }
            }

            JsonNode structure = root.path("structure");
            if (!structure.isMissingNode() && structure.isObject()) {
                sb.append("## 内容结构\n");
                sb.append(structure.toPrettyString()).append("\n\n");
            }

            JsonNode viralHypotheses = root.path("viralHypotheses");
            if (viralHypotheses.isMissingNode()) viralHypotheses = root.path("viral_hypotheses");
            if (!viralHypotheses.isMissingNode()) {
                sb.append("## 爆款假设\n");
                sb.append(nodeToText(viralHypotheses)).append("\n\n");
            }

            JsonNode remakeVars = root.path("remakeVariableTable");
            if (remakeVars.isMissingNode()) remakeVars = root.path("remake_variable_table");
            if (!remakeVars.isMissingNode()) {
                sb.append("## 二创变量表\n");
                sb.append(nodeToText(remakeVars)).append("\n\n");
            }
        } catch (Exception e) {
            sb.append("## 深度分析结果\n");
            sb.append(deepAnalysisResult).append("\n\n");
        }
    }

    private void appendEvidenceSection(StringBuilder sb, ViralEvidenceHelper.EvidenceSnapshot evidence) {
        if (!evidence.hasDisclosure()) {
            return;
        }
        sb.append("## 证据口径\n");
        appendEvidenceLine(sb, "总体证据", ViralEvidenceHelper.evidenceDisplayLabel(evidence.overallLevel()));
        appendEvidenceLine(sb, "口播来源", describeTranscriptEvidence(evidence));
        appendEvidenceLine(sb, "场景来源", describeSceneEvidence(evidence));
        appendEvidenceLine(sb, "评论样本", describeCommentEvidence(evidence));
        if (evidence.hasInferenceRisk()) {
            sb.append("- 使用说明：带“推演”标签的口播/场景来自标题、封面、互动与结构线索，不应表述为真实 ASR 或真实抽帧。\n");
        }
        sb.append("\n");
    }

    private void appendEvidenceLine(StringBuilder sb, String label, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        sb.append("- ").append(label).append("：").append(value).append("\n");
    }

    private String transcriptHeading(ViralEvidenceHelper.EvidenceSnapshot evidence) {
        return switch (ViralEvidenceHelper.normalizeEvidenceLevel(evidence.transcriptLevel())) {
            case "empirical" -> "## 实证口播文案";
            case "inferred" -> "## 推演口播文案（非 ASR 实录）";
            default -> "## 完整话术/文案";
        };
    }

    private String structuredTranscriptHeading(ViralEvidenceHelper.EvidenceSnapshot evidence) {
        return switch (ViralEvidenceHelper.normalizeEvidenceLevel(evidence.transcriptLevel())) {
            case "empirical" -> "## 结构化口播文案（实证整理）";
            case "inferred" -> "## 结构化口播文案（推演提炼，非 ASR 实录）";
            default -> "## 结构化口播文案";
        };
    }

    private String sceneHeading(ViralEvidenceHelper.EvidenceSnapshot evidence) {
        return switch (ViralEvidenceHelper.normalizeEvidenceLevel(evidence.sceneLevel())) {
            case "empirical" -> "## 实证场景拆解";
            case "inferred" -> "## 推演场景拆解（非真实抽帧）";
            default -> "## 场景拆解";
        };
    }

    private String describeTranscriptEvidence(ViralEvidenceHelper.EvidenceSnapshot evidence) {
        return switch (ViralEvidenceHelper.normalizeEvidenceLevel(evidence.transcriptLevel())) {
            case "empirical" -> "实证（可视作真实转写/原视频口播）";
            case "inferred" -> "推演（基于标题、封面、互动等线索推演，不能视为真实 ASR）";
            case "missing" -> "缺失";
            default -> null;
        };
    }

    private String describeSceneEvidence(ViralEvidenceHelper.EvidenceSnapshot evidence) {
        return switch (ViralEvidenceHelper.normalizeEvidenceLevel(evidence.sceneLevel())) {
            case "empirical" -> "实证（可视作真实场景/画面拆解）";
            case "inferred" -> "推演（基于封面、结构和上下文推演，不能视为真实抽帧）";
            case "missing" -> "缺失";
            default -> null;
        };
    }

    private String describeCommentEvidence(ViralEvidenceHelper.EvidenceSnapshot evidence) {
        String level = ViralEvidenceHelper.normalizeEvidenceLevel(evidence.commentLevel());
        String sampleNote = Boolean.TRUE.equals(evidence.hasCommentSamples()) ? "，含评论样本"
                : Boolean.FALSE.equals(evidence.hasCommentSamples()) ? "，无评论样本" : "";
        return switch (level) {
            case "empirical" -> "实证" + sampleNote;
            case "inferred" -> "推演" + sampleNote;
            case "missing" -> "缺失" + sampleNote;
            default -> sampleNote.isEmpty() ? null : "未标注" + sampleNote;
        };
    }

    private boolean sameContent(String left, String right) {
        return normalizeComparableText(left).equals(normalizeComparableText(right));
    }

    private String normalizeComparableText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text
                .replace("【推演口播】", "")
                .replace("【推演场景】", "")
                .replaceAll("\\s+", "")
                .trim();
    }

    private void appendJsonFieldWithFallback(StringBuilder sb, JsonNode root, String camelKey, String snakeKey, String heading) {
        JsonNode node = root.path(camelKey);
        if (node.isMissingNode() || node.isNull()) {
            node = root.path(snakeKey);
        }
        if (node.isMissingNode() || node.isNull()) return;
        sb.append(heading).append("\n");
        sb.append(nodeToText(node)).append("\n\n");
    }

    private void appendJsonField(StringBuilder sb, JsonNode root, String key, String heading) {
        JsonNode node = root.path(key);
        if (node.isMissingNode() || node.isNull()) return;
        sb.append(heading).append("\n");
        sb.append(nodeToText(node)).append("\n\n");
    }

    private String nodeToText(JsonNode node) {
        if (node.isTextual()) return node.asText();
        if (node.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode item : node) {
                sb.append("- ").append(item.isTextual() ? item.asText() : item.toString()).append("\n");
            }
            return sb.toString().trim();
        }
        return node.toPrettyString();
    }

    private static String safeStr(String s, String fallback) {
        return StringUtils.hasText(s) ? s : fallback;
    }

    private static String num(Long v) {
        return v != null ? String.valueOf(v) : "0";
    }
}
