package cn.gaifan.douyinOperations.module.drama.repository;

import cn.gaifan.douyinOperations.module.drama.entity.DramaProject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface DramaProjectRepository extends JpaRepository<DramaProject, Long>,
        JpaSpecificationExecutor<DramaProject> {

    List<DramaProject> findByUserIdAndDeletedOrderByCreateTimeDesc(Long userId, Integer deleted);

    Page<DramaProject> findByUserIdAndDeleted(Long userId, Integer deleted, Pageable pageable);
}
