package cn.gaifan.douyinOperations.module.script.repository;

import cn.gaifan.douyinOperations.module.script.entity.ScriptVectorEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 向量嵌入 Repository
 */
@Repository
public interface ScriptVectorEmbeddingRepository extends JpaRepository<ScriptVectorEmbedding, Long>,
        JpaSpecificationExecutor<ScriptVectorEmbedding> {

    Optional<ScriptVectorEmbedding> findByIdAndDeleted(Long id, Integer deleted);

    Optional<ScriptVectorEmbedding> findByScriptIdAndDeleted(Long scriptId, Integer deleted);

    List<ScriptVectorEmbedding> findByOwnerIdAndDeletedOrderByCreatedAtDesc(Long ownerId, Integer deleted);

    @Query(value = "SELECT * FROM sc_script_vector_embedding WHERE is_indexed_milvus = false AND deleted = 0 ORDER BY created_at ASC LIMIT :limit",
            nativeQuery = true)
    List<ScriptVectorEmbedding> findUnindexedEmbeddings(@Param("limit") int limit);

    @Query(value = "SELECT COUNT(*) FROM sc_script_vector_embedding WHERE owner_id = :ownerId AND deleted = 0",
            nativeQuery = true)
    long countByOwnerId(@Param("ownerId") Long ownerId);

    @Query(value = "SELECT COUNT(*) FROM sc_script_vector_embedding WHERE is_indexed_milvus = true AND deleted = 0",
            nativeQuery = true)
    long countIndexedEmbeddings();

    void deleteByScriptIdAndDeleted(Long scriptId, Integer deleted);
}
