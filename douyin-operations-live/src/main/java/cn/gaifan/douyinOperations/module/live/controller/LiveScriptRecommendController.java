package cn.gaifan.douyinOperations.module.live.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.entity.AiPromptTemplate;
import cn.gaifan.douyinOperations.module.ai.service.PromptTemplateService;
import cn.gaifan.douyinOperations.module.live.service.LiveScriptRecommendService;
import cn.gaifan.douyinOperations.module.live.vo.ScriptRecommendRequestVO;
import cn.gaifan.douyinOperations.module.live.vo.ScriptRecommendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 直播话术推荐与策略 Controller（话术推荐、2小时聊天策略）
 */
@RestController
@RequestMapping("/api/v1/live/ai")
@Tag(name = "直播话术推荐 / Script Recommendation", description = "多场景话术智能推荐与策略建议（需登录）")
public class LiveScriptRecommendController {

    /** 2小时聊天策略的模板编码（在 ai_prompt_template 表中维护可覆盖配置） */
    private static final String CHAT_2H_TEMPLATE_CODE = "live_chat_2h_strategy";

    @Autowired(required = false)
    private LiveScriptRecommendService liveScriptRecommendService;

    @Autowired(required = false)
    private PromptTemplateService promptTemplateService;

    /**
     * 多场景话术智能推荐。
     * 基于当前商品/时段/在线人数，结合历史效果分进行加权推荐。
     * 注意：当前为规则加权实现，未接入向量语义召回。
     */
    @PostMapping("/recommend-scripts")
    @Operation(summary = "多场景话术推荐（规则加权）",
            description = "根据当前商品/时段/在线人数推荐历史高效话术，Top-N 返回。" +
                    "当前基于效果分+时段匹配的规则加权，非向量语义推荐。")
    public RESTResult<List<ScriptRecommendVO>> recommendScripts(HttpServletRequest request,
                                                                @RequestBody ScriptRecommendRequestVO vo) {
        Long userId = requireUserId(request);
        if (liveScriptRecommendService == null) {
            return withTraceId(RESTResult.error(ErrorCode.SYSTEM_BUSY, "推荐服务暂不可用"));
        }
        List<ScriptRecommendVO> result = liveScriptRecommendService.recommend(vo, userId);
        return withTraceId(RESTResult.getSuccess(result));
    }

    /**
     * 2小时聊天式拉自然流策略配置。
     * <p>
     * 优先读取 {@code ai_prompt_template} 表中 {@code templateCode=live_chat_2h_strategy} 的记录，
     * 若存在则将 templateContent 作为 strategyNote 字段返回，供前端/运营覆盖内置策略。
     * 若无模板记录，则降级到内置预设策略（向后兼容）。
     * <p>
     * 运营维护：通过 /api/v1/ai/prompt-template/save 创建或更新 templateCode=live_chat_2h_strategy 的模板即可覆盖。
     */
    @PostMapping("/chat-2h-strategy")
    @Operation(summary = "2小时聊天式直播预设策略",
            description = "按 chat_2h 格式输出分时段话术结构、节奏建议与槽位配置。" +
                    "优先读取 ai_prompt_template 表（templateCode=live_chat_2h_strategy），" +
                    "不存在时降级到内置预设策略。")
    public RESTResult<Map<String, Object>> chat2hStrategy(HttpServletRequest request,
                                                          @RequestBody(required = false) Map<String, Object> body) {
        Long userId = requireUserId(request);
        String personaCode = body != null && body.get("personaCode") instanceof String s ? s : "local_flavor";
        int productCount = body != null && body.get("productCount") instanceof Number n ? n.intValue() : 3;

        Map<String, Object> strategy = buildPresetStrategy(personaCode, productCount);

        // 尝试读取运营配置的模板（可按人设区分变体）
        if (promptTemplateService != null) {
            try {
                AiPromptTemplate tpl = promptTemplateService.getActiveTemplate(
                        CHAT_2H_TEMPLATE_CODE, personaCode, userId);
                if (tpl == null) {
                    tpl = promptTemplateService.getActiveTemplate(CHAT_2H_TEMPLATE_CODE, null, userId);
                }
                if (tpl != null && tpl.getTemplateContent() != null && !tpl.getTemplateContent().isBlank()) {
                    strategy.put("strategyType", "template");
                    strategy.put("templateId", tpl.getId());
                    strategy.put("strategyNote", tpl.getTemplateContent());
                    promptTemplateService.incrementUsage(tpl.getId());
                }
            } catch (Exception ignored) {
                // 模板读取失败不影响预设策略返回
            }
        }

        return withTraceId(RESTResult.getSuccess(strategy));
    }

    private Map<String, Object> buildPresetStrategy(String personaCode, int productCount) {
        Map<String, Object> strategy = new java.util.LinkedHashMap<>();
        strategy.put("liveFormat", "chat_2h");
        strategy.put("totalDurationMin", 120);
        strategy.put("recommendedPersona", personaCode);
        strategy.put("strategyType", "preset");

        List<Map<String, Object>> phases = new ArrayList<>();
        phases.add(java.util.Map.of(
                "phase", 1, "range", "0-30min", "theme", "暖场聊天建立信任",
                "focus", "local_flavor 东北/方言闲聊，无压力互动，不急于推品",
                "scriptTypes", List.of("opening", "interaction"),
                "productSlots", 0
        ));
        phases.add(java.util.Map.of(
                "phase", 2, "range", "30-60min", "theme", "软性种草引流",
                "focus", "自然引出1-2款体验款，强调真实用感，引导低门槛转化",
                "scriptTypes", List.of("product", "interaction"),
                "productSlots", Math.min(2, productCount)
        ));
        phases.add(java.util.Map.of(
                "phase", 3, "range", "60-90min", "theme", "深度讲解利润品",
                "focus", "选1-2款利润品深度讲，四段式：成分→功效→对比→下单理由",
                "scriptTypes", List.of("product", "closing"),
                "productSlots", Math.min(2, productCount)
        ));
        phases.add(java.util.Map.of(
                "phase", 4, "range", "90-120min", "theme", "扫尾与福袋留存",
                "focus", "福袋或抽奖留人，再次强调爆品，自然收尾",
                "scriptTypes", List.of("interaction", "closing"),
                "productSlots", 1
        ));
        strategy.put("phases", phases);

        strategy.put("slotConfig", java.util.Map.of(
                "openingSlots", 2,
                "productSlots", productCount,
                "interactionSlots", 4,
                "closingSlots", 1,
                "rotationMode", "fixed"
        ));

        strategy.put("notes", List.of(
                "聊天式重点：弱化「买」字，多用「用感分享」替代「推品话术」",
                "东北 local_flavor 风格：方言词汇（老铁、实在、整）+ 真实生活场景",
                "自然流关键：前30分钟无销售压力，让观众因「好玩」留下而非「促销」",
                "插播评论：每10分钟互动一次，读评论+回应 增加真实感",
                "运营可通过 ai_prompt_template（templateCode=live_chat_2h_strategy）覆盖此策略"
        ));
        return strategy;
    }

    private Long requireUserId(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        return userId;
    }

    private <T> RESTResult<T> withTraceId(RESTResult<T> r) {
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
