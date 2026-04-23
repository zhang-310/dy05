package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiFullResultVO;
import cn.gaifan.douyinOperations.module.live.vo.LiveAiResultVO;
import cn.gaifan.douyinOperations.module.product.entity.DyProductScript;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 直播话术生成后处理工具：RAG 引用转换、时间线构建、质量入库、A/B 分配等。
 * 从 LiveScriptGenerationServiceImpl 提取的无状态工具方法集合。
 */
@Component
public class LiveScriptPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(LiveScriptPostProcessor.class);

    @Resource
    private LiveScriptRepository scriptRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    private LiveKnowledgeBaseAccessResolver knowledgeBaseAccessResolver;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private cn.gaifan.douyinOperations.module.abtest.service.ScriptStyleAbService scriptStyleAbService;

    // ─── 记录类型 ──────────────────────────────────────

    record GenerateResult(String content, List<KnowledgeBaseService.SearchResult> ragRefs, String generationPromptHash) {}

    record SlotResult(LiveAiResultVO result, double consumption) {}

    record TransitionProductPair(String from, String to) {}

    // ─── RAG 引用处理 ──────────────────────────────────────

    static final int RAG_REF_PREVIEW_LEN = 120;

    static List<LiveAiResultVO.RagRefVO> toRagRefVOs(List<KnowledgeBaseService.SearchResult> refs) {
        if (refs == null || refs.isEmpty()) return null;
        List<LiveAiResultVO.RagRefVO> list = new ArrayList<>();
        for (KnowledgeBaseService.SearchResult r : refs) {
            LiveAiResultVO.RagRefVO vo = new LiveAiResultVO.RagRefVO();
            vo.setDocId(r.docId());
            vo.setChunkId(r.chunkId());
            vo.setTitle(r.title());
            String content = r.content();
            vo.setContentPreview(content != null && content.length() > RAG_REF_PREVIEW_LEN ? content.substring(0, RAG_REF_PREVIEW_LEN) + "…" : content);
            vo.setScore(r.score());
            list.add(vo);
        }
        return list;
    }

    static String mergeReferencedChunkIds(List<String> jsonArrays) {
        if (jsonArrays == null || jsonArrays.isEmpty()) return null;
        Set<Long> all = new HashSet<>();
        for (String s : jsonArrays) {
            if (s == null || s.isBlank()) continue;
            try {
                List<Number> nums = com.alibaba.fastjson2.JSON.parseArray(s, Number.class);
                if (nums != null) nums.forEach(n -> all.add(n.longValue()));
            } catch (Exception ignored) {}
        }
        if (all.isEmpty()) return null;
        return com.alibaba.fastjson2.JSON.toJSONString(new ArrayList<>(all));
    }

    static String toReferencedChunkIdsJson(List<KnowledgeBaseService.SearchResult> refs) {
        if (refs == null || refs.isEmpty()) return null;
        List<Long> ids = refs.stream()
                .map(KnowledgeBaseService.SearchResult::chunkId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) return null;
        return com.alibaba.fastjson2.JSON.toJSONString(ids);
    }

    // ─── 时间线构建 ──────────────────────────────────────

    static final String[][] TIME_RANGES = {
        {"0-15min", "留人破冰"},
        {"15-45min", "价值输出"},
        {"45-75min", "转化高潮"},
        {"75-105min", "逼单冲刺"},
        {"105-120min", "收尾预告"},
    };

    static List<LiveAiFullResultVO.TimelineEntry> buildTimeline(List<LiveScript> slots) {
        List<LiveAiFullResultVO.TimelineEntry> timeline = new ArrayList<>();
        int total = slots.size();
        for (int i = 0; i < total; i++) {
            LiveScript s = slots.get(i);
            double ratio = total > 1 ? (double) i / total : 0;
            int rangeIdx = Math.min((int) (ratio * TIME_RANGES.length), TIME_RANGES.length - 1);
            LiveAiFullResultVO.TimelineEntry entry = new LiveAiFullResultVO.TimelineEntry();
            entry.setTimeRange(TIME_RANGES[rangeIdx][0]);
            entry.setTarget(TIME_RANGES[rangeIdx][1]);
            entry.setScriptType(s.getScriptType());
            entry.setScriptId(s.getId());
            entry.setSequenceNo(s.getSequenceNo() != null ? s.getSequenceNo() : i);
            String content = s.getScriptContent();
            if (content != null && content.length() > 100) {
                entry.setSummary(content.substring(0, 100) + "...");
            } else {
                entry.setSummary(content);
            }
            timeline.add(entry);
        }
        return timeline;
    }

    // ─── 槽位标签 ──────────────────────────────────────

    static String slotLabel(String scriptType, int index, int productCount) {
        if (scriptType == null) return "自定义";
        return switch (scriptType) {
            case "opening" -> "开场";
            case "product" -> "产品" + ((index + 1) / 2);
            case "transition" -> "转场" + (index / 2);
            case "closing" -> "收尾";
            case "chat" -> "聊家常" + Math.max(1, (index + 2) / 3);
            default -> scriptType;
        };
    }

    // ─── 默认需求描述 ──────────────────────────────────────

    static String defaultRequirement(String scriptType) {
        return switch (scriptType) {
            case "opening" -> "开场白";
            case "product" -> "产品介绍";
            case "transition" -> "转场";
            case "closing" -> "收尾";
            case "chat" -> "聊家常（夫妻/婆媳/励志/歇后语/名言等）";
            case "interaction" -> "互动引导";
            case "welfare" -> "福利话术";
            case "closing_deal" -> "逼单促单";
            case "hold_back" -> "憋单蓄水";
            case "emotional" -> "情绪价值";
            case "rapid_intro" -> "快速过品";
            case "deep_sell" -> "深度单品";
            case "pain_point" -> "痛点放大";
            case "testimony" -> "用户证言";
            case "custom" -> "自定义";
            default -> "互动引导";
        };
    }

    // ─── 引用快照构建 ──────────────────────────────────────

    String buildReferencedSnapshot(DyProductScript ps) {
        try {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", ps.getId());
            m.put("version", ps.getVersion() != null ? ps.getVersion() : 1);
            m.put("content", ps.getScriptContent());
            m.put("style", ps.getStyle());
            m.put("scriptType", ps.getScriptType());
            return new ObjectMapper().writeValueAsString(m);
        } catch (Exception e) {
            log.warn("构建引用快照失败: {}", e.getMessage());
            return null;
        }
    }

    // ─── 序号获取 ──────────────────────────────────────

    int getNextSequenceNo(Long sessionId) {
        Integer max = scriptRepository.findMaxSequenceNoBySessionId(sessionId);
        return (max != null ? max : 0) + 1;
    }

    // ─── 高质量话术自动入库 ──────────────────────────────────────

    void autoIngestHighQualityScript(String content, String scriptType, Long userId, double score) {
        if (knowledgeBaseService == null) {
            log.debug("高质量话术入库跳过：KnowledgeBaseService 未注入");
            return;
        }
        try {
            LiveKnowledgeBaseAccessResolver.ResolvedKnowledgeBase resolvedKb =
                    knowledgeBaseAccessResolver.resolveHuashu(userId);
            if (resolvedKb == null) {
                log.debug("高质量话术入库跳过：未找到 huashu 知识库");
                return;
            }
            String title = String.format("高效话术[%s]·%.1f分", scriptType != null ? scriptType : "通用", score);
            knowledgeBaseService.uploadDocument(
                    resolvedKb.kbId(),
                    title,
                    content,
                    "text",
                    resolvedKb.accessUserId(),
                    "high_quality_script",
                    "script");
            log.info("高质量话术自动入库: type={}, score={}, kbId={}, sharedFallback={}",
                    scriptType, score, resolvedKb.kbId(), resolvedKb.sharedFallback());
        } catch (Exception e) {
            log.debug("高质量话术入库失败: {}", e.getMessage());
        }
    }

    // ─── A/B 风格分配 ──────────────────────────────────────

    cn.gaifan.douyinOperations.module.abtest.vo.ScriptStyleAssignVO tryAssignAbStyle(LiveSession session) {
        if (scriptStyleAbService == null) return null;
        try {
            return scriptStyleAbService.assignStyle(
                    session.getUserId(),
                    "live_session",
                    session.getId(),
                    "session_" + session.getId()
            );
        } catch (Exception e) {
            log.warn("A/B 风格分配失败，跳过: {}", e.getMessage());
            return null;
        }
    }
}
