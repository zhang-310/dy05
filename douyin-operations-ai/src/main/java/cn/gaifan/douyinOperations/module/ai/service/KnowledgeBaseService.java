package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.module.ai.entity.AiKbDocument;
import cn.gaifan.douyinOperations.module.ai.entity.AiKnowledgeBase;
import cn.gaifan.douyinOperations.module.ai.entity.KbImportReport;
import cn.gaifan.douyinOperations.module.ai.vo.KbDocumentSearchVO;

import java.util.List;

/**
 * 知识库管理服务
 */
public interface KnowledgeBaseService {

    /**
     * 创建知识库
     */
    AiKnowledgeBase createKnowledgeBase(String name, String description, Long userId);

    /**
     * 删除知识库
     */
    void deleteKnowledgeBase(Long kbId, Long userId);

    /**
     * 校验知识库归属当前用户（否则 NOT_FOUND / FORBIDDEN）
     */
    void assertKbOwnership(Long kbId, Long userId);

    /**
     * 获取知识库列表
     */
    List<AiKnowledgeBase> listKnowledgeBases(Long userId);

    /**
     * 上传文档到知识库
     */
    AiKbDocument uploadDocument(Long kbId, String title, String content, String fileType, Long userId);

    /**
     * 上传文档到知识库（指定来源，用于索引队列消费）
     */
    AiKbDocument uploadDocument(Long kbId, String title, String content, String fileType, Long userId, String sourceType);

    /**
     * 上传文档（支持 contentType：general|script|auto，控制分块策略）
     */
    AiKbDocument uploadDocument(Long kbId, String title, String content, String fileType, Long userId, String sourceType, String contentType);

    /**
     * 上传文档（支持目录结构元数据：relativePath、dirCategory、dirSubCategory，用于目录导入）
     */
    AiKbDocument uploadDocument(Long kbId, String title, String content, String fileType, Long userId, String sourceType, String contentType, java.util.Map<String, String> dirMetadata);


    /**
     * 删除文档
     */
    void deleteDocument(Long docId, Long userId);

    /**
     * 获取知识库文档列表
     */
    List<AiKbDocument> listDocuments(Long kbId, Long userId);

    /**
     * 获取知识库文档列表（分页）
     */
    PageResultVO<AiKbDocument> pageDocuments(Long kbId, Long userId, KbDocumentSearchVO vo);

    /**
     * 获取知识库导入报告列表（最近若干条，用于运维查看）
     */
    List<KbImportReport> listImportReports(Long kbId, Long userId);

    /**
     * 导入/上传后清除该知识库检索缓存，使新文档立即可被检索
     */
    void invalidateSearchCache(Long kbId);

    /**
     * 按名称解析知识库 ID（供 RAG 等按名称使用 huashu/douyin）
     */
    Long resolveKbIdByName(Long userId, String kbName);

    /**
     * 混合搜索（向量 + 全文）
     */
    default List<SearchResult> hybridSearch(Long kbId, String query, int topK, Long userId) {
        return hybridSearch(kbId, query, topK, userId, null, false, false);
    }

    /**
     * 混合搜索（支持元数据过滤与跳过缓存，供 RAG 使用）
     */
    default List<SearchResult> hybridSearch(Long kbId, String query, int topK, Long userId, String metadataFilter, boolean skipCache) {
        return hybridSearch(kbId, query, topK, userId, metadataFilter, skipCache, false);
    }

    /**
     * 混合搜索（支持跳过 LLM 查询改写：管理端检索建议 skipQueryRewrite=true，可显著降低延迟）
     */
    List<SearchResult> hybridSearch(Long kbId, String query, int topK, Long userId, String metadataFilter, boolean skipCache, boolean skipQueryRewrite);

    /**
     * 混合搜索（支持额外 ES 过滤器，7 参数重载）
     */
    default List<SearchResult> hybridSearch(Long kbId, String query, int topK, Long userId, String metadataFilter, java.util.Map<String, Object> esFilters, boolean skipCache) {
        return hybridSearch(kbId, query, topK, userId, metadataFilter, skipCache, false);
    }

    /**
     * 跨知识库搜索（RAG 服务用，返回 RagRetrieveItemVO 列表）
     * @param scope 检索范围：null 或 "all" 表示所有知识库
     * @param filter 额外过滤参数（可为 null）
     */
    default List<cn.gaifan.douyinOperations.module.ai.vo.RagRetrieveItemVO> hybridSearchAllKbs(Long userId, String query, int topK, String scope, Object filter) {
        return List.of();
    }

    /**
     * 去重预览（dry-run）：分块后逐条检索，返回 skip/downweight/keep 统计与明细，不入库
     */
    cn.gaifan.douyinOperations.module.ai.vo.DedupPreviewVO dedupPreview(Long kbId, String content, String contentType, Long userId);

    /**
     * 获取文档分块列表（按当前分块策略重算，供前端 chunk 预览）
     */
    List<cn.gaifan.douyinOperations.module.ai.vo.ChunkItemVO> getDocumentChunks(Long kbId, Long docId, Long userId);

    /**
     * 提交知识反馈（有用/无用），并更新 boost_factor
     */
    void submitFeedback(Long docId, String query, int rating, String comment, String searchMode, Long userId);

    /**
     * P0 双写补偿：重试 status=0 的卡住文档
     */
    int retrySyncDocument(AiKbDocument doc);

    /**
     * 搜索结果（RAG 需 chunkId 去重、labels 做 category 展示）
     */
    record SearchResult(
            Long docId,
            String title,
            String content,
            double score,
            String source,  // "vector" | "fulltext" | "hybrid" | "reranked"
            Long chunkId,   // docId * 10000 + chunkIndex，用于 RAG 去重
            List<String> labels,
            String explain  // 可选的检索解释（截断），用于调试
    ) {
        public SearchResult(Long docId, String title, String content, double score, String source) {
            this(docId, title, content, score, source, docId != null ? docId * 10000 : null, List.of(), null);
        }

        public SearchResult(Long docId, String title, String content, double score, String source, Long chunkId, List<String> labels) {
            this(docId, title, content, score, source, chunkId, labels, null);
        }
    }
}
