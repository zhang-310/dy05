package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

/**
 * 统一解析爆款深度分析中的 evidenceLevel / evidenceDetails，避免下游各自猜测 transcript / scene 是否为实证。
 */
final class ViralEvidenceHelper {

    private static final ObjectMapper JSON = new ObjectMapper();

    private ViralEvidenceHelper() {
    }

    static EvidenceSnapshot resolveEvidence(String deepAnalysisResult, String transcript, String sceneDescriptions) {
        String overallLevel = null;
        String transcriptLevel = inferEvidenceFromPersistedText(transcript, "【推演口播】");
        String sceneLevel = inferEvidenceFromPersistedText(sceneDescriptions, "【推演场景】");
        String commentLevel = null;
        Boolean hasCommentSamples = null;

        if (StringUtils.hasText(deepAnalysisResult)) {
            try {
                JsonNode root = JSON.readTree(deepAnalysisResult);
                overallLevel = firstNonBlank(
                        trimToNull(root.path("evidenceLevel").asText(null)),
                        trimToNull(root.path("evidence_level").asText(null))
                );

                JsonNode evidenceDetails = root.path("evidenceDetails");
                if (!evidenceDetails.isMissingNode() && !evidenceDetails.isNull()) {
                    overallLevel = firstNonBlank(trimToNull(evidenceDetails.path("overallLevel").asText(null)), overallLevel);
                    transcriptLevel = firstNonBlank(trimToNull(evidenceDetails.path("transcriptLevel").asText(null)), transcriptLevel);
                    sceneLevel = firstNonBlank(trimToNull(evidenceDetails.path("sceneLevel").asText(null)), sceneLevel);
                    commentLevel = firstNonBlank(trimToNull(evidenceDetails.path("commentLevel").asText(null)), commentLevel);
                    if (!evidenceDetails.path("hasCommentSamples").isMissingNode() && !evidenceDetails.path("hasCommentSamples").isNull()) {
                        hasCommentSamples = evidenceDetails.path("hasCommentSamples").asBoolean();
                    }
                }
            } catch (Exception ignored) {
                // ignore and fall back to persisted markers
            }
        }

        return new EvidenceSnapshot(
                normalizeEvidenceLevel(overallLevel),
                normalizeEvidenceLevel(transcriptLevel),
                normalizeEvidenceLevel(sceneLevel),
                normalizeEvidenceLevel(commentLevel),
                hasCommentSamples
        );
    }

    static String normalizeEvidenceLevel(String level) {
        String normalized = trimToNull(level);
        if (normalized == null) {
            return "";
        }
        return switch (normalized.toLowerCase()) {
            case "empirical" -> "empirical";
            case "inferred", "partial" -> "inferred";
            case "missing" -> "missing";
            default -> normalized.toLowerCase();
        };
    }

    static String evidenceDisplayLabel(String level) {
        return switch (normalizeEvidenceLevel(level)) {
            case "empirical" -> "实证";
            case "inferred" -> "推演";
            case "missing" -> "缺失";
            default -> "";
        };
    }

    private static String inferEvidenceFromPersistedText(String value, String inferredPrefix) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.startsWith(inferredPrefix) ? "inferred" : null;
    }

    private static String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    record EvidenceSnapshot(String overallLevel, String transcriptLevel, String sceneLevel,
                            String commentLevel, Boolean hasCommentSamples) {

        boolean hasDisclosure() {
            return StringUtils.hasText(overallLevel)
                    || StringUtils.hasText(transcriptLevel)
                    || StringUtils.hasText(sceneLevel)
                    || StringUtils.hasText(commentLevel)
                    || hasCommentSamples != null;
        }

        boolean hasInferenceRisk() {
            return "inferred".equalsIgnoreCase(transcriptLevel)
                    || "inferred".equalsIgnoreCase(sceneLevel);
        }
    }
}
