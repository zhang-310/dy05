package cn.gaifan.douyinOperations.module.photoavatar.repository;

import cn.gaifan.douyinOperations.module.photoavatar.entity.PhotoAvatarTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface PhotoAvatarTaskRepository extends JpaRepository<PhotoAvatarTask, Long>,
        JpaSpecificationExecutor<PhotoAvatarTask> {

    List<PhotoAvatarTask> findByUserIdAndDeletedOrderByCreateTimeDesc(Long userId, Integer deleted);

    Page<PhotoAvatarTask> findByUserIdAndDeleted(Long userId, Integer deleted, Pageable pageable);
}
