package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.service.*;
import cn.gaifan.douyinOperations.module.ai.vo.HuashuGenerateRequestVO;
import cn.gaifan.douyinOperations.module.ai.vo.HuashuGenerateResponseVO;
import cn.gaifan.douyinOperations.module.live.service.impl.LiveAiModelHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

/**
 * 基于话术知识库（huashu）生成高光话术。
 * 流程：1）可选导入 sourcePath 到 huashu；2）RAG 混合检索；3）LLM 生成。
 */
@Service
public class HuashuGenerateServiceImpl implements HuashuGenerateService {

    private static final Logger log = LoggerFactory.getLogger(HuashuGenerateServiceImpl.class);

    private static final int RAG_TOP_K = 12;
    private static final int RAG_CONTEXT_BUDGET = 6000;
    private static final String DEFAULT_QUERY_SUFFIX = " 高光话术 开场 种草 转化 留人 护肤 彩妆 情感共鸣";

    @Autowired private KnowledgeBaseService knowledgeBaseService;
    @Autowired private KnowledgeBaseImportService knowledgeBaseImportService;
    @Autowired private LlmClient llmClient;
    @Autowired private LiveAiModelHelper modelHelper;
    @Autowired(required = false) private AiPromptConfigService aiPromptConfigService;

    @Value("${app.ai.huashu-generate.rag-enabled:true}")
    private boolean ragEnabled;

    @Value("${app.ai.kb.shared-owner-id:0}")
    private Long sharedKbOwnerId;

    @Value("${app.ai.kb.allow-shared-fallback:false}")
    private boolean allowSharedKbFallback;

    @Override
    public HuashuGenerateResponseVO generate(HuashuGenerateRequestVO vo, Long userId) {
        if (userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        String hostName = vo.getHostName() != null ? vo.getHostName().trim() : "";
        if (hostName.isBlank()) throw new BusinessException(ErrorCode.VALIDATION_FAIL, "hostName 不能为空");

        HuashuGenerateResponseVO response = new HuashuGenerateResponseVO();

        // 1. 可选：导入 sourcePath 到 huashu
        if (vo.getSourcePath() != null && !vo.getSourcePath().isBlank()) {
            Long importKbId = knowledgeBaseService.resolveKbIdByName(userId, "huashu");
            if (importKbId != null) {
                try {
                    var ir = knowledgeBaseImportService.importFromPath(
                            vo.getSourcePath().trim(), null, "huashu", false, userId, null);
                    var summary = new HuashuGenerateResponseVO.ImportSummary();
                    summary.setTotal(ir.total());
                    summary.setSuccess(ir.success());
                    summary.setFailed(ir.failed());
                    response.setImportSummary(summary);
                    log.info("高光话术生成前导入完成: path={}, success={}, failed={}", vo.getSourcePath(), ir.success(), ir.failed());
                } catch (Exception e) {
                    log.warn("导入参考素材失败，继续使用已有知识库: {}", e.getMessage());
                }
            } else {
                log.warn("用户无 huashu 知识库，跳过导入");
            }
        }

        // 2. 解析 huashu kbId（当前用户优先，必要时显式使用共享知识库）
        Long kbId = knowledgeBaseService.resolveKbIdByName(userId, "huashu");
        Long searchUserId = userId;
        if (kbId == null) {
            Long sharedOwnerId = resolveSharedKnowledgeOwnerId(userId);
            if (sharedOwnerId != null) {
                kbId = knowledgeBaseService.resolveKbIdByName(sharedOwnerId, "huashu");
                if (kbId != null) {
                    searchUserId = sharedOwnerId;
                }
            }
        }
        if (kbId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "未找到话术知识库 huashu，请先创建或导入话术素材");
        }

        // 3. RAG 检索
        String query = vo.getQuery() != null && !vo.getQuery().isBlank()
                ? vo.getQuery().trim()
                : hostName + DEFAULT_QUERY_SUFFIX;
        List<KnowledgeBaseService.SearchResult> ragRefs = List.of();
        String ragXml = "";
        if (ragEnabled) {
            Map<String, Object> esFilters = new HashMap<>(Map.of("source_type", List.of("live_script", "manual", "evolved_script")));
            String milvusFilter = "(metadata[\"source_type\"] == \"live_script\") || (metadata[\"source_type\"] == \"manual\") || (metadata[\"source_type\"] == \"evolved_script\")";
            try {
                List<KnowledgeBaseService.SearchResult> raw = knowledgeBaseService.hybridSearch(
                        kbId, query, RAG_TOP_K + 5, searchUserId, milvusFilter, true);
                if (raw != null && !raw.isEmpty()) {
                    ragRefs = raw.stream()
                            .filter(r -> r.score() >= 0.3)
                            .sorted(Comparator.comparingDouble(KnowledgeBaseService.SearchResult::score).reversed())
                            .limit(RAG_TOP_K)
                            .toList();
                }
            } catch (Exception e) {
                log.warn("RAG 检索异常，将无参考生成: {}", e.getMessage());
            }
            if (!ragRefs.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("<reference_scripts>\n<note>仅参考风格和技巧，禁止照搬内容</note>\n");
                int totalLen = 0;
                for (int i = 0; i < ragRefs.size(); i++) {
                    KnowledgeBaseService.SearchResult r = ragRefs.get(i);
                    if (totalLen + r.content().length() > RAG_CONTEXT_BUDGET) break;
                    sb.append(String.format("<script id=\"%d\" score=\"%.2f\">", i + 1, r.score()));
                    sb.append(r.content());
                    sb.append("</script>\n");
                    totalLen += r.content().length();
                }
                sb.append("</reference_scripts>");
                ragXml = sb.toString();
            }
        }

        response.setRagRefCount(ragRefs.size());
        response.setRagRefTitles(ragRefs.stream().map(KnowledgeBaseService.SearchResult::title).filter(Objects::nonNull).distinct().limit(10).toList());

        // 4. 构建 prompt 并调用 LLM
        String systemPrompt = getSystemPrompt();
        String userPrompt = buildUserPrompt(hostName, ragXml);
        AiModel model = modelHelper.findAvailableModel();
        if (model == null) {
            throw new BusinessException(ErrorCode.AI_MODEL_UNAVAILABLE, "无可用 AI 模型，请配置 copy_processing 或启用模型");
        }
        try {
            LlmClient.LlmResponse resp = llmClient.chat(model, systemPrompt, userPrompt);
            if (resp.success() && resp.content() != null && !resp.content().isBlank()) {
                if (resp.tokensUsed() > 0) {
                    modelHelper.incrementQuotaUsed(model.getId(), resp.tokensUsed());
                }
                response.setContent(resp.content().trim());
                log.info("高光话术生成成功: hostName={}, model={}, ragRefs={}", hostName, model.getModelVersion(), ragRefs.size());
            } else {
                response.setContent(buildFallbackContent(hostName));
                log.warn("LLM 生成失败，使用模板: {}", resp != null ? resp.errorMsg() : "null");
            }
        } catch (Exception e) {
            log.error("LLM 调用异常", e);
            response.setContent(buildFallbackContent(hostName));
        }

        return response;
    }

