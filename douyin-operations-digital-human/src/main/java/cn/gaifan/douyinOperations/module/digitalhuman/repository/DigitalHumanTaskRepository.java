package cn.gaifan.douyinOperations.module.digitalhuman.repository;

import cn.gaifan.douyinOperations.module.digitalhuman.entity.DigitalHumanTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface DigitalHumanTaskRepository extends JpaRepository<DigitalHumanTask, Long>,
        JpaSpecificationExecutor<DigitalHumanTask> {

    List<DigitalHumanTask> findByUserIdAndDeletedOrderByCreateTimeDesc(Long userId, Integer deleted);

    Page<DigitalHumanTask> findByUserIdAndDeleted(Long userId, Integer deleted, Pageable pageable);
}
