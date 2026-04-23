package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDramaCharacter;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDramaEpisode;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvDramaCharacterRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvDramaEpisodeRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 短剧场景/连贯性分析助手。
 * 负责相邻剧集衔接评分、角色指纹计算等启发式逻辑。
 */
@Component
public class DramaSceneManager {

    @Resource
    private SvDramaEpisodeRepository episodeRepository;
    @Resource
    private SvDramaCharacterRepository characterRepository;

    /**
     * D-3：相邻集剧情连贯性启发式分析。
     */
    public Map<String, Object> analyzeEpisodeContinuity(Long dramaId) {
        List<SvDramaEpisode> eps = episodeRepository.findByDramaIdOrderByEpisodeNumberAsc(dramaId);
        List<SvDramaCharacter> chars = characterRepository.findByDramaIdOrderByIdAsc(dramaId);
        Set<String> charNames = new HashSet<>();
        for (SvDramaCharacter c : chars) {
            if (c != null && StringUtils.hasText(c.getCharacterName())) {
                charNames.add(c.getCharacterName().trim());
            }
        }
        List<Map<String, Object>> pairs = new ArrayList<>();
        int sum = 0;
        int n = 0;
        int riskPairs = 0;
        int watchPairs = 0;
        int issuePairs = 0;
        for (int i = 0; i < eps.size() - 1; i++) {
            Map<String, Object> row = continuityPair(eps.get(i), eps.get(i + 1), charNames);
            pairs.add(row);
            Object sc = row.get("score");
            if (sc instanceof Number num) {
                sum += num.intValue();
                n++;
            }
            String band = row.get("band") instanceof String s ? s : "";
            if ("risk".equals(band)) {
                riskPairs++;
            } else if ("watch".equals(band)) {
                watchPairs++;
            }
            if (row.get("issues") instanceof List<?> l && !l.isEmpty()) {
                issuePairs++;
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        Integer overallScore = n > 0 ? Math.round((float) sum / n) : null;
        out.put("dramaId", dramaId);
        out.put("pairCount", pairs.size());
        out.put("overallScore", overallScore);
        out.put("overallBand", overallScore != null ? continuityBandByScore(overallScore) : "unknown");
        out.put("overallLabel", overallScore != null ? continuityLabelByScore(overallScore) : "无法评估");
        out.put("riskPairCount", riskPairs);
        out.put("watchPairCount", watchPairs);
        out.put("issuePairCount", issuePairs);
        out.put("pairs", pairs);
        return out;
    }

    /**
     * D-2/H-3：统一连载健康度（启发式 pairs + 角色指纹）。
     */
    public Map<String, Object> buildContinuityReport(Long dramaId) {
        Map<String, Object> out = new LinkedHashMap<>(analyzeEpisodeContinuity(dramaId));
        List<SvDramaCharacter> chars = characterRepository.findByDramaIdOrderByIdAsc(dramaId);
        List<Map<String, Object>> fingerprints = new ArrayList<>();
        for (SvDramaCharacter c : chars) {
            if (c == null) {
                continue;
            }
            Map<String, Object> fp = new LinkedHashMap<>();
            fp.put("characterId", c.getId());
            fp.put("name", c.getCharacterName());
            fp.put("fingerprint", dramaCharacterFingerprint(c));
            fingerprints.add(fp);
        }
        out.put("continuityReportVersion", "v2");
        out.put("characterFingerprints", fingerprints);
        return out;
    }

    // ---- internal helpers ----

    Map<String, Object> continuityPair(SvDramaEpisode a, SvDramaEpisode b, Set<String> charNames) {
        List<String> issues = new ArrayList<>();
        String synA = a.getSynopsis() != null ? a.getSynopsis() : "";
        String synB = b.getSynopsis() != null ? b.getSynopsis() : "";
        String cliff = a.getCliffhanger() != null ? a.getCliffhanger() : "";
        int epA = a.getEpisodeNumber() != null ? a.getEpisodeNumber() : 0;
        int epB = b.getEpisodeNumber() != null ? b.getEpisodeNumber() : epA + 1;
        int sequenceGap = epB - epA - 1;
        if (sequenceGap > 0) {
            issues.add("集号不连续（第" + epA + "集→第" + epB + "集），时间线可能存在断层");
        }
        Set<String> tokA = continuityTokens(synA);
        Set<String> tokB = continuityTokens(synB);
        int tokenOverlap = 0;
        for (String t : tokA) {
            if (tokB.contains(t)) {
                tokenOverlap++;
            }
        }
        boolean cliffhangerDirty = false;
        if (!StringUtils.hasText(cliff) && StringUtils.hasText(synA)) {
            String parsedHook = extractCliffhangerFromScriptBody(synA);
            if (StringUtils.hasText(parsedHook)) {
                cliffhangerDirty = true;
                issues.add("第" + epA + "集：正文含悬念钩子但若「悬念」字段为空，建议保存或重新解析入库");
            }
        }
        boolean nextSynopsisTooShort = false;
        if (StringUtils.hasText(synA) && StringUtils.hasText(synB) && synB.trim().length() < 40 && tokenOverlap == 0) {
            nextSynopsisTooShort = true;
            issues.add("第" + epB + "集概要过短且与上集无共同词元，角色/情节延续难判断");
        }
        double jaccard = continuityJaccard(tokA, tokB);
        double cliffRatio;
        double openingCliffRecall = 0.0;
        int cliffTokenCount = 0;
        int cliffTokenHit = 0;
        if (!StringUtils.hasText(cliff)) {
            cliffRatio = 0.35;
            issues.add("第" + epA + "集未填写悬念/钩子");
        } else {
            Set<String> cliffTok = continuityTokens(cliff);
            cliffTokenCount = cliffTok.size();
            if (cliffTok.isEmpty()) {
                cliffRatio = 0.4;
            } else {
                long hit = cliffTok.stream().filter(tokB::contains).count();
                cliffTokenHit = (int) hit;
                cliffRatio = (double) hit / cliffTok.size();
                if (StringUtils.hasText(synB)) {
                    String openB = synB.length() > 160 ? synB.substring(0, 160) : synB;
                    long hitOpen = cliffTok.stream().filter(openB::contains).count();
                    openingCliffRecall = (double) hitOpen / cliffTok.size();
                    if (cliffTok.size() >= 2 && openingCliffRecall < 0.2 && cliffRatio >= 0.12) {
                        issues.add("悬念关键词多出现在下集中后段，建议开篇先落一脚回收钩子");
                    }
                }
            }
            if (cliffRatio < 0.12 && continuityTokens(cliff).size() >= 2) {
                issues.add("第" + epA + "集悬念在下集概要中体现较弱");
            }
        }
        if (!StringUtils.hasText(synA) || !StringUtils.hasText(synB)) {
            issues.add("相邻集概要缺失，衔接难评估");
        }
        int sharedChar = 0;
        List<String> singleSideChars = new ArrayList<>();
        for (String name : charNames) {
            if (name == null || name.length() < 2) continue;
            boolean inA = synA.contains(name);
            boolean inB = synB.contains(name);
            if (inA && inB) {
                sharedChar++;
            } else if (inA ^ inB) {
                singleSideChars.add(name);
            }
        }
        if (!singleSideChars.isEmpty() && StringUtils.hasText(synA) && StringUtils.hasText(synB)) {
            int show = Math.min(3, singleSideChars.size());
            StringBuilder sb = new StringBuilder("角色仅在单集概要出现：");
            for (int i = 0; i < show; i++) {
                if (i > 0) sb.append("、");
                sb.append(singleSideChars.get(i));
            }
            if (singleSideChars.size() > show) {
                sb.append(" 等");
            }
            issues.add(sb.toString());
        }
        int jaccardScore = (int) Math.round(28 * jaccard);
        int cliffScore = (int) Math.round(22 * cliffRatio);
        int charScore = Math.min(10, sharedChar * 4);
        int score = 40 + jaccardScore + cliffScore + charScore;
        if (sequenceGap > 0) {
            score -= Math.min(12, 4 + sequenceGap * 3);
        }
        score = Math.max(0, Math.min(100, score));
        String band = continuityBandByScore(score);
        List<String> recommendations = new ArrayList<>();
        if (!StringUtils.hasText(synA) || !StringUtils.hasText(synB)) {
            recommendations.add("先补齐相邻集剧情概要，再做连贯分析。");
        } else {
            if (tokenOverlap <= 2) {
                recommendations.add("在下集开头回收上集关键词（人物、地点、目标）增强衔接。");
            }
            if (cliffTokenCount >= 2 && cliffRatio < 0.12) {
                recommendations.add("将上集钩子中的核心动作或结果写入下集前 1-2 段。");
            }
            if (sharedChar == 0 && !charNames.isEmpty()) {
                recommendations.add("加入至少一位跨集角色，避免两集叙事断层。");
            }
        }
        if (recommendations.isEmpty()) {
            recommendations.add("衔接稳定，可继续保持当前叙事节奏。");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("fromEpisode", epA);
        m.put("toEpisode", epB);
        m.put("score", score);
        m.put("band", band);
        m.put("label", continuityLabelByScore(score));
        m.put("tokenJaccard", Math.round(jaccard * 1000.0) / 1000.0);
        m.put("tokenOverlap", tokenOverlap);
        m.put("cliffRecallInNext", Math.round(cliffRatio * 1000.0) / 1000.0);
        m.put("openingCliffRecall", Math.round(openingCliffRecall * 1000.0) / 1000.0);
        m.put("sequenceGapEpisodes", sequenceGap);
        m.put("cliffTokenCount", cliffTokenCount);
        m.put("cliffTokenHitInNext", cliffTokenHit);
        m.put("sharedCharacterMentions", sharedChar);
        m.put("singleSideCharacterCount", singleSideChars.size());
        m.put("cliffhangerDirty", cliffhangerDirty);
        m.put("nextSynopsisTooShort", nextSynopsisTooShort);
        m.put("components", Map.of(
                "base", 40,
                "jaccardScore", jaccardScore,
                "cliffScore", cliffScore,
                "characterScore", charScore));
        m.put("issues", issues);
        m.put("recommendations", recommendations);
        return m;
    }

    // ---- static utilities (package-visible so DramaAiAdapter can reuse) ----

    static Set<String> continuityTokens(String text) {
        Set<String> out = new HashSet<>();
        if (!StringUtils.hasText(text)) {
            return out;
        }
        Matcher mat = Pattern.compile("[\\u4e00-\\u9fa5]{2,}|[a-zA-Z]{2,}").matcher(text);
        while (mat.find()) {
            out.add(mat.group());
        }
        return out;
    }

    static double continuityJaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 1.0;
        }
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        int inter = 0;
        for (String x : a) {
            if (b.contains(x)) {
                inter++;
            }
        }
        int union = a.size() + b.size() - inter;
        return union <= 0 ? 0.0 : (double) inter / union;
    }

    static String continuityBandByScore(int score) {
        if (score >= 80) return "good";
        if (score >= 60) return "watch";
        return "risk";
    }

    static String continuityLabelByScore(int score) {
        if (score >= 80) return "衔接良好";
        if (score >= 60) return "可用但建议优化";
        return "衔接风险较高";
    }

    /**
     * D-4：从单集正文中解析悬念句；未匹配时返回 null。
     */
    static String extractCliffhangerFromScriptBody(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        Pattern[] patterns = new Pattern[]{
                Pattern.compile("【悬念钩子】\\s*([^\\n【]+)"),
                Pattern.compile("悬念钩子\\s*[：:]\\s*([^\\n]+)"),
                Pattern.compile("【钩子】\\s*([^\\n]+)"),
        };
        for (Pattern p : patterns) {
            Matcher m = p.matcher(content);
            if (m.find()) {
                String h = m.group(1).trim();
                if (h.length() > 500) {
                    h = h.substring(0, 500);
                }
                return h.isEmpty() ? null : h;
            }
        }
        return null;
    }

    static String dramaCharacterFingerprint(SvDramaCharacter c) {
        String norm = (safeStr(c.getCharacterName()) + "|" + safeStr(c.getDescription()) + "|" + safeStr(c.getPromptTags()))
                .trim().toLowerCase(Locale.ROOT);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(norm.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                sb.append(String.format(Locale.ROOT, "%02x", h[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return "0000000000000000";
        }
    }

    static String safeStr(String s) {
        return s == null ? "" : s;
    }
}
