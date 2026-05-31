package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.service.SearchService;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchServiceImpl.class);

    @Resource
    private ElasticsearchClient esClient;

    /** IK 中文分词器，需安装 elasticsearch-analysis-ik 插件；未安装时使用 standard */
    @Value("${app.ai.es.use-ik:false}")
    private boolean useIkAnalyzer;

    @Override
    public void createIndex(String indexName) {
        try {
            String textAnalyzer = useIkAnalyzer ? "ik_max_word" : "standard";
            CreateIndexRequest request = CreateIndexRequest.of(b -> b
                    .index(indexName)
                    .settings(s -> s
                            .numberOfShards("1")
                            .numberOfReplicas("1")
                    )
                    .mappings(m -> m
                            .properties("text", p -> p.text(t -> t.analyzer(textAnalyzer)))
                            .properties("title", p -> p.text(t -> t.analyzer(textAnalyzer)))
                            .properties("content", p -> p.text(t -> t.analyzer(textAnalyzer)))
                            .properties("kb_id", p -> p.long_(l -> l))
                            .properties("doc_id", p -> p.long_(l -> l))
                            .properties("chunk_index", p -> p.integer(i -> i))
                            .properties("created_at", p -> p.date(d -> d))
                    )
            );

            esClient.indices().create(request);
            log.info("索引 {} 创建成功", indexName);
        } catch (Exception e) {
            log.error("创建索引失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建索引失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteIndex(String indexName) {
        try {
            DeleteIndexRequest request = DeleteIndexRequest.of(b -> b.index(indexName));
            esClient.indices().delete(request);
            log.info("索引 {} 删除成功", indexName);
        } catch (Exception e) {
            log.error("删除索引失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "删除索引失败: " + e.getMessage());
        }
    }

    @Override
    public void indexDocument(String indexName, String id, Map<String, Object> document) {
        try {
            IndexRequest<Map<String, Object>> request = IndexRequest.of(b -> b
                    .index(indexName)
                    .id(id)
                    .document(document)
            );

            esClient.index(request);
            log.debug("文档 {} 索引成功", id);
        } catch (Exception e) {
            log.error("索引文档失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "索引文档失败: " + e.getMessage());
        }
    }

    private static final int WRITE_MAX_RETRIES = 3;
    private static final int[] WRITE_BACKOFF_MS = {1000, 2000, 4000};

    @Override
    public void bulkIndexDocuments(String indexName, List<DocumentWithId> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        Exception lastEx = null;
        for (int attempt = 0; attempt < WRITE_MAX_RETRIES; attempt++) {
            try {
                doBulkIndexDocuments(indexName, documents);
                return;
            } catch (Exception e) {
                lastEx = e;
                if (attempt < WRITE_MAX_RETRIES - 1) {
                    int backoff = WRITE_BACKOFF_MS[Math.min(attempt, WRITE_BACKOFF_MS.length - 1)];
                    log.warn("ES 批量索引失败(attempt={}), {}ms 后重试: {}", attempt + 1, backoff, e.getMessage());
                    try { Thread.sleep(backoff); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
        log.error("ES 批量索引失败: {}", lastEx != null ? lastEx.getMessage() : "unknown");
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "批量索引文档失败: " + (lastEx != null ? lastEx.getMessage() : "unknown"));
    }

    private void doBulkIndexDocuments(String indexName, List<DocumentWithId> documents) throws Exception {
        List<BulkOperation> operations = documents.stream()
                .map(doc -> BulkOperation.of(b -> b
                        .index(i -> i
                                .index(indexName)
                                .id(doc.id())
                                .document(doc.document())
                        )
                ))
                .collect(Collectors.toList());

        BulkRequest request = BulkRequest.of(b -> b.operations(operations));
        BulkResponse response = esClient.bulk(request);

        if (response.errors()) {
            log.warn("批量索引部分失败");
        }
        log.info("批量索引 {} 条文档", documents.size());
    }

    @SuppressWarnings({ "null", "unchecked" })
    @Override
    public List<SearchResult> search(String indexName, String query, int from, int size, Map<String, Object> filters) {
        try {
            BoolQuery.Builder boolQuery = new BoolQuery.Builder();

            // 全文搜索
            if (query != null && !query.isEmpty()) {
                boolQuery.must(Query.of(q -> q
                        .multiMatch(m -> m
                                .query(query)
                                .fields("text", "title", "content")
                        )
                ));
            }

            // 过滤条件
            if (filters != null && !filters.isEmpty()) {
                filters.forEach((field, value) -> {
                    boolQuery.filter(Query.of(q -> q
                            .term(t -> t
                                    .field(field)
                                    .value(v -> v.stringValue(value.toString()))
                            )
                    ));
                });
            }

            SearchRequest request = SearchRequest.of(s -> s
                    .index(indexName)
                    .query(q -> q.bool(boolQuery.build()))
                    .from(from)
                    .size(size)
            );

            @SuppressWarnings("rawtypes")
            SearchResponse<Map> response = esClient.search(request, Map.class);

            return response.hits().hits().stream()
                    .map(hit -> new SearchResult(
                            hit.id(),
                            hit.score() != null ? hit.score() : 0.0,
                            hit.source()
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("搜索失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "搜索失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteDocument(String indexName, String id) {
        try {
            DeleteRequest request = DeleteRequest.of(b -> b
                    .index(indexName)
                    .id(id)
            );

            esClient.delete(request);
            log.debug("文档 {} 删除成功", id);
        } catch (Exception e) {
            log.error("删除文档失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "删除文档失败: " + e.getMessage());
        }
    }

    @Override
    public void bulkDeleteDocuments(String indexName, List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        try {
            List<BulkOperation> operations = ids.stream()
                    .map(id -> BulkOperation.of(b -> b
                            .delete(d -> d
                                    .index(indexName)
                                    .id(id)
                            )
                    ))
                    .collect(Collectors.toList());

            BulkRequest request = BulkRequest.of(b -> b
                    .operations(operations)
            );

            esClient.bulk(request);
            log.info("批量删除 {} 条文档", ids.size());
        } catch (Exception e) {
            log.error("批量删除文档失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "批量删除文档失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Map<String, Object> aggregate(String indexName, String field, String aggType) {
        try {
            SearchRequest request = SearchRequest.of(s -> s
                    .index(indexName)
                    .size(0)
                    .aggregations("agg_result", a -> a
                            .terms(t -> t.field(field))
                    )
            );

            SearchResponse<Map> response = esClient.search(request, Map.class);

            Map<String, Object> result = new HashMap<>();
            StringTermsAggregate termsAgg = response.aggregations()
                    .get("agg_result")
                    .sterms();

            List<Map<String, Object>> buckets = termsAgg.buckets().array().stream()
                    .map(bucket -> {
                        Map<String, Object> bucketMap = new HashMap<>();
                        bucketMap.put("key", bucket.key().stringValue());
                        bucketMap.put("count", bucket.docCount());
                        return bucketMap;
                    })
                    .collect(Collectors.toList());

            result.put("buckets", buckets);
            return result;
        } catch (Exception e) {
            log.error("聚合分析失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "聚合分析失败: " + e.getMessage());
        }
    }
}
