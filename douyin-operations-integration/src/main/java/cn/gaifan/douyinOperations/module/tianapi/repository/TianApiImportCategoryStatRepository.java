package cn.gaifan.douyinOperations.module.tianapi.repository;

import cn.gaifan.douyinOperations.module.tianapi.entity.TianApiImportCategoryStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TianApiImportCategoryStatRepository extends JpaRepository<TianApiImportCategoryStat, Long> {

    List<TianApiImportCategoryStat> findByRunIdOrderByIdAsc(Long runId);

    long countByRunId(Long runId);

    @Query("SELECT COALESCE(SUM(s.calls),0), COALESCE(SUM(s.imported),0), COALESCE(SUM(s.skipped),0), "
            + "COALESCE(SUM(s.emptyResponses),0), COALESCE(SUM(s.failedCalls),0) "
            + "FROM TianApiImportCategoryStat s WHERE s.runId = :runId")
    Object[] aggregateByRunId(@Param("runId") Long runId);
}
