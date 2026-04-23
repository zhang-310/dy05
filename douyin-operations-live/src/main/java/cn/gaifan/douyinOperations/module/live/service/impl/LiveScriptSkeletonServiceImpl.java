package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.ai.service.ModelChatStreamService;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptService;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptSkeletonService;
import cn.gaifan.douyinOperations.module.live.vo.SkeletonSlotVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 直播话术骨架生成服务实现。
 */
@Service
public class LiveScriptSkeletonServiceImpl implements LiveScriptSkeletonService {

    private static final Logger log = LoggerFactory.getLogger(LiveScriptSkeletonServiceImpl.class);

    @Resource private LiveScriptRepository scriptRepository;
    @Resource private LiveSessionRepository sessionRepository;
    @Resource private LiveScriptService liveScriptService;
    @Resource private LlmClient llmClient;
    @Resource private LiveAiModelHelper modelHelper;
    @Resource private ModelChatStreamService modelChatStreamService;
    @Resource private AiModelRepository aiModelRepository;

    private static ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Override
    public List<SkeletonSlotVO> generateSkeleton(Long sessionId, Long userId, Long modelId) {
        if (sessionId == null || sessionId <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "场次 ID 无效");
        }
        LiveSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "直播场次不存在"));
        liveScriptService.ensureScriptSlotsForSession(sessionId, session.getUserId());
        List<LiveScript> scripts = scriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(sessionId, 0);
        if (scripts.isEmpty()) return List.of();

        List<AiModel> models = modelHelper.resolveModels(modelId);
        if (models == null || models.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED, "无可用 AI 模型");
        }

        String scriptList = scripts.stream()
                .map(s -> "ID:" + s.getId() + " 类型:" + ("opening".equals(s.getScriptType()) ? "开场" : "product".equals(s.getScriptType()) ? "产品" : "transition".equals(s.getScriptType()) ? "转场" : "closing".equals(s.getScriptType()) ? "结尾" : s.getScriptType()))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
        String systemPrompt = """
            你是直播话术策划师。为每段话术生成「可执行」的一句话摘要和建议时长。
            summary：具体可展开的要点，如「开场：欢迎+点关注+预告福利」「产品1：卖点+限时抢购」。
            durationSec：开场60-90、产品120-180、转场20-40、结尾60-90，按类型合理设定。
            输出格式，每行一条：scriptId|summary|durationSec。按 scriptId 顺序输出。
            """;
        String userPrompt = "直播主题：" + (session.getLiveTitle() != null ? session.getLiveTitle() : "直播") + "\n\n话术槽位：\n" + scriptList + "\n\n请生成骨架（每行：scriptId|具体可展开的摘要|建议秒数）：";
        LlmClient.LlmResponse resp = llmClient.chatWithFallback(models, systemPrompt, userPrompt);
        if (!resp.success() || resp.content() == null || resp.content().isBlank()) {
            return scripts.stream().map(s -> {
                SkeletonSlotVO vo = new SkeletonSlotVO();
                vo.setScriptId(s.getId());
                vo.setScriptType(s.getScriptType());
                vo.setSummary("");
                vo.setSuggestedDurationSec(60);
                return vo;
            }).toList();
        }

        List<SkeletonSlotVO> result = new ArrayList<>();
        Map<Long, SkeletonSlotVO> byId = new java.util.HashMap<>();
        for (LiveScript s : scripts) {
            SkeletonSlotVO vo = new SkeletonSlotVO();
            vo.setScriptId(s.getId());
            vo.setScriptType(s.getScriptType());
            vo.setSummary("");
            vo.setSuggestedDurationSec(60);
            byId.put(s.getId(), vo);
            result.add(vo);
        }
        for (String line : resp.content().trim().split("\n")) {
            String[] parts = line.split("\\|");
            if (parts.length >= 3) {
                try {
                    Long id = Long.parseLong(parts[0].trim());
                    SkeletonSlotVO vo = byId.get(id);
                    if (vo != null) {
                        vo.setSummary(parts[1].trim());
                        vo.setSuggestedDurationSec(Integer.parseInt(parts[2].trim().replaceAll("\\D", "")));
                        if (vo.getSuggestedDurationSec() <= 0) vo.setSuggestedDurationSec(60);
                    }
                } catch (Exception e) {
                        log.warn("解析骨架行失败: {}", e.getMessage());
                    }
            }
        }
        for (SkeletonSlotVO vo : result) {
            if (vo.getScriptId() == null) continue;
            scriptRepository.findById(vo.getScriptId()).ifPresent(script -> {
                if (vo.getSummary() != null && !vo.getSummary().isBlank()) {
                    String req = vo.getSummary().length() > 128 ? vo.getSummary().substring(0, 128) : vo.getSummary();
                    script.setRequirement(req);
                }
                if (vo.getSuggestedDurationSec() != null && vo.getSuggestedDurationSec() > 0) {
                    script.setDurationLimitSec(vo.getSuggestedDurationSec());
                }
                scriptRepository.save(script);
            });
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateSkeletonStream(Long sessionId, Long userId, OutputStream out, Long modelId) throws java.io.IOException {
        if (sessionId == null || sessionId <= 0) {
            modelChatStreamService.writeErrorToStream(out, "场次 ID 无效");
            return;
        }
        LiveSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            modelChatStreamService.writeErrorToStream(out, "直播场次不存在");
            return;
        }
        liveScriptService.ensureScriptSlotsForSession(sessionId, session.getUserId());
        List<LiveScript> scripts = scriptRepository.findBySessionIdAndDeletedOrderBySequenceNoAsc(sessionId, 0);
        if (scripts.isEmpty()) {
            writeSkeletonDone(out, List.of());
            return;
        }
        Long resolvedModelId = modelHelper.resolveModelIdForStream(modelId);
        if (resolvedModelId == null) {
            modelChatStreamService.writeErrorToStream(out, "模型 ID 无效");
            return;
        }

        String scriptList = scripts.stream()
                .map(s -> "ID:" + s.getId() + " 类型:" + ("opening".equals(s.getScriptType()) ? "开场" : "product".equals(s.getScriptType()) ? "产品" : "transition".equals(s.getScriptType()) ? "转场" : "closing".equals(s.getScriptType()) ? "结尾" : s.getScriptType()))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
        String systemPrompt = """
            你是直播话术策划师。为每段话术生成「可执行」的一句话摘要和建议时长。
            summary：具体可展开的要点，如「开场：欢迎+点关注+预告福利」「产品1：卖点+限时抢购」。
            durationSec：开场60-90、产品120-180、转场20-40、结尾60-90，按类型合理设定。
            输出格式，每行一条：scriptId|summary|durationSec。按 scriptId 顺序输出。
            """;
        String userPrompt = "直播主题：" + (session.getLiveTitle() != null ? session.getLiveTitle() : "直播") + "\n\n话术槽位：\n" + scriptList + "\n\n请生成骨架（每行：scriptId|具体可展开的摘要|建议秒数）：";
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        );

        SkeletonStreamFilterOutputStream filterOut = new SkeletonStreamFilterOutputStream(out, scripts, scriptRepository, objectMapper());
        try {
            modelChatStreamService.streamChatToOutputStream(resolvedModelId, messages, filterOut);
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("Broken pipe") || msg.contains("Connection reset"))) {
                log.debug("客户端断开连接: sessionId={}", sessionId);
            } else {
                log.warn("generate-skeleton-sse 异常: {}", msg, e);
            }
            try {
                out.write(("event: error\ndata: " + objectMapper().writeValueAsString(Map.of("error", msg != null ? msg : "未知错误")) + "\n\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            } catch (Exception ex) {
                log.debug("写入SSE错误失败: {}", ex.getMessage());
            }
        } finally {
            filterOut.finish();
        }
    }

    private static void writeSkeletonDone(OutputStream out, List<SkeletonSlotVO> list) throws java.io.IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "ok");
        data.put("list", list);
        String json = objectMapper().writeValueAsString(data);
        String sse = "event: done\ndata: " + json + "\n\n";
        out.write(sse.getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private static class SkeletonStreamFilterOutputStream extends java.io.FilterOutputStream {
        private final java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        private final StringBuilder contentAccumulator = new StringBuilder();
        private final List<LiveScript> scripts;
        private final LiveScriptRepository scriptRepository;
        private final ObjectMapper objectMapper;
        private boolean finished;

        SkeletonStreamFilterOutputStream(OutputStream out, List<LiveScript> scripts,
                LiveScriptRepository scriptRepository, ObjectMapper objectMapper) {
            super(out);
            this.scripts = scripts;
            this.scriptRepository = scriptRepository;
            this.objectMapper = objectMapper;
        }

        @Override
        public void write(int b) throws java.io.IOException {
            buf.write(b);
            drainCompleteMessages();
        }

        @Override
        public void write(byte[] b, int off, int len) throws java.io.IOException {
            buf.write(b, off, len);
            drainCompleteMessages();
        }

        void finish() throws java.io.IOException {
            if (finished) return;
            finished = true;
            byte[] remaining = buf.toByteArray();
            if (remaining.length > 0) drainCompleteMessages();
        }

        private void drainCompleteMessages() throws java.io.IOException {
            byte[] bytes = buf.toByteArray();
            String s = new String(bytes, StandardCharsets.UTF_8);
            int idx;
            while ((idx = s.indexOf("\n\n")) >= 0) {
                String msg = s.substring(0, idx);
                s = s.substring(idx + 2);
                buf.reset();
                buf.write(s.getBytes(StandardCharsets.UTF_8));
                handleSseMessage(msg);
            }
        }

        private void handleSseMessage(String msg) throws java.io.IOException {
            String event = null;
            String data = null;
            for (String line : msg.split("\n")) {
                if (line.startsWith("event: ")) event = line.substring(7).trim();
                else if (line.startsWith("data: ")) data = line.substring(6);
            }
            if (event == null || data == null) return;
            if ("chunk".equals(event)) {
                try {
                    com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(data);
                    String content = node.path("content").asText("");
                    if (content != null) contentAccumulator.append(content);
                } catch (Exception e) {
                        log.warn("解析骨架行失败: {}", e.getMessage());
                    }
                out.write(("event: chunk\ndata: " + data + "\n\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            } else if ("done".equals(event)) {
                List<SkeletonSlotVO> result = parseAndPersistSkeleton(contentAccumulator.toString());
                writeSkeletonDone(out, result);
            } else {
                out.write(("event: " + event + "\ndata: " + data + "\n\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        }

        private List<SkeletonSlotVO> parseAndPersistSkeleton(String content) {
            List<SkeletonSlotVO> result = new ArrayList<>();
            Map<Long, SkeletonSlotVO> byId = new java.util.HashMap<>();
            for (LiveScript s : scripts) {
                SkeletonSlotVO vo = new SkeletonSlotVO();
                vo.setScriptId(s.getId());
                vo.setScriptType(s.getScriptType());
                vo.setSummary("");
                vo.setSuggestedDurationSec(60);
                byId.put(s.getId(), vo);
                result.add(vo);
            }
            if (content != null && !content.isBlank()) {
                for (String line : content.trim().split("\n")) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 3) {
                        try {
                            Long id = Long.parseLong(parts[0].trim());
                            SkeletonSlotVO vo = byId.get(id);
                            if (vo != null) {
                                vo.setSummary(parts[1].trim());
                                vo.setSuggestedDurationSec(Integer.parseInt(parts[2].trim().replaceAll("\\D", "")));
                                if (vo.getSuggestedDurationSec() <= 0) vo.setSuggestedDurationSec(60);
                            }
                        } catch (Exception e) {
                        log.warn("解析骨架行失败: {}", e.getMessage());
                    }
                    }
                }
            }
            for (SkeletonSlotVO vo : result) {
                if (vo.getScriptId() == null) continue;
                scriptRepository.findById(vo.getScriptId()).ifPresent(script -> {
                    if (vo.getSummary() != null && !vo.getSummary().isBlank()) {
                        String req = vo.getSummary().length() > 128 ? vo.getSummary().substring(0, 128) : vo.getSummary();
                        script.setRequirement(req);
                    }
                    if (vo.getSuggestedDurationSec() != null && vo.getSuggestedDurationSec() > 0) {
                        script.setDurationLimitSec(vo.getSuggestedDurationSec());
                    }
                    scriptRepository.save(script);
                });
            }
            return result;
        }
    }
}
