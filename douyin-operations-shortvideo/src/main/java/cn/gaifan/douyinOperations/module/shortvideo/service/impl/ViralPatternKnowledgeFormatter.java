package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ViralPatternKnowledgeFormatter {

    private static final ObjectMapper JSON = new ObjectMapper();

    public PatternDocument build(SvViralVideo viral, String source, String breakdownContent) {
        PatternSignals signals = analyze(viral);
        String title = "爆款模式-" + (StringUtils.hasText(viral.getTitle()) ? viral.getTitle() : viral.getId());
        StringBuilder sb = new StringBuilder();
        sb.append("# 爆款模式结构化沉淀：").append(safe(viral.getTitle(), "无标题")).append("\n\n");
        sb.append("## 质量门禁\n");
        sb.append("- 质量分：").append(signals.qualityScore()).append("/100\n");
        sb.append("- 证据等级：").append(signals.evidenceLevel()).append("\n");
        sb.append("- 口播证据：").append(signals.hasTranscript() ? "有" : "缺失").append("\n");
        sb.append("- 画面证据：").append(signals.hasScenes() ? "有" : "缺失").append("\n");
        sb.append("- 互动样本：").append(signals.hasEngagement() ? "有" : "缺失").append("\n");
        sb.append("- 复用风险：").append(signals.reuseRisk()).append("\n");
        sb.append("- 合规风险：").append(signals.complianceRisk()).append("\n\n");
        sb.append("## 结构化标签\n");
        sb.append("- 行业/标签：").append(safe(firstText(viral.getIndustryTags(), viral.getHashtags(), viral.getTags()), "未标注")).append("\n");
        sb.append("- 内容类型：").append(signals.contentType()).append("\n");
        sb.append("- 最佳二创类型：").append(safe(signals.bestRemakeType(), "未识别")).append("\n");
        sb.append("- 数据量级：播放 ").append(num(viral.getViewCount()))
                .append(" / 点赞 ").append(num(viral.getLikeCount()))
                .append(" / 评论 ").append(num(viral.getCommentCount()))
                .append(" / 分享 ").append(num(viral.getShareCount()))
                .append(" / 收藏 ").append(num(viral.getFavoriteCount())).append("\n\n");
        sb.append("## 可复用爆款模式字段\n");
        sb.append("- 三秒钩子：从开场结构、标题、封面和口播中提炼，可用于同品类二创。\n");
        sb.append("- 脚本结构：问题/冲突 -> 证据/演示 -> 信任/对比 -> 行动。\n");
        sb.append("- 镜头节奏：标记产品特写、人物口播、证据覆盖、评论/字幕强化和 CTA。\n");
        sb.append("- 转化信号：保留评论痛点、互动诱因、购买理由和适合/不适合人群。\n");
        sb.append("- 合规边界：必须结合 douyin_weigui 审核，禁止复制违规表达。\n\n");
        sb.append("## 原始拆解材料\n");
        sb.append(StringUtils.hasText(breakdownContent) ? breakdownContent : "");

        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source", source);
        metadata.put("viralVideoId", String.valueOf(viral.getId()));
        metadata.put("douyinVideoId", safe(viral.getDouyinVideoId(), ""));
        metadata.put("patternType", signals.contentType());
        metadata.put("qualityScore", String.valueOf(signals.qualityScore()));
        metadata.put("evidenceLevel", signals.evidenceLevel());
        metadata.put("reuseRisk", signals.reuseRisk());
        metadata.put("complianceRisk", signals.complianceRisk());
        metadata.put("bestRemakeType", safe(signals.bestRemakeType(), ""));
        metadata.put("hasTranscript", String.valueOf(signals.hasTranscript()));
        metadata.put("hasScenes", String.valueOf(signals.hasScenes()));
        metadata.put("hasEngagement", String.valueOf(signals.hasEngagement()));
        return new PatternDocument(title, sb.toString(), metadata);
    }

    public PatternSignals analyze(SvViralVideo viral) {
        boolean hasTranscript = StringUtils.hasText(viral.getTranscript());
        boolean hasScenes = StringUtils.hasText(viral.getSceneDescriptions()) || StringUtils.hasText(viral.getKeyframeBosUrls());
        boolean hasEngagement = positive(viral.getViewCount()) || positive(viral.getLikeCount()) || positive(viral.getCommentCount());
        String evidenceLevel = resolveEvidenceLevel(viral, hasTranscript, hasScenes);
        String bestRemakeType = jsonText(viral.getDeepAnalysisResult(), "bestRemakeType", "best_remake_type");
        String complianceRisk = resolveComplianceRisk(viral);
        String contentType = resolveContentType(viral, bestRemakeType);
        String reuseRisk = resolveReuseRisk(evidenceLevel, hasTranscript, hasScenes);
        int quality = 20;
        if (hasTranscript) quality += 20;
        if (hasScenes) quality += 20;
        if (hasEngagement) quality += 15;
        if (StringUtils.hasText(viral.getDeepAnalysisResult())) quality += 15;
        if ("empirical".equals(evidenceLevel)) quality += 10;
        if ("high".equals(complianceRisk)) quality -= 20;
        if ("high".equals(reuseRisk)) quality -= 10;
        quality = Math.max(0, Math.min(100, quality));
        return new PatternSignals(quality, evidenceLevel, contentType, bestRemakeType, reuseRisk, complianceRisk,
                hasTranscript, hasScenes, hasEngagement);
    }

    private String resolveEvidenceLevel(SvViralVideo viral, boolean hasTranscript, boolean hasScenes) {
        String explicit = jsonText(viral.getDeepAnalysisResult(), "evidenceLevel", "evidence_level");
        if (StringUtils.hasText(explicit)) return explicit.trim();
        if (hasTranscript && hasScenes) return "empirical";
        if (hasTranscript || hasScenes) return "partial";
        return "inferred";
    }

    private String resolveComplianceRisk(SvViralVideo viral) {
        String text = (safe(viral.getDeepAnalysisResult(), "") + " " + safe(viral.getTranscript(), "") + " " + safe(viral.getTitle(), "")).toLowerCase();
        if (containsAny(text, "违规", "风险", "绝对化", "医疗", "功效", "最低价", "保底", "暴富")) return "high";
        if (containsAny(text, "合规注意", "注意事项", "承诺")) return "medium";
        return "low";
    }

    private String resolveContentType(SvViralVideo viral, String bestRemakeType) {
        String text = (safe(viral.getTitle(), "") + " " + safe(viral.getDescription(), "") + " " + safe(viral.getTranscript(), "")
                + " " + safe(viral.getHashtags(), "") + " " + safe(bestRemakeType, "")).toLowerCase();
        if (containsAny(text, "数字人", "口播", "带货", "产品", "测评", "开箱", "commerce", "product")) return "commerce_script";
        if (containsAny(text, "剧情", "反转", "故事")) return "story_drama";
        if (containsAny(text, "教程", "干货", "方法", "知识")) return "educational";
        return "general_viral";
    }

    private String resolveReuseRisk(String evidenceLevel, boolean hasTranscript, boolean hasScenes) {
        if ("inferred".equals(evidenceLevel) || (!hasTranscript && !hasScenes)) return "high";
        if (!hasTranscript || !hasScenes) return "medium";
        return "low";
    }

    private String jsonText(String json, String... keys) {
        if (!StringUtils.hasText(json)) return null;
        try {
            JsonNode root = JSON.readTree(json);
            for (String key : keys) {
                JsonNode node = root.path(key);
                if (!node.isMissingNode() && !node.isNull() && node.isValueNode()) {
                    String value = node.asText();
                    if (StringUtils.hasText(value)) return value;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static boolean containsAny(String text, String... words) {
        if (text == null) return false;
        for (String word : words) {
            if (word != null && text.contains(word.toLowerCase())) return true;
        }
        return false;
    }

    private static boolean positive(Long value) {
        return value != null && value > 0;
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) return value.trim();
        }
        return null;
    }

    private static String safe(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private static long num(Long value) {
        return value != null ? value : 0L;
    }

    public record PatternDocument(String title, String content, Map<String, String> metadata) {}

    public record PatternSignals(
            int qualityScore,
            String evidenceLevel,
            String contentType,
            String bestRemakeType,
            String reuseRisk,
            String complianceRisk,
            boolean hasTranscript,
            boolean hasScenes,
            boolean hasEngagement
    ) {}
}
