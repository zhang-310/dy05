package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvVideoGeneration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SvVideoGenerationRepository extends JpaRepository<SvVideoGeneration, Long>, JpaSpecificationExecutor<SvVideoGeneration> {
}
