package cn.gaifan.douyinOperations.module.script.repository;

import cn.gaifan.douyinOperations.module.script.entity.ScriptLibrary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScriptLibraryRepository extends JpaRepository<ScriptLibrary, Long>, JpaSpecificationExecutor<ScriptLibrary> {

    Optional<ScriptLibrary> findByIdAndDeleted(Long id, Integer deleted);

    @Modifying
    @Query("UPDATE ScriptLibrary s SET s.useCount = s.useCount + 1 WHERE s.id = :id")
    void incrementUseCount(@Param("id") Long id);

    /**
     * 查询所有不重复的分类
     */
    @Query("SELECT DISTINCT s.category FROM ScriptLibrary s WHERE s.deleted = 0 AND s.category IS NOT NULL AND s.category != '' ORDER BY s.category")
    List<String> findDistinctCategories();
}
