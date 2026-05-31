package cn.gaifan.douyinOperations.module.tianapi.repository;

import cn.gaifan.douyinOperations.module.tianapi.entity.TianApiImportRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TianApiImportRunRepository extends JpaRepository<TianApiImportRun, Long> {

    Optional<TianApiImportRun> findTopByOrderByCreateTimeDesc();
}
