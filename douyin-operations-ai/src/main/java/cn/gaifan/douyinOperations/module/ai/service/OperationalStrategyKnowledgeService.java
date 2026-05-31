package cn.gaifan.douyinOperations.module.ai.service;

import java.util.List;
import java.util.Map;

/**
 * 抖音运营智能体的策略知识入口。
 * <p>
 * 这里不生成具体内容，而是把「排品策略、时长策略、官方规则、违规约束」整理成
 * 直播话术、短视频脚本等生成链路可消费的上下文。
 */
public interface OperationalStrategyKnowledgeService {

    void ensureSeeded(Long userId);

    PromptContext buildLiveGenerationContext(Long userId, String query, String materialType, int maxChars);

    PromptContext buildShortVideoGenerationContext(Long userId, String query, int maxChars);

    PromptContext buildViolationRuleContext(Long userId, String query, String scene, int maxChars);

    PromptContext buildViralPatternContext(Long userId, String query, int maxChars);

    void writeViralPatternKnowledge(Long userId, String title, String content, Map<String, String> metadata);

    void writePerformanceReflection(Long userId, String title, String content, Map<String, String> metadata);

    record OfficialReference(
            String kbName,
            String refType,
            Long docId,
            Long chunkId,
            String title,
            String contentPreview,
            double score
    ) {}

    record PromptContext(
            String promptBlock,
            List<KnowledgeBaseService.SearchResult> refs,
            List<OfficialReference> officialReferences
    ) {
        public PromptContext(String promptBlock, List<KnowledgeBaseService.SearchResult> refs) {
            this(promptBlock, refs, List.of());
        }

        public boolean hasText() {
            return promptBlock != null && !promptBlock.isBlank();
        }
    }
}
