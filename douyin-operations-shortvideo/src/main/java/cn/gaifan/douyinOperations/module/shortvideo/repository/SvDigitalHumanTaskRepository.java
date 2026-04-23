package cn.gaifan.douyinOperations.module.shortvideo.repository;

import cn.gaifan.douyinOperations.module.shortvideo.entity.SvDigitalHumanTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SvDigitalHumanTaskRepository extends JpaRepository<SvDigitalHumanTask, Long> {
    List<SvDigitalHumanTask> findByStatusInAndDeletedAndRetryCountLessThan(List<String> statuses, int deleted, int maxRetries);
    Optional<SvDigitalHumanTask> findByProviderAndExternalTaskIdAndDeleted(String provider, String externalTaskId, int deleted);
    Optional<SvDigitalHumanTask> findByProjectIdAndDeleted(Long projectId, int deleted);
}
