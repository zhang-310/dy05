package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.entity.AiKbDocument;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiKbDocumentRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.FreshnessCheckService;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 时效性检测 Agent：LLM 判断文档内容是否过期（规则/政策/数据等）
 */
@Service
public class FreshnessCheckServiceImpl implements FreshnessCheckService {

    private static final Logger log = LoggerFactory.getLogger(FreshnessCheckServiceImpl.class);

    @Resource
    private AiKbDocumentRepository documentRepository;

    @Resource
    private AiModelRepository modelRepository;

    @Resource
    private LlmClient llmClient;

    @Override
    public int runFreshnessCheck(Long kbId, int maxDocs) {
        Timestamp cutoff = Timestamp.from(LocalDateTime.now().minusDays(90).atZone(ZoneId.systemDefault()).toInstant());
        List<AiKbDocument> docs = kbId != null
                ? documentRepository.findForFreshnessCheckByKb(kbId, cutoff, PageRequest.of(0, maxDocs, Sort.unsorted()))
                : documentRepository.findForFreshnessCheck(cutoff, PageRequest.of(0, maxDocs, Sort.unsorted()));
        if (docs.isEmpty()) return 0;

        List<AiModel> models = modelRepository.findByStatusAndDeleted(1, 0).stream()
                .filter(m -> "ollama".equalsIgnoreCase(m.getModelProvider()) || "deepseek".equalsIgnoreCase(m.getModelProvider()) || "580ai".equalsIgnoreCase(m.getModelProvider()))
                .limit(3)
                .toList();
        if (models.isEmpty()) {
            log.warn("无可用模型，跳过时效性检测");
            return 0;
        }

        int checked = 0;
        String currentYear = String.valueOf(LocalDateTime.now().getYear());
        for (AiKbDocument doc : docs) {
            try {
                String excerpt = doc.getContent() != null && doc.getContent().length() > 800
                        ? doc.getContent().substring(0, 800) + "..." : (doc.getContent() != null ? doc.getContent() : "");
                String prompt = String.format("当前年份：%s。请判断以下知识内容是否可能已过期（如规则、政策、数据、日期等）。仅回答：VALID 或 EXPIRED，并简要说明理由（1句）。\n\n标题：%s\n内容：%s", currentYear, doc.getTitle(), excerpt);
                LlmClient.LlmResponse resp = llmClient.chatWithFallback(models, "你是知识时效性检测专家。", prompt);
                int status = 1;
                if (resp.success() && resp.content() != null) {
                    String lower = resp.content().toLowerCase();
                    if (lower.contains("expired") || lower.contains("过期")) status = 2;
                }
                doc.setExpiryStatus(status);
                doc.setLastExpiryCheck(new Timestamp(System.currentTimeMillis()));
                documentRepository.save(doc);
                checked++;
            } catch (Exception e) {
                log.warn("时效性检测失败 docId={}: {}", doc.getId(), e.getMessage());
            }
        }
        if (checked > 0) log.info("时效性检测完成，检查 {} 篇文档", checked);
        return checked;
    }
}
