package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.entity.AiTaskModelConfig;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiTaskModelConfigRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.douyin.entity.DyPersona;
import cn.gaifan.douyinOperations.module.douyin.repository.DyPersonaRepository;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvViralVideo;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvViralVideoRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoAiService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class ShortVideoAiServiceImpl implements ShortVideoAiService {

    private static final Logger log = LoggerFactory.getLogger(ShortVideoAiServiceImpl.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    @Resource
    private LlmClient llmClient;

    @Resource
    private AiTaskModelConfigRepository taskModelConfigRepository;

    @Resource
    private AiModelRepository modelRepository;

    @Resource
    private SvViralVideoRepository viralVideoRepository;

    @Resource
    private DyPersonaRepository personaRepository;

    /** 任务编码：短视频脚本/文案/分镜/标题等 AI 生成 */
    private static final String TASK_CODE = "short_video_script";

    @Override
    public String generateCopy(AiCopyGenerateVO vo, Long userId) {
        return generateCopy(vo, userId, TASK_CODE);
    }

    @Override
    public String generateCopy(AiCopyGenerateVO vo, Long userId, String taskCode) {
        // 构建上下文
        StringBuilder context = new StringBuilder();

        // 1. 人设上下文
        if (vo.getPersonaId() != null) {
            DyPersona persona = personaRepository.findById(vo.getPersonaId()).orElse(null);
            if (persona != null) {
                context.append("人设信息：\n");
                context.append("- 名称：").append(persona.getPersonaName()).append("\n");
                context.append("- 类型：").append(persona.getPersonaType()).append("\n");
                context.append("- 语气：").append(persona.getTone()).append("\n");
                context.append("- 描述：").append(persona.getDescription()).append("\n\n");
            }
        }

        // 2. 爆款参考
        if (vo.getViralId() != null) {
            SvViralVideo viral = viralVideoRepository.findById(vo.getViralId()).orElse(null);
            if (viral != null) {
                context.append(buildViralReferenceContext(viral));
            }
        }

        // 3. 构建 Prompt
        String lengthDesc = getLengthDesc(vo.getLength());
        String prompt = String.format("""
                请生成一段短视频文案。

                %s
                主题：%s
                风格：%s
                长度：%s
                关键词：%s

                要求：
                1. 符合人设和风格
                2. 吸引眼球，引发共鸣
                3. 适合短视频场景
                4. 包含关键词
                5. 长度控制在%s

                请直接输出文案内容，不要额外说明。
                """,
                context.toString(),
                vo.getTopic() != null ? vo.getTopic() : "自由发挥",
                vo.getStyle() != null ? vo.getStyle() : "轻松随意",
                lengthDesc,
                vo.getKeywords() != null ? vo.getKeywords() : "无",
                lengthDesc
        );

        String effectiveTask = taskCode != null ? taskCode : TASK_CODE;
        return callLlm("你是专业的短视频文案创作者", prompt, effectiveTask);
    }

    @Override
    public String generateScript(AiScriptGenerateVO vo, Long userId) {
        // 构建上下文
        StringBuilder context = new StringBuilder();

        // 1. 人设上下文
        if (vo.getPersonaId() != null) {
            DyPersona persona = personaRepository.findById(vo.getPersonaId()).orElse(null);
            if (persona != null) {
                context.append("人设：").append(persona.getPersonaName()).append("\n");
                context.append("风格：").append(persona.getTone()).append("\n\n");
            }
        }

        // 2. 爆款参考
        if (vo.getViralId() != null) {
            SvViralVideo viral = viralVideoRepository.findById(vo.getViralId()).orElse(null);
            if (viral != null) {
                context.append(buildViralReferenceContext(viral));
            }
        }

        // 3. 构建 Prompt
        String prompt = String.format("""
                请基于以下文案，生成详细的短视频拍摄脚本。

                %s
                文案内容：
                %s

                场景类型：%s
                视频时长：%d秒

                请按以下格式输出脚本：

                ## 镜头1（0-5秒）
                - 画面：描述画面内容
                - 文案：对应的文案
                - 动作：演员动作
                - 音效：背景音效

                ## 镜头2（5-10秒）
                ...

                要求：
                1. 镜头切换流畅
                2. 画面与文案配合
                3. 符合场景类型
                4. 控制在指定时长
                """,
                context.toString(),
                vo.getCopyText(),
                vo.getSceneType() != null ? vo.getSceneType() : "室内",
                vo.getDuration() != null ? vo.getDuration() : 60
        );

        return callLlm("你是专业的短视频导演", prompt);
    }

    @Override
    public List<String> generateTitles(AiTitleGenerateVO vo, Long userId) {
        String styleDesc = getStyleDesc(vo.getStyle());
        int count = vo.getCount() != null ? vo.getCount() : 5;

        String prompt = String.format("""
                请基于以下文案内容，生成%d个短视频标题。

                文案内容：
                %s

                标题风格：%s

                要求：
                1. 每个标题独立一行
                2. 标题长度15-30字
                3. 吸引眼球，引发点击
                4. 符合指定风格
                5. 不要编号，直接输出标题

                请直接输出%d个标题，每行一个。
                """,
                count,
                vo.getCopyText(),
                styleDesc,
                count
        );

        String result = callLlm("你是专业的短视频标题创作者", prompt);

        // 解析标题列表
        return Arrays.stream(result.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .limit(count)
                .toList();
    }

    @Override
    public String generateVideoPlan(AiVideoPlanGenerateVO vo, Long userId) {
        // 构建上下文
        StringBuilder context = new StringBuilder();

        if (vo.getViralId() != null) {
            SvViralVideo viral = viralVideoRepository.findById(vo.getViralId()).orElse(null);
            if (viral != null && viral.getAnalysisResult() != null) {
                context.append("参考爆款分析：\n").append(viral.getAnalysisResult()).append("\n\n");
            }
        }

        String prompt = String.format("""
                请基于以下内容，生成完整的短视频制作方案。

                %s
                文案：
                %s

                脚本：
                %s

                拍摄风格：%s

                请生成以下内容：

                ## 1. 创意概述
                - 核心创意点
                - 目标受众
                - 预期效果

                ## 2. 拍摄方案
                - 场景选择
                - 道具准备
                - 服装造型
                - 灯光布置

                ## 3. 后期制作
                - 剪辑节奏
                - 特效建议
                - 配乐选择
                - 字幕样式

                ## 4. 发布建议
                - 最佳发布时间
                - 标签推荐
                - 互动策略

                ## 5. 注意事项
                - 拍摄要点
                - 常见问题
                - 优化建议
                """,
                context.toString(),
                vo.getCopyText() != null ? vo.getCopyText() : "无",
                vo.getScriptText() != null ? vo.getScriptText() : "无",
                vo.getShootingStyle() != null ? vo.getShootingStyle() : "vlog"
        );

        return callLlm("你是专业的短视频制作顾问", prompt, TASK_CODE);
    }

    // ─── 工具方法 ──────────────────────────────────────

    private String callLlm(String system, String prompt) {
        return callLlm(system, prompt, TASK_CODE);
    }

    private String callLlm(String system, String prompt, String taskCode) {
        try {
            String effectiveTask = taskCode != null ? taskCode : TASK_CODE;
            List<AiModel> models = resolveModels(effectiveTask);
            if (!models.isEmpty()) {
                log.info("AI任务[{}]使用模型: {}", effectiveTask, models.stream().map(m -> m.getModelName() + "(" + m.getId() + ")").toList());
            }
            if (models.isEmpty()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                        "请先在「AI 模型配置」中配置至少一个可用模型（如 DeepSeek/Ollama）");
            }
            LlmClient.LlmResponse response = llmClient.chatWithFallback(models, system, prompt);
            if (response.success()) {
                return response.content();
            } else {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI生成失败: " + response.errorMsg());
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI生成失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI生成失败");
        }
    }

    /** 解析任务模型：优先 ai_task_model_config 对应任务，无配置时回退到任意可用模型 */
    private List<AiModel> resolveModels(String taskCode) {
        var config = taskModelConfigRepository.findByTaskCodeAndStatusAndDeleted(taskCode, 1, 0);
        if (config.isPresent()) {
            AiTaskModelConfig tc = config.get();
            List<AiModel> result = new ArrayList<>();
            for (Long modelId : Arrays.asList(tc.getPrimaryModelId(), tc.getFallbackModelId(), tc.getFallback2ModelId())) {
                if (modelId == null) continue;
                modelRepository.findById(modelId).filter(m -> m.getStatus() == 1 && m.getDeleted() == 0)
                        .ifPresent(result::add);
            }
            if (!result.isEmpty()) return result;
        }
        return modelRepository.findByStatusAndDeleted(1, 0).stream().limit(3).toList();
    }

    private String getLengthDesc(Integer length) {
        if (length == null) return "100字左右";
        return switch (length) {
            case 1 -> "50字以内";
            case 2 -> "100字左右";
            case 3 -> "200字左右";
            default -> "100字左右";
        };
    }

    private String getStyleDesc(String style) {
        if (style == null) return "吸引眼球";
        return switch (style) {
            case "clickbait" -> "标题党风格，吸引眼球";
            case "professional" -> "专业严谨风格";
            case "creative" -> "创意新颖风格";
            default -> "吸引眼球";
        };
    }

    private String buildViralReferenceContext(SvViralVideo viral) {
        StringBuilder context = new StringBuilder();
        context.append("参考爆款：\n");
        appendContextLine(context, "标题", viral.getTitle());
        appendContextLine(context, "作者", viral.getAuthorName());
        appendContextLine(context, "播放量", stringifyNumber(viral.getViewCount()));
        appendContextLine(context, "点赞数", stringifyNumber(viral.getLikeCount()));
        appendContextLine(context, "评论数", stringifyNumber(viral.getCommentCount()));
        appendContextLine(context, "分享数", stringifyNumber(viral.getShareCount()));

        boolean appendedStructured = appendStructuredDeepAnalysis(context, viral.getDeepAnalysisResult());

        if (!appendedStructured) {
            appendFallbackTranscriptContext(context, viral.getTranscript());
            appendFallbackSceneContext(context, viral.getSceneDescriptions());
            appendContextLine(context, "二创变量表", truncate(viral.getRemakeVariableTable(), 300));
        }

        if (viral.getAnalysisResult() != null && !viral.getAnalysisResult().isBlank()) {
            appendContextLine(context, "AI分析摘要", truncate(viral.getAnalysisResult(), 500));
        }
        context.append("\n");
        return context.toString();
    }

    private boolean appendStructuredDeepAnalysis(StringBuilder context, String deepAnalysisResult) {
        if (deepAnalysisResult == null || deepAnalysisResult.isBlank()) {
            return false;
        }
        try {
            JsonNode root = JSON.readTree(deepAnalysisResult);
            appendContextLine(context, "证据等级", root.path("evidenceLevel").asText(""));
            appendContextLine(context, "最佳二创方式", root.path("bestRemakeType").asText(""));

            JsonNode transcript = root.path("transcript").path("fullText");
            appendContextLine(context, "实证/推演口播稿", truncate(transcript.asText(""), 300));

            JsonNode hypotheses = root.path("viralHypotheses");
            if (hypotheses.isObject()) {
                appendContextLine(context, "情绪触发", hypotheses.path("emotionTrigger").asText(""));
                appendContextLine(context, "节奏模式", hypotheses.path("rhythmPattern").asText(""));
                appendContextLine(context, "可复制性", hypotheses.path("replicability").asText(""));
            }

            JsonNode variableTable = root.path("remakeVariableTable");
            if (variableTable.isObject()) {
                List<String> mustKeep = new ArrayList<>();
                for (JsonNode item : variableTable.path("mustKeep")) {
                    String text = item.asText("").trim();
                    if (!text.isEmpty()) {
                        mustKeep.add(text);
                    }
                }
                if (!mustKeep.isEmpty()) {
                    appendContextLine(context, "必须保留元素", String.join("；", mustKeep));
                }

                List<String> replaceable = new ArrayList<>();
                for (JsonNode item : variableTable.path("replaceable")) {
                    String variable = item.path("variable").asText("").trim();
                    String suggestion = item.path("suggestion").asText("").trim();
                    if (!variable.isEmpty() && !suggestion.isEmpty()) {
                        replaceable.add(variable + "→" + suggestion);
                    }
                }
                if (!replaceable.isEmpty()) {
                    appendContextLine(context, "可替换变量", String.join("；", replaceable));
                }
            }
            return true;
        } catch (Exception e) {
            log.debug("解析 deepAnalysisResult 失败，降级为原始文本: {}", e.getMessage());
            return false;
        }
    }

    private void appendContextLine(StringBuilder context, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        context.append("- ").append(label).append("：").append(value.trim()).append("\n");
    }

    private void appendFallbackTranscriptContext(StringBuilder context, String transcript) {
        if (!StringUtils.hasText(transcript)) {
            return;
        }
        String label = transcript.startsWith("【推演口播】")
                ? "推演口播稿（非 ASR 实录）"
                : "转写文案";
        appendContextLine(context, label, truncate(transcript, 300));
    }

    private void appendFallbackSceneContext(StringBuilder context, String sceneDescriptions) {
        if (!StringUtils.hasText(sceneDescriptions)) {
            return;
        }
        String label = sceneDescriptions.startsWith("【推演场景】")
                ? "推演场景摘要（非真实抽帧）"
                : "场景摘要";
        appendContextLine(context, label, truncate(sceneDescriptions, 300));
    }

    private String stringifyNumber(Number value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }
}
