package cn.gaifan.douyinOperations.module.ai.repository;

import cn.gaifan.douyinOperations.module.ai.entity.EvolutionFitnessRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvolutionFitnessRecordRepository extends JpaRepository<EvolutionFitnessRecord, Long>, JpaSpecificationExecutor<EvolutionFitnessRecord> {

    List<EvolutionFitnessRecord> findByTaskIdOrderByCreateTimeDesc(String taskId);
}
