package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.entity.AiTaskModelConfig;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiTaskModelConfigRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDramaCharacter;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDramaEpisode;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDrama;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvDramaEpisodeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 短剧 AI 适配器。
 * 负责 LLM 调用（剧本生成、逐集精修）、悬念承接逻辑以及模型解析。
 */
@Component
public class DramaAiAdapter {

    private static final Logger log = LoggerFactory.getLogger(DramaAiAdapter.class);

    @Autowired(required = false)
    private LlmClient llmClient;
    @Autowired(required = false)
    private AiTaskModelConfigRepository taskModelConfigRepository;
    @Autowired(required = false)
    private AiModelRepository modelRepository;
    @Autowired
    private SvDramaEpisodeRepository episodeRepository;

    /** D-4：默认悬念承接模式 */
    @Value("${app.shortvideo.drama.cliffhanger-carryover:normal}")
    private String defaultCliffhangerCarryoverMode;
    /** D-4：追加到编剧 system 提示的可运营片段 */
    @Value("${app.shortvideo.drama.script-system-extra:}")
    private String dramaScriptSystemExtra;
    /** D-4：追加到 user 提示的可运营片段 */
    @Value("${app.shortvideo.drama.script-user-extra:}")
    private String dramaScriptUserExtra;

    /**
     * 生成全剧剧本正文并写入各集概要/悬念。
     *
     * @param drama             短剧实体
     * @param characters        角色列表
     * @param episodes          剧集列表
     * @param theme             题材
     * @param style             风格
     * @param progress          SSE 进度回调（可 null）
     * @param cliffhangerCarryoverMode 悬念承接模式
     * @param episodeApplier    将脚本应用到剧集的回调（委托 DramaServiceImpl.applyScriptToEpisodes）
     * @param episodeAdder      创建新剧集的回调（委托 DramaServiceImpl.addEpisode）
     * @return 生成的完整剧本文本
     */
    public String generateDramaScript(SvDrama drama, List<SvDramaCharacter> characters,
                                      List<SvDramaEpisode> episodes, Long ownerId,
                                      String theme, String style,
                                      Consumer<Map<String, Object>> progress,
                                      String cliffhangerCarryoverMode,
                                      ScriptEpisodeApplier episodeApplier,
                                      EpisodeAdder episodeAdder) {
        int totalEps = drama.getTotalEpisodes() != null && drama.getTotalEpisodes() > 0 ? drama.getTotalEpisodes() : 1;
        String carryMode = resolveCliffhangerCarryoverMode(cliffhangerCarryoverMode);

        if (progress != null) {
            Map<String, Object> start = new LinkedHashMap<>();
            start.put("phase", "episode_start");
            start.put("episode", 1);
            start.put("total", totalEps);
            progress.accept(start);
        }

        if (llmClient != null) {
            try {
                List<AiModel> models = resolveDramaModels();
                if (!models.isEmpty()) {
                    String charDesc = characters.stream()
                            .map(c -> c.getCharacterName() + (StringUtils.hasText(c.getDescription()) ? "：" + c.getDescription() : ""))
                            .reduce("", (a, b) -> a + "\n- " + b);
                    if (!charDesc.isEmpty()) charDesc = "\n角色设定：\n" + charDesc;

                    String episodeContext = buildEpisodeContextForPrompt(episodes, carryMode);
                    if (!episodeContext.isEmpty()) episodeContext = "\n已有剧集概要：\n" + episodeContext;

                    String carrySystemExtra = cliffhangerCarryoverSystemExtra(carryMode);
                    String carryUserExtra = cliffhangerCarryoverUserExtra(episodes, carryMode);

                    String system = String.format("""
                            你是专业的短剧编剧。根据用户提供的短剧信息，生成符合抖音短剧风格的剧本。
                            严格按以下分隔符格式输出每集内容，共 %d 集：
                            ---EPISODE_1_START---
                            （第1集剧本内容，包含【场景】【人物】【对白】【动作提示】）
                            每集正文最后必须单独一行，格式：【悬念钩子】后接 15～50 字强悬念，便于系统写入分集「悬念」字段。
                            ---EPISODE_1_END---
                            ---EPISODE_2_START---
                            ...
                            ---EPISODE_2_END---
                            每集时长约60-90秒，对白简洁有力，节奏紧凑，适合竖屏短视频。
                            只输出剧本正文，不要额外说明。必须使用 ---EPISODE_N_START--- 和 ---EPISODE_N_END--- 分隔每集。
                            """, totalEps) + carrySystemExtra
                            + (StringUtils.hasText(dramaScriptSystemExtra) ? "\n" + dramaScriptSystemExtra.trim() : "");
                    String prompt = String.format("短剧《%s》%d集%s%s%s%s。请生成完整剧本：",
                            drama.getTitle(),
                            totalEps,
                            StringUtils.hasText(drama.getGenre()) ? "，类型：" + drama.getGenre() : "",
                            StringUtils.hasText(drama.getDescription()) ? "，简介：" + drama.getDescription() : "",
                            StringUtils.hasText(theme) ? "，题材：" + theme : "",
                            StringUtils.hasText(style) ? "，风格：" + style : "")
                            + charDesc + episodeContext + carryUserExtra
                            + (StringUtils.hasText(dramaScriptUserExtra) ? "\n" + dramaScriptUserExtra.trim() : "");

                    var response = llmClient.chatWithFallback(models, system, prompt);
                    if (response.success() && StringUtils.hasText(response.content())) {
                        String fullScript = response.content().trim();
                        int[] successCount = new int[]{0};
                        int splitApplied = splitAndApplyEpisodes(drama.getId(), ownerId, fullScript, episodes, progress, successCount, episodeAdder);
                        if (splitApplied == 0) {
                            int n = episodeApplier.apply(drama.getId(), ownerId, fullScript);
                            successCount[0] = n;
                            if (progress != null && n > 0) {
                                Map<String, Object> m = new LinkedHashMap<>();
                                m.put("phase", "episode_done");
                                m.put("episode", 1);
                                m.put("ok", true);
                                m.put("message", "已按正文解析写入 " + n + " 集");
                                progress.accept(m);
                            }
                        }
                        if (progress != null) {
                            int ok = successCount[0];
                            Map<String, Object> c = new LinkedHashMap<>();
                            c.put("phase", "complete");
                            c.put("successes", ok);
                            c.put("failures", Math.max(0, totalEps - ok));
                            c.put("total", totalEps);
                            progress.accept(c);
                        }
                        return fullScript;
                    }
                }
            } catch (Exception e) {
                log.warn("Drama generate-script LLM 调用失败: {}", e.getMessage());
            }
        }

        String fallback = "【AI 剧本生成】短剧《" + drama.getTitle() + "》" + (drama.getTotalEpisodes() != null ? drama.getTotalEpisodes() : 1) + "集"
                + (StringUtils.hasText(theme) ? "，题材：" + theme : "")
                + (StringUtils.hasText(style) ? "，风格：" + style : "")
                + "。请配置 LLM 以启用完整剧本生成。";
        if (progress != null) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("phase", "complete");
            c.put("successes", 0);
            c.put("failures", totalEps);
            c.put("total", totalEps);
            progress.accept(c);
        }
        return fallback;
    }

    /**
     * D-4：逐集 LLM 精修概要/悬念。
     *
     * @param ep                剧集实体
     * @param characters        角色列表
     * @param cliffhangerContext 运营承接说明
     * @param episodeUpdater    更新剧集的回调（委托 DramaServiceImpl.updateEpisode）
     * @return 精修结果 Map
     */
    public Map<String, Object> refineEpisode(SvDramaEpisode ep, List<SvDramaCharacter> characters,
                                              String cliffhangerContext, EpisodeUpdater episodeUpdater, Long ownerId) {
        String ctx = cliffhangerContext != null ? cliffhangerContext.trim() : "";
        String charDesc = characters.stream()
                .map(c -> c.getCharacterName() + (StringUtils.hasText(c.getDescription()) ? "：" + c.getDescription() : ""))
                .reduce("", (a, b) -> a + "\n- " + b);
        if (!charDesc.isEmpty()) {
            charDesc = "\n角色设定：\n" + charDesc;
        }

        SvDramaEpisode prev = findPreviousEpisode(ep);
        String prevHook = "";
        if (prev != null) {
            String ph = effectiveCliffhanger(prev);
            if (StringUtils.hasText(ph)) {
                prevHook = "\n上一集（第" + prev.getEpisodeNumber() + "集）悬念须承接：" + ph;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("episodeId", ep.getId());

        if (llmClient == null) {
            result.put("ok", false);
            result.put("message", "LLM 未配置");
            return result;
        }
        List<AiModel> models = resolveDramaModels();
        if (models.isEmpty()) {
            result.put("ok", false);
            result.put("message", "无可用模型");
            return result;
        }

        int epNum = ep.getEpisodeNumber() != null ? ep.getEpisodeNumber() : 0;
        String title = ep.getTitle() != null ? ep.getTitle() : "";
        String syn = ep.getSynopsis() != null ? ep.getSynopsis() : "";
        String cliff = ep.getCliffhanger() != null ? ep.getCliffhanger() : "";

        String system = String.format("""
                你是抖音竖屏短剧编剧。只输出一个 JSON 对象，不要 markdown 代码块。
                字段：synopsis（ string，第%d 集剧情概要 80～400 字）、cliffhanger（ string，15～50 字强悬念）。
                必须保持角色人设一致，承接上文悬念（若有）。""", epNum);
        String user = String.format("短剧第 %d 集《%s》。\n当前概要：\n%s\n当前悬念字段：\n%s\n运营补充/承接说明：\n%s%s%s",
                epNum, title, syn, cliff, ctx.isEmpty() ? "（无）" : ctx, prevHook, charDesc);

        try {
            var response = llmClient.chatWithFallback(models, system, user);
            if (!response.success() || !StringUtils.hasText(response.content())) {
                result.put("ok", false);
                result.put("message", response.errorMsg() != null ? response.errorMsg() : "LLM 无输出");
                return result;
            }
            String raw = response.content().trim()
                    .replaceAll("^```json\\s*", "")
                    .replaceAll("```\\s*$", "");
            String jsonSlice = extractJsonObject(raw);
            ObjectMapper om = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = om.readValue(jsonSlice, Map.class);
            Object synObj = parsed.get("synopsis");
            Object cliffObj = parsed.get("cliffhanger");
            String newSyn = synObj != null ? synObj.toString().trim() : "";
            String newCliff = cliffObj != null ? cliffObj.toString().trim() : "";
            if (!StringUtils.hasText(newSyn)) {
                result.put("ok", false);
                result.put("message", "模型未返回 synopsis");
                result.put("raw", raw);
                return result;
            }
            String cliffToSave = StringUtils.hasText(newCliff) ? newCliff : cliff;
            episodeUpdater.update(ep.getId(), ownerId, title, newSyn, cliffToSave);
            result.put("ok", true);
            result.put("synopsis", newSyn);
            result.put("cliffhanger", cliffToSave);
            return result;
        } catch (Exception e) {
            log.warn("refineEpisode failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "逐集精修失败: " + e.getMessage());
        }
    }

    // ---- callbacks (functional interfaces) ----

    @FunctionalInterface
    public interface ScriptEpisodeApplier {
        int apply(Long dramaId, Long ownerId, String script);
    }

    @FunctionalInterface
    public interface EpisodeAdder {
        SvDramaEpisode add(Long dramaId, Long ownerId, int episodeNumber, String title, String synopsis, String cliffhanger);
    }

    @FunctionalInterface
    public interface EpisodeUpdater {
        void update(Long episodeId, Long ownerId, String title, String synopsis, String cliffhanger);
    }

    // ---- internal helpers ----

    String resolveCliffhangerCarryoverMode(String requestOverride) {
        if (StringUtils.hasText(requestOverride)) {
            return normalizeCliffhangerCarryoverMode(requestOverride);
        }
        return normalizeCliffhangerCarryoverMode(defaultCliffhangerCarryoverMode);
    }

    private static String normalizeCliffhangerCarryoverMode(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "normal";
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if ("strong".equals(s) || "hard".equals(s)) {
            return "strong";
        }
        if ("off".equals(s) || "false".equals(s) || "none".equals(s)) {
            return "off";
        }
        return "normal";
    }

    /** 悬念字段优先；否则从正文解析【悬念钩子】等（D-4） */
    String effectiveCliffhanger(SvDramaEpisode e) {
        if (e == null) {
            return null;
        }
        if (StringUtils.hasText(e.getCliffhanger())) {
            return e.getCliffhanger().trim();
        }
        return DramaSceneManager.extractCliffhangerFromScriptBody(e.getSynopsis());
    }

    private String buildEpisodeContextForPrompt(List<SvDramaEpisode> episodes, String carryMode) {
        if (episodes == null || episodes.isEmpty()) {
            return "";
        }
        boolean inject = !"off".equals(carryMode);
        StringBuilder sb = new StringBuilder();
        for (SvDramaEpisode e : episodes) {
            if (e == null) {
                continue;
            }
            int num = e.getEpisodeNumber() != null ? e.getEpisodeNumber() : 0;
            String title = e.getTitle() != null ? e.getTitle() : "";
            String syn = StringUtils.hasText(e.getSynopsis()) ? e.getSynopsis().trim() : "";
            sb.append("第").append(num).append("集：").append(title);
            if (StringUtils.hasText(syn)) {
                sb.append(" ").append(syn);
            }
            if (inject) {
                String hook = effectiveCliffhanger(e);
                if (StringUtils.hasText(hook)) {
                    sb.append(" | 本集悬念（下一集开篇须承接）：").append(hook);
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private static String cliffhangerCarryoverSystemExtra(String mode) {
        if ("off".equals(mode)) {
            return "";
        }
        if ("strong".equals(mode)) {
            return """

                    【悬念承接·强约束】从第2集起，正文前 80 字内必须直接点名或行动化解上一集「本集悬念」中的具体人、事、物或问题；禁止开篇另起与上集无关的全新故事线。若上一集未提供悬念文本，则必须自然承接该集概要的最后情节。各集之间场景/人物动机需连贯。""";
        }
        return """

                【悬念承接·常规】从第2集起，开篇用 1～3 句对白或画外音/动作回收上一集悬念中的核心矛盾或疑问，再展开本集情节；不得完全忽略上集伏笔。各集之间人物与主线需前后呼应。""";
    }

    private String cliffhangerCarryoverUserExtra(List<SvDramaEpisode> episodes, String mode) {
        if ("off".equals(mode) || episodes == null || episodes.size() < 2) {
            return "";
        }
        List<SvDramaEpisode> sorted = new ArrayList<>(episodes);
        sorted.sort((a, b) -> Integer.compare(
                a.getEpisodeNumber() != null ? a.getEpisodeNumber() : 0,
                b.getEpisodeNumber() != null ? b.getEpisodeNumber() : 0));
        StringBuilder sb = new StringBuilder("\n逐集悬念衔接清单（写作时务必满足）：\n");
        boolean any = false;
        for (int i = 0; i < sorted.size() - 1; i++) {
            SvDramaEpisode cur = sorted.get(i);
            SvDramaEpisode nxt = sorted.get(i + 1);
            String hook = effectiveCliffhanger(cur);
            int from = cur.getEpisodeNumber() != null ? cur.getEpisodeNumber() : i + 1;
            int to = nxt.getEpisodeNumber() != null ? nxt.getEpisodeNumber() : i + 2;
            if (StringUtils.hasText(hook)) {
                sb.append("- 第").append(from).append("集末悬念 → 第").append(to).append("集开篇必须落地：「").append(hook).append("」\n");
                any = true;
            }
        }
        return any ? sb.toString() : "";
    }

    private int splitAndApplyEpisodes(Long dramaId, Long ownerId, String fullScript, List<SvDramaEpisode> existingEpisodes,
                                      Consumer<Map<String, Object>> progress, int[] successCount,
                                      EpisodeAdder episodeAdder) {
        Pattern episodePattern = Pattern.compile(
                "---EPISODE_(\\d+)_START---\\s*(.+?)\\s*---EPISODE_\\1_END---",
                Pattern.DOTALL);
        Matcher matcher = episodePattern.matcher(fullScript);

        Map<Integer, SvDramaEpisode> byNumber = existingEpisodes.stream()
                .collect(Collectors.toMap(SvDramaEpisode::getEpisodeNumber, e -> e, (a, b) -> a));

        int applied = 0;
        while (matcher.find()) {
            int epNum = Integer.parseInt(matcher.group(1));
            String content = matcher.group(2).trim();
            if (content.isEmpty()) continue;

            String hook = DramaSceneManager.extractCliffhangerFromScriptBody(content);
            SvDramaEpisode ep = byNumber.get(epNum);
            if (ep != null) {
                ep.setSynopsis(content);
                ep.setCliffhanger(hook);
                episodeRepository.save(ep);
            } else {
                SvDramaEpisode newEp = episodeAdder.add(dramaId, ownerId, epNum, "第" + epNum + "集", content, hook);
                byNumber.put(epNum, newEp);
            }
            applied++;
            successCount[0]++;
            if (progress != null) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("phase", "episode_done");
                m.put("episode", epNum);
                m.put("ok", true);
                progress.accept(m);
            }
        }

        if (applied == 0) {
            log.info("未检测到 EPISODE 分隔符，尝试 fallback 拆分");
            return 0;
        }
        log.info("已从生成剧本中拆分并保存 {} 集内容", applied);
        return applied;
    }

    SvDramaEpisode findPreviousEpisode(SvDramaEpisode ep) {
        if (ep == null || ep.getDramaId() == null || ep.getEpisodeNumber() == null || ep.getEpisodeNumber() <= 1) {
            return null;
        }
        int want = ep.getEpisodeNumber() - 1;
        for (SvDramaEpisode x : episodeRepository.findByDramaIdOrderByEpisodeNumberAsc(ep.getDramaId())) {
            if (x != null && want == (x.getEpisodeNumber() != null ? x.getEpisodeNumber() : -1)) {
                return x;
            }
        }
        return null;
    }

    static String extractJsonObject(String raw) {
        if (raw == null) {
            return "{}";
        }
        int i = raw.indexOf('{');
        int j = raw.lastIndexOf('}');
        if (i >= 0 && j > i) {
            return raw.substring(i, j + 1);
        }
        return raw;
    }

    List<AiModel> resolveDramaModels() {
        var config = taskModelConfigRepository != null
                ? taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted("short_video_script", 1, 0)
                : java.util.Optional.<AiTaskModelConfig>empty();
        if (config.isPresent() && modelRepository != null) {
            AiTaskModelConfig tc = config.get();
            List<AiModel> result = new ArrayList<>();
            for (Long modelId : Arrays.asList(tc.getPrimaryModelId(), tc.getFallbackModelId(), tc.getFallback2ModelId())) {
                if (modelId == null) continue;
                modelRepository.findById(modelId).filter(m -> m.getStatus() == 1 && m.getDeleted() == 0)
                        .ifPresent(result::add);
            }
            if (!result.isEmpty()) return result;
        }
        if (modelRepository != null) {
            return modelRepository.findByStatusAndDeleted(1, 0).stream().limit(3).toList();
        }
        return List.of();
    }
}
