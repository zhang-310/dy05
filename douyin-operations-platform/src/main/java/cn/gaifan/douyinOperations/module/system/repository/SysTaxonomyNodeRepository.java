package cn.gaifan.douyinOperations.module.system.repository;

import cn.gaifan.douyinOperations.module.system.entity.SysTaxonomyNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SysTaxonomyNodeRepository extends JpaRepository<SysTaxonomyNode, Long> {

    @Query("SELECT n FROM SysTaxonomyNode n WHERE n.deleted = 0 AND n.enabled = 1 AND n.moduleScope = :scope "
            + "AND n.ownerId IN :owners AND ((:parentId IS NULL AND n.parentId IS NULL) OR n.parentId = :parentId) "
            + "ORDER BY n.sortOrder ASC, n.id ASC")
    List<SysTaxonomyNode> listByScopeOwnersAndParent(
            @Param("scope") String scope,
            @Param("owners") Collection<Long> owners,
            @Param("parentId") Long parentId);

    @Query("SELECT n FROM SysTaxonomyNode n WHERE n.deleted = 0 AND n.enabled = 1 AND n.moduleScope = :scope "
            + "AND n.ownerId IN :owners AND LOWER(n.code) IN :codesLower")
    List<SysTaxonomyNode> findActiveByScopeAndOwnersAndCodesLower(
            @Param("scope") String scope,
            @Param("owners") Collection<Long> owners,
            @Param("codesLower") Collection<String> codesLower);
}