    private Long resolveSharedKnowledgeOwnerId(Long preferredUserId) {
        if (!allowSharedKbFallback || sharedKbOwnerId == null || sharedKbOwnerId < 0) {
            return null;
        }
        if (sharedKbOwnerId.equals(preferredUserId)) {
            return preferredUserId;
        }
        return sharedKbOwnerId;
    }

    private String getSystemPrompt() {
        if (aiPromptConfigService != null) {
            return aiPromptConfigService.getPrompt("ai.prompt.evolve.huashu.system",
                    "你是一位资深直播话术教练，擅长提炼可直接使用的直播话术。输出的每条话术都应是主播能直接照着念的完整句子，不要输出方法论或分析，只要实战话术。");
        }
        return "你是一位资深直播话术教练，擅长提炼可直接使用的直播话术。输出的每条话术都应是主播能直接照着念的完整句子，不要输出方法论或分析，只要实战话术。";
    }

    private String buildUserPrompt(String hostName, String ragXml) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为主播「").append(hostName).append("」生成高光话术，包含以下结构：\n\n");
        sb.append("1. 【开场话术】—— 拉新、亮灯牌、建立信任\n");
        sb.append("2. 【种草话术】—— 按产品类型（护肤/彩妆/香水等）分功能-优势-利益-证据\n");
        sb.append("3. 【转化话术】—— 促单、限时、稀缺感\n");
        sb.append("4. 【留人话术】—— 预告、个人故事、情感共鸣\n\n");
        sb.append("要求：风格符合情感共鸣、女性成长、克制美学；每条话术可直接使用。\n\n");
        if (ragXml != null && !ragXml.isBlank()) {
            sb.append(ragXml).append("\n\n请参考以上案例的表达方式和技巧，为「").append(hostName).append("」生成原创高光话术。\n");
        }
        return sb.toString();
    }

    private String buildFallbackContent(String hostName) {
        return "# " + hostName + " 高光话术（模板）\n\n" +
                "## 【开场话术】\n\n直播间新来的姐妹，先点个关注亮灯牌。今天给大家带来一份新人礼物……\n\n" +
                "## 【种草话术】\n\n（请根据实际产品补充功能-优势-利益-证据）\n\n" +
                "## 【转化话术】\n\n（限时促单、稀缺感）\n\n" +
                "## 【留人话术】\n\n（预告、个人故事、情感共鸣）";
    }
}
