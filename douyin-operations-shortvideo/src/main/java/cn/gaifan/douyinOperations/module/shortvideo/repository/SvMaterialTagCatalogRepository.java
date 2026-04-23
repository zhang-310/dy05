package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvMaterialTagCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SvMaterialTagCatalogRepository extends JpaRepository<SvMaterialTagCatalog, Long> {

    List<SvMaterialTagCatalog> findByOwnerIdOrderByTagAsc(Long ownerId);

    Optional<SvMaterialTagCatalog> findByOwnerIdAndTagIgnoreCase(Long ownerId, String tag);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE sv_material_tag_catalog SET deleted = 1, update_time = CURRENT_TIMESTAMP "
            + "WHERE id = :id AND owner_id = :ownerId AND deleted = 0", nativeQuery = true)
    int softDeleteByIdAndOwner(@Param("id") Long id, @Param("ownerId") Long ownerId);
}
