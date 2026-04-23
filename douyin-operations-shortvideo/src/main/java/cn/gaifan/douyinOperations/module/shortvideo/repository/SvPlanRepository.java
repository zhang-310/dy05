package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SvPlanRepository extends JpaRepository<SvPlan, Long>, JpaSpecificationExecutor<SvPlan> {

    Optional<SvPlan> findByIdAndDeleted(Long id, Integer deleted);
}
