package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.service.VectorService;
import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * pgvector 实现：向量存 gf_kb_chunk_embedding，嵌入生成委托 Milvus 实现。
 */
public class PgVectorStoreService implements VectorService {

    private static final Logger log = LoggerFactory.getLogger(PgVectorStoreService.class);
    private static final int DIMENSION = 768;

    private final JdbcTemplate jdbcTemplate;
    private final VectorServiceImpl embeddingDelegate;

    public PgVectorStoreService(JdbcTemplate jdbcTemplate, VectorServiceImpl embeddingDelegate) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingDelegate = embeddingDelegate;
    }

    @Override
    public void createCollection(String collectionName, int dimension) {
        log.debug("[pgvector] createCollection {} dim={}", collectionName, dimension);
    }

    @Override
    public void dropCollection(String collectionName) {
        Long kbId = parseKbId(collectionName);
        if (kbId != null) {
            jdbcTemplate.update("delete from gf_kb_chunk_embedding where kb_id = ?", kbId);
        }
    }

    @Override
    public void insertVectors(String collectionName, List<Long> ids, List<List<Float>> vectors, List<Map<String, Object>> metadata) {
        Long kbId = parseKbId(collectionName);
        if (kbId == null || ids == null || vectors == null) {
            return;
        }
        for (int i = 0; i < ids.size(); i++) {
            Long chunkId = ids.get(i);
            List<Float> vec = i < vectors.size() ? vectors.get(i) : List.of();
            Map<String, Object> meta = metadata != null && i < metadata.size() ? metadata.get(i) : Map.of();
            String tenantId = meta.get("tenant_id") != null ? String.valueOf(meta.get("tenant_id")) : null;
            jdbcTemplate.update(
                    """
                            insert into gf_kb_chunk_embedding (chunk_id, kb_id, tenant_id, embedding, metadata)
                            values (?, ?, ?, ?::vector, ?::jsonb)
                            on conflict (chunk_id) do update set
                                embedding = excluded.embedding,
                                metadata = excluded.metadata
                            """,
                    chunkId,
                    kbId,
                    tenantId,
                    toPgVectorLiteral(vec),
                    JSON.toJSONString(meta)
            );
        }
    }

    @Override
    public List<VectorSearchResult> search(String collectionName, List<Float> queryVector, int topK, String filter) {
        Long kbId = parseKbId(collectionName);
        if (kbId == null || queryVector == null || queryVector.isEmpty()) {
            return List.of();
        }
        String vec = toPgVectorLiteral(queryVector);
        return jdbcTemplate.query(
                """
                        select chunk_id, 1 - (embedding <=> ?::vector) as score, metadata
                        from gf_kb_chunk_embedding
                        where kb_id = ?
                        order by embedding <=> ?::vector
                        limit ?
                        """,
                (rs, rowNum) -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> meta = rs.getString("metadata") != null
                            ? JSON.parseObject(rs.getString("metadata"), Map.class)
                            : Map.of();
                    return new VectorSearchResult(rs.getLong("chunk_id"), rs.getFloat("score"), meta);
                },
                vec,
                kbId,
                vec,
                topK
        );
    }

    @Override
    public List<List<VectorSearchResult>> batchSearch(String collectionName, List<List<Float>> queryVectors, int topKPerQuery, String filter) {
        if (queryVectors == null) {
            return List.of();
        }
        return queryVectors.stream()
                .map(q -> search(collectionName, q, topKPerQuery, filter))
                .collect(Collectors.toList());
    }

    @Override
    public void deleteVectors(String collectionName, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(","));
        List<Object> args = new ArrayList<>(ids);
        jdbcTemplate.update("delete from gf_kb_chunk_embedding where chunk_id in (" + placeholders + ")", args.toArray());
    }

    @Override
    public List<Float> generateEmbedding(String text) {
        return embeddingDelegate.generateEmbedding(text);
    }

    @Override
    public List<List<Float>> generateEmbeddings(List<String> texts) {
        return embeddingDelegate.generateEmbeddings(texts);
    }

    private static Long parseKbId(String collectionName) {
        if (collectionName == null) {
            return null;
        }
        String digits = collectionName.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String toPgVectorLiteral(List<Float> vector) {
        if (vector == null || vector.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector.get(i));
        }
        sb.append(']');
        return sb.toString();
    }
}
